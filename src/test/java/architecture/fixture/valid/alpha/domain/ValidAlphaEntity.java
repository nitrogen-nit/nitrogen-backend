package architecture.fixture.valid.alpha.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.UUID;
import vn.nitrogen.common.domain.ModuleReference;

@Entity
public class ValidAlphaEntity {

    @Id
    private UUID id;

    @ModuleReference("beta")
    private UUID betaId;
}
