package architecture.fixture;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import vn.nitrogen.architecture.rules.FlywayMigrationRules;

@Tag("architecture")
class FlywayMigrationRuleFixtureTest {

    @TempDir
    Path migrationRoot;

    @Test
    void migrationInWrongOwnerFolderIsRejected() throws Exception {
        Path alphaFolder = migrationRoot.resolve("alpha");
        Files.createDirectories(alphaFolder);
        Files.writeString(alphaFolder.resolve("V202608300000__beta_create_schema.sql"), """
                CREATE TABLE beta.bad_table (
                    id UUID PRIMARY KEY
                );
                """);

        List<String> problems = FlywayMigrationRules.validate(
                migrationRoot,
                Set.of("alpha", "beta"),
                Set.of());

        assertThat(problems).anySatisfy(problem ->
                assertThat(problem).contains("declares module beta").contains("under alpha"));
        assertThat(problems).anySatisfy(problem ->
                assertThat(problem).contains("writes schema beta").contains("owner folder alpha"));
    }
}
