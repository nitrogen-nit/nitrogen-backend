package architecture.fixture.bad.controllerrepo.alpha.repository;

import java.util.UUID;
import org.springframework.data.repository.Repository;
import architecture.fixture.bad.controllerrepo.alpha.domain.ControllerRepoEntity;

public interface ControllerRepoRepository extends Repository<ControllerRepoEntity, UUID> {}
