package vn.nitrogen.identity.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.nitrogen.identity.domain.EmailVerificationToken;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {

    /** Nạp kèm user: luồng xác minh luôn cập nhật user ngay sau khi tìm thấy token. */
    @Query("""
            select t
            from EmailVerificationToken t
            join fetch t.user
            where t.tokenHash = :tokenHash
            """)
    Optional<EmailVerificationToken> findWithUserByTokenHash(@Param("tokenHash") String tokenHash);
}
