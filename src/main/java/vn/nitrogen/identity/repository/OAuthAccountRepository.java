package vn.nitrogen.identity.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.nitrogen.identity.domain.OAuthAccount;

public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, UUID>{
    Optional<OAuthAccount> findByProviderAndProviderSubject(
            String provider,
            String providerSubject
    );
    boolean existsByProviderAndProviderSubject(
            String provider,
            String providerSubject
    );
}
