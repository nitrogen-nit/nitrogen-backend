package vn.nitrogen.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class UuidV7Test {

    @Test
    void createsRfcCompatibleUuidV7() {
        long unixMillis = 1_777_777_777_777L;

        UUID uuid = UuidV7.randomUuid(unixMillis);

        assertThat(uuid.version()).isEqualTo(7);
        assertThat(uuid.variant()).isEqualTo(2);
        assertThat(UuidV7.timestamp(uuid)).isEqualTo(unixMillis);
    }

    @Test
    void createsUuidForCurrentTime() {
        UUID uuid = UuidV7.randomUuid();

        assertThat(uuid.version()).isEqualTo(7);
        assertThat(UuidV7.timestamp(uuid)).isPositive();
    }

    @Test
    void rejectsUuidThatIsNotVersion7() {
        UUID uuid = UUID.randomUUID();

        assertThatThrownBy(() -> UuidV7.timestamp(uuid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Không phải UUIDv7");
    }
}
