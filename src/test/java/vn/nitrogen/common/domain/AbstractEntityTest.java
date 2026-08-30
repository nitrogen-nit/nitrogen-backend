package vn.nitrogen.common.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class AbstractEntityTest {

    @Test
    void createsUuidV7IdentifierByDefault() {
        TestEntity entity = new TestEntity();

        assertThat(entity.getId().version()).isEqualTo(7);
    }

    @Test
    void comparesEntitiesByNonNullIdentifier() {
        UUID id = UUID.randomUUID();
        TestEntity left = new TestEntity();
        TestEntity right = new TestEntity();
        left.setId(id);
        right.setId(id);

        assertThat(left).isEqualTo(right);
        assertThat(left.hashCode()).isEqualTo(right.hashCode());
        assertThat(left.toString()).isEqualTo("TestEntity{id=" + id + "}");
    }

    @Test
    void doesNotTreatNullIdentifiersAsEqual() {
        TestEntity left = new TestEntity();
        TestEntity right = new TestEntity();
        left.setId(null);
        right.setId(null);

        assertThat(left).isEqualTo(left);
        assertThat(left).isNotEqualTo(right);
        assertThat(left).isNotEqualTo("not an entity");
    }

    @Test
    void exposesAuditingFields() {
        Instant createdAt = Instant.parse("2026-08-30T00:00:00Z");
        Instant updatedAt = Instant.parse("2026-08-30T01:00:00Z");
        AuditedEntity entity = new AuditedEntity();

        entity.setCreatedBy("creator");
        entity.setCreatedAt(createdAt);
        entity.setUpdatedBy("updater");
        entity.setUpdatedAt(updatedAt);

        assertThat(entity.getCreatedBy()).isEqualTo("creator");
        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
        assertThat(entity.getUpdatedBy()).isEqualTo("updater");
        assertThat(entity.getUpdatedAt()).isEqualTo(updatedAt);
    }

    private static class TestEntity extends AbstractIdentifiableEntity {
    }

    private static class AuditedEntity extends AbstractAuditingEntity {
    }
}
