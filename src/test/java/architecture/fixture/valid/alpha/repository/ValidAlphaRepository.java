package architecture.fixture.valid.alpha.repository;

import java.util.UUID;
import org.springframework.data.repository.Repository;
import architecture.fixture.valid.alpha.domain.ValidAlphaEntity;

public interface ValidAlphaRepository extends Repository<ValidAlphaEntity, UUID> {}
