package architecture.fixture.bad.transaction.alpha.web;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Transactional
public class TransactionalController {

    @GetMapping("/api/v1/transactional-controller")
    @Transactional
    public String probe() {
        return "bad";
    }
}
