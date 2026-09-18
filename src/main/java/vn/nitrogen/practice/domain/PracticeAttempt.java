package vn.nitrogen.practice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import vn.nitrogen.common.domain.AbstractIdentifiableEntity;
import vn.nitrogen.common.domain.ModuleReference;

@Entity
@Table(schema = "practice", name = "practice_attempts")
public class PracticeAttempt extends AbstractIdentifiableEntity {

    @ModuleReference("identity")
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "attempt_kind", nullable = false, length = 32)
    private AttemptKind attemptKind;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin_type", nullable = false, length = 32)
    private OriginType originType;

    @Column(name = "origin_id", nullable = false)
    private UUID originId;

    @Column(name = "attempt_no", nullable = false)
    private int attemptNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;

    @Enumerated(EnumType.STRING)
    @Column(name = "grading_status", nullable = false, length = 24)
    private GradingStatus gradingStatus = GradingStatus.NOT_REQUIRED;

    @Column(name = "score", precision = 12, scale = 4)
    private BigDecimal score;

    @Column(name = "max_score", precision = 12, scale = 4)
    private BigDecimal maxScore;

    @Column(name = "score_final", nullable = false)
    private boolean scoreFinal;

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;

    @Column(name = "deadline_at")
    private Instant deadlineAt;

    @Column(name = "resume_key_hash", length = 128)
    private String resumeKeyHash;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(name = "submitted_reason", length = 32)
    private String submittedReason;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    protected PracticeAttempt() {
    }

    public PracticeAttempt(
            UUID userId,
            AttemptKind attemptKind,
            OriginType originType,
            UUID originId,
            int attemptNo,
            String idempotencyKey,
            Instant deadlineAt) {
        this.userId = userId;
        this.attemptKind = attemptKind;
        this.originType = originType;
        this.originId = originId;
        this.attemptNo = attemptNo;
        this.idempotencyKey = idempotencyKey;
        this.deadlineAt = deadlineAt;
    }

    public UUID getUserId() {
        return userId;
    }

    public AttemptKind getAttemptKind() {
        return attemptKind;
    }

    public OriginType getOriginType() {
        return originType;
    }

    public UUID getOriginId() {
        return originId;
    }

    public int getAttemptNo() {
        return attemptNo;
    }

    public AttemptStatus getStatus() {
        return status;
    }

    public GradingStatus getGradingStatus() {
        return gradingStatus;
    }

    public BigDecimal getScore() {
        return score;
    }

    public BigDecimal getMaxScore() {
        return maxScore;
    }

    public boolean isScoreFinal() {
        return scoreFinal;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getRowVersion() {
        return rowVersion;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        startedAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
