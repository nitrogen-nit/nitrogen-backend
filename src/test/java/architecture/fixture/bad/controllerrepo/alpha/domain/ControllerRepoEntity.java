package architecture.fixture.bad.controllerrepo.alpha.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.UUID;

@Entity
public class ControllerRepoEntity {

    @Id
    private UUID id;
}
