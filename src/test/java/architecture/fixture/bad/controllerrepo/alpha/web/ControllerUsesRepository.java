package architecture.fixture.bad.controllerrepo.alpha.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import architecture.fixture.bad.controllerrepo.alpha.repository.ControllerRepoRepository;

@RestController
public class ControllerUsesRepository {

    private final ControllerRepoRepository repository;

    public ControllerUsesRepository(ControllerRepoRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/api/v1/controller-repo")
    public String probe() {
        return repository.getClass().getSimpleName();
    }
}
