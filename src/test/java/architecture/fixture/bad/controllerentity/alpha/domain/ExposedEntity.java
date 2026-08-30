package architecture.fixture.bad.controllerentity.alpha.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.UUID;

@Entity
public class ExposedEntity {

    @Id
    private UUID id;
}
