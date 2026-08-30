package architecture.fixture.bad.crossrepository.alpha.repository;

import java.util.UUID;
import org.springframework.data.repository.Repository;
import architecture.fixture.bad.crossrepository.alpha.domain.AlphaEntity;

public interface AlphaRepository extends Repository<AlphaEntity, UUID> {}
