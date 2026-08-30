package vn.nitrogen.architecture.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import vn.nitrogen.architecture.NitrogenModules;
import vn.nitrogen.architecture.rules.FlywayMigrationRules;

@Tag("architecture")
class FlywayMigrationOwnershipTest {

    private static final Path MIGRATION_ROOT = Path.of("src/main/resources/db/migration");
    private static final Set<String> APPROVED_CROSS_SCHEMA_REFERENCES = Set.of("practice:identity");

    @Test
    void migrationsFollowModuleOwnershipConventions() {
        assertThat(FlywayMigrationRules.validate(
                        MIGRATION_ROOT,
                        NitrogenModules.BUSINESS_MODULE_NAMES,
                        APPROVED_CROSS_SCHEMA_REFERENCES))
                .isEmpty();
    }
}
