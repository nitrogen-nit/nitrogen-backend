package architecture.fixture.bad.crossentity.beta.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.util.UUID;
import architecture.fixture.bad.crossentity.alpha.domain.AlphaEntity;

@Entity
public class BetaEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    private AlphaEntity alpha;
}
