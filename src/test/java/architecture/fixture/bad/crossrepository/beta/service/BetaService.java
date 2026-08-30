package architecture.fixture.bad.crossrepository.beta.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import architecture.fixture.bad.crossrepository.alpha.repository.AlphaRepository;

@Service
@Transactional(readOnly = true)
public class BetaService {

    private final AlphaRepository alphaRepository;

    public BetaService(AlphaRepository alphaRepository) {
        this.alphaRepository = alphaRepository;
    }

    public String probe() {
        return alphaRepository.getClass().getSimpleName();
    }
}
