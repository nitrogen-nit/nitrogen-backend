package architecture.fixture.bad.controllerentity.alpha.web;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import architecture.fixture.bad.controllerentity.alpha.domain.ExposedEntity;

@RestController
public class ControllerReturnsEntity {

    @GetMapping("/api/v1/controller-entity")
    public ResponseEntity<List<ExposedEntity>> list() {
        return ResponseEntity.ok(List.of());
    }
}
