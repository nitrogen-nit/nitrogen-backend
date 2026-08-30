package vn.nitrogen.architecture.rules;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class FlywayMigrationRules {

    private static final Pattern FLYWAY_FILE =
            Pattern.compile("V(?<version>\\d{12,})__(?<module>[a-z][a-z0-9]*)_[a-z0-9_]+\\.sql");

    private static final List<Pattern> OWNED_SCHEMA_OPERATIONS = List.of(
            Pattern.compile("\\bCREATE\\s+SCHEMA\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?([a-z][a-z0-9_]*)",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bCREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?([a-z][a-z0-9_]*)\\.",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bALTER\\s+TABLE\\s+(?:IF\\s+EXISTS\\s+)?([a-z][a-z0-9_]*)\\.",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile(
                    "\\bCREATE\\s+(?:UNIQUE\\s+)?INDEX\\s+(?:CONCURRENTLY\\s+)?"
                            + "(?:IF\\s+NOT\\s+EXISTS\\s+)?[a-z][a-z0-9_]*\\s+ON\\s+([a-z][a-z0-9_]*)\\.",
                    Pattern.CASE_INSENSITIVE));

    private static final Pattern REFERENCED_SCHEMA =
            Pattern.compile("\\bREFERENCES\\s+([a-z][a-z0-9_]*)\\.", Pattern.CASE_INSENSITIVE);

    private FlywayMigrationRules() {}

    public static List<String> validate(
            Path migrationRoot,
            Collection<String> moduleNames,
            Set<String> approvedCrossSchemaReferences) {
        List<String> problems = new ArrayList<>();
        if (!Files.isDirectory(migrationRoot)) {
            return List.of("Missing Flyway migration root: " + migrationRoot);
        }

        Set<String> modules = Set.copyOf(moduleNames);
        problems.addAll(validateImmediateChildren(migrationRoot, modules));

        List<Path> migrationFiles = findMigrationFiles(migrationRoot);
        Map<String, List<Path>> filesByVersion = new HashMap<>();
        for (Path migrationFile : migrationFiles) {
            validateMigrationFile(migrationRoot, migrationFile, modules, approvedCrossSchemaReferences, problems)
                    .ifPresent(version -> filesByVersion.computeIfAbsent(version, ignored -> new ArrayList<>())
                            .add(migrationFile));
        }

        filesByVersion.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .forEach(entry -> problems.add("Duplicate Flyway version " + entry.getKey() + " in "
                        + entry.getValue()));

        return problems;
    }

    private static List<String> validateImmediateChildren(Path migrationRoot, Set<String> modules) {
        List<String> problems = new ArrayList<>();
        try (Stream<Path> children = Files.list(migrationRoot)) {
            children.forEach(child -> {
                String name = child.getFileName().toString();
                if (Files.isDirectory(child) && !modules.contains(name)) {
                    problems.add("Migration folder " + name + " does not match a business module");
                } else if (Files.isRegularFile(child) && name.endsWith(".sql")) {
                    problems.add("Migration " + name + " is placed directly under db/migration");
                }
            });
        } catch (IOException ex) {
            problems.add("Cannot list migration root " + migrationRoot + ": " + ex.getMessage());
        }
        return problems;
    }

    private static List<Path> findMigrationFiles(Path migrationRoot) {
        try (Stream<Path> files = Files.walk(migrationRoot)) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".sql"))
                    .sorted()
                    .toList();
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot scan Flyway migrations under " + migrationRoot, ex);
        }
    }

    private static Optional<String> validateMigrationFile(
            Path migrationRoot,
            Path migrationFile,
            Set<String> modules,
            Set<String> approvedCrossSchemaReferences,
            List<String> problems) {
        Path relative = migrationRoot.relativize(migrationFile);
        if (relative.getNameCount() < 2) {
            problems.add("Migration " + relative + " must live under db/migration/<module>/");
            return Optional.empty();
        }

        String folder = relative.getName(0).toString();
        String fileName = migrationFile.getFileName().toString();
        Matcher matcher = FLYWAY_FILE.matcher(fileName);
        if (!matcher.matches()) {
            problems.add("Migration " + relative + " does not match V<timestamp>__<module>_<description>.sql");
            return Optional.empty();
        }

        String version = matcher.group("version");
        String fileModule = matcher.group("module");
        if (!modules.contains(folder)) {
            problems.add("Migration folder " + folder + " does not match a business module");
        }
        if (!folder.equals(fileModule)) {
            problems.add("Migration " + relative + " declares module " + fileModule + " but is under " + folder);
        }

        validateSqlOwnership(relative, migrationFile, folder, modules, approvedCrossSchemaReferences, problems);
        return Optional.of(version);
    }

    private static void validateSqlOwnership(
            Path relative,
            Path migrationFile,
            String ownerModule,
            Set<String> modules,
            Set<String> approvedCrossSchemaReferences,
            List<String> problems) {
        String sql = readSql(migrationFile);
        String normalizedSql = sql.toLowerCase(Locale.ROOT);

        for (Pattern operation : OWNED_SCHEMA_OPERATIONS) {
            Matcher matcher = operation.matcher(normalizedSql);
            while (matcher.find()) {
                String schema = matcher.group(1);
                if (modules.contains(schema) && !schema.equals(ownerModule)) {
                    problems.add("Migration " + relative + " writes schema " + schema
                            + " from owner folder " + ownerModule);
                }
            }
        }

        Matcher references = REFERENCED_SCHEMA.matcher(normalizedSql);
        while (references.find()) {
            String schema = references.group(1);
            String approvalKey = ownerModule + ":" + schema;
            if (modules.contains(schema)
                    && !schema.equals(ownerModule)
                    && !approvedCrossSchemaReferences.contains(approvalKey)) {
                problems.add("Migration " + relative + " references schema " + schema
                        + " without explicit cross-schema approval " + approvalKey);
            }
        }
    }

    private static String readSql(Path migrationFile) {
        try {
            return Files.readString(migrationFile);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot read migration " + migrationFile, ex);
        }
    }
}
