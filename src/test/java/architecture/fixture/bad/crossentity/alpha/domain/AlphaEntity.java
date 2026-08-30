package architecture.fixture.bad.crossentity.alpha.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.UUID;

@Entity
public class AlphaEntity {

    @Id
    private UUID id;
}
