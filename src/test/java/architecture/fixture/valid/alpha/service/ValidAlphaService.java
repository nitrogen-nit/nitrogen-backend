package architecture.fixture.valid.alpha.service;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import architecture.fixture.valid.alpha.dto.ValidAlphaDto;
import architecture.fixture.valid.alpha.repository.ValidAlphaRepository;

@Service
@Transactional(readOnly = true)
public class ValidAlphaService {

    private final ValidAlphaRepository repository;

    public ValidAlphaService(ValidAlphaRepository repository) {
        this.repository = repository;
    }

    public ValidAlphaDto findById(UUID id) {
        return new ValidAlphaDto(id);
    }
}
