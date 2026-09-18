package vn.nitrogen.practice.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.nitrogen.practice.dto.PracticeAttemptSummary;
import vn.nitrogen.practice.dto.StartPracticeAttemptRequest;
import vn.nitrogen.practice.service.PracticeAttemptService;

@RestController
@Validated
@Profile("web")
@RequestMapping("/api/v1/practice-attempts")
public class PracticeAttemptController {

    private final PracticeAttemptService attempts;

    public PracticeAttemptController(PracticeAttemptService attempts) {
        this.attempts = attempts;
    }

    @PostMapping
    public ResponseEntity<PracticeAttemptSummary> start(@Valid @RequestBody StartPracticeAttemptRequest request) {
        PracticeAttemptSummary summary = attempts.start(request);
        return ResponseEntity.created(URI.create("/api/v1/practice-attempts/" + summary.id()))
                .body(summary);
    }

    @GetMapping("/{attemptId}")
    public PracticeAttemptSummary findById(@PathVariable UUID attemptId) {
        return attempts.findById(attemptId);
    }

    @GetMapping
    public List<PracticeAttemptSummary> findRecentByUser(
            @RequestParam UUID userId,
            @RequestParam(defaultValue = "20") @Min(1) @Max(20) int limit) {
        return attempts.findRecentByUser(userId, limit);
    }
}
