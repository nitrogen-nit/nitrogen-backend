package architecture.fixture.bad.moduleref.beta.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.UUID;
import vn.nitrogen.common.domain.ModuleReference;

@Entity
public class BetaReferenceEntity {

    @Id
    private UUID id;

    @ModuleReference("alpha")
    private String alphaId;
}
