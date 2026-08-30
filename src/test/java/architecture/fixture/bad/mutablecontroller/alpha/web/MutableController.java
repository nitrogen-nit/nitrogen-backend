package architecture.fixture.bad.mutablecontroller.alpha.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MutableController {

    private int requestCount;

    @GetMapping("/api/v1/mutable-controller")
    public int count() {
        requestCount++;
        return requestCount;
    }
}
