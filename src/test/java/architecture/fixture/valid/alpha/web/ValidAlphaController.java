package architecture.fixture.valid.alpha.web;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import architecture.fixture.valid.alpha.dto.ValidAlphaDto;
import architecture.fixture.valid.alpha.service.ValidAlphaService;

@RestController
public class ValidAlphaController {

    private final ValidAlphaService service;

    public ValidAlphaController(ValidAlphaService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/valid-alpha/{id}")
    public ResponseEntity<ValidAlphaDto> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id));
    }
}
