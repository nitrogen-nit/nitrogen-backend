package vn.nitrogen.common.util;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Sinh UUID version 7 (RFC 9562) — 48 bit timestamp Unix millisecond ở đầu,
 * phần còn lại ngẫu nhiên.
 *
 * <p>Chọn v7 thay vì v4 vì khoá chính tăng dần theo thời gian giữ B-tree không
 * bị phân mảnh khi insert nóng (practice_attempts, attempt_items, outbox_events).
 *
 * <p>Layout:
 * <pre>
 *   unix_ts_ms (48) | ver=7 (4) | rand_a (12) | var=0b10 (2) | rand_b (62)
 * </pre>
 */
public final class UuidV7 {

    private static final SecureRandom RANDOM = new SecureRandom();

    private UuidV7() {
    }

    public static UUID randomUuid() {
        return randomUuid(System.currentTimeMillis());
    }

    static UUID randomUuid(long unixMillis) {
        byte[] random = new byte[10];
        RANDOM.nextBytes(random);

        long msb = (unixMillis & 0xFFFF_FFFF_FFFFL) << 16;
        msb |= 0x7000L;                                  // version 7
        msb |= ((random[0] & 0xFFL) << 8) | (random[1] & 0xFFL);

        long lsb = 0L;
        for (int i = 2; i < 10; i++) {
            lsb = (lsb << 8) | (random[i] & 0xFFL);
        }
        lsb &= 0x3FFF_FFFF_FFFF_FFFFL;                   // xoá 2 bit variant
        lsb |= 0x8000_0000_0000_0000L;                   // variant RFC 4122

        return new UUID(msb, lsb);
    }

    /** Trích timestamp Unix millisecond đã nhúng trong UUIDv7. */
    public static long timestamp(UUID uuid) {
        if (uuid.version() != 7) {
            throw new IllegalArgumentException("Không phải UUIDv7: " + uuid);
        }
        return uuid.getMostSignificantBits() >>> 16;
    }
}
