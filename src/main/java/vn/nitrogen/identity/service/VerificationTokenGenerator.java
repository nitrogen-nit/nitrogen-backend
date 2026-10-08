package vn.nitrogen.identity.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

/**
 * Sinh token thô và hash của nó.
 *
 * <p>Chỉ hash được lưu DB. Token có 256 bit ngẫu nhiên nên SHA-256 không muối
 * là đủ — không có không gian nào nhỏ để brute-force như với mật khẩu — và hash
 * tất định cho phép tra cứu thẳng bằng unique index.
 */
@Component
public class VerificationTokenGenerator {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom random = new SecureRandom();

    public GeneratedToken generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new GeneratedToken(raw, hash(raw));
    }

    public String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    public record GeneratedToken(String raw, String hash) {

        @Override
        public String toString() {
            return "GeneratedToken[raw=***, hash=%s]".formatted(hash);
        }
    }
}
