-- =====================================================================
-- Practice attempt runtime — DDL theo đặc tả §11.1, §11.2, §11.3
--
-- Practice sở hữu TOÀN BỘ attempt runtime (ADR-004): attempts, items,
-- responses, grading runs, topic results. Assessment chỉ sở hữu định
-- nghĩa bất biến.
--
-- DDL dưới đây giữ NGUYÊN VĂN theo đặc tả v1.0. Ba khoảng trống đã được
-- báo cáo đánh giá ghi nhận; không tự sửa ở đây để tài liệu và schema
-- không lệch nhau. Xử lý khi các mục BL được chốt.
-- =====================================================================

CREATE TABLE practice.practice_attempts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    -- TODO(BL-04): attempt_kind / origin_type / submitted_reason chưa có CHECK
    --              và chưa có trong Status Catalog (Phụ lục A).
    attempt_kind VARCHAR(32) NOT NULL,
    origin_type VARCHAR(32) NOT NULL,
    origin_id UUID NOT NULL,
    attempt_no INTEGER NOT NULL DEFAULT 1,
    status VARCHAR(24) NOT NULL,
    grading_status VARCHAR(24) NOT NULL,
    score NUMERIC(12,4),
    max_score NUMERIC(12,4),
    score_final BOOLEAN NOT NULL DEFAULT FALSE,
    duration_seconds INTEGER NOT NULL DEFAULT 0,
    deadline_at TIMESTAMPTZ,
    resume_key_hash VARCHAR(128),
    idempotency_key VARCHAR(100),
    submitted_reason VARCHAR(32),
    started_at TIMESTAMPTZ NOT NULL,
    submitted_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL,
    row_version BIGINT NOT NULL DEFAULT 0,
    -- TODO(BL-02): thiếu current_grading_run_id — sau regrade không có con trỏ
    --              tới grading run đang có hiệu lực, "điểm chính thức" phải suy
    --              ra từ run chưa bị superseded hoặc max(run_number).
    CONSTRAINT fk_attempt_user FOREIGN KEY (user_id)
        REFERENCES identity.users(id) ON DELETE RESTRICT,
    CONSTRAINT chk_attempt_no CHECK (attempt_no > 0),
    CONSTRAINT chk_attempt_status CHECK (
        status IN ('IN_PROGRESS','SUBMITTED','COMPLETED','EXPIRED','CANCELLED')
    ),
    CONSTRAINT chk_attempt_grading_status CHECK (
        grading_status IN ('NOT_REQUIRED','PENDING_AUTO','PENDING_MANUAL','PARTIALLY_GRADED','GRADED','FAILED')
    ),
    CONSTRAINT chk_attempt_score CHECK (
        score IS NULL OR (max_score > 0 AND score BETWEEN 0 AND max_score)
    ),
    CONSTRAINT chk_attempt_counters CHECK (duration_seconds >= 0 AND row_version >= 0),
    CONSTRAINT chk_attempt_submitted_time CHECK (
        submitted_at IS NULL OR submitted_at >= started_at
    ),
    CONSTRAINT chk_attempt_completed_time CHECK (
        completed_at IS NULL OR completed_at >= COALESCE(submitted_at, started_at)
    )
);

CREATE INDEX idx_attempt_user_history
ON practice.practice_attempts (user_id, started_at DESC);

CREATE INDEX idx_attempt_auto_submit
ON practice.practice_attempts (deadline_at, id)
WHERE status = 'IN_PROGRESS' AND deadline_at IS NOT NULL;

CREATE INDEX idx_attempt_pending_grading
ON practice.practice_attempts (grading_status, submitted_at)
WHERE status = 'SUBMITTED';

CREATE UNIQUE INDEX uk_attempt_resume_key_hash
ON practice.practice_attempts (resume_key_hash)
WHERE resume_key_hash IS NOT NULL;

CREATE UNIQUE INDEX uk_attempt_origin
ON practice.practice_attempts (user_id, origin_type, origin_id, attempt_no);

-- Chặn hai attempt đang mở cùng một origin cho cùng user (§5.1).
CREATE UNIQUE INDEX uk_attempt_active_origin
ON practice.practice_attempts (user_id, origin_type, origin_id)
WHERE status = 'IN_PROGRESS';

CREATE UNIQUE INDEX uk_attempt_idempotency
ON practice.practice_attempts (user_id, idempotency_key)
WHERE idempotency_key IS NOT NULL;


-- ── §11.2 attempt_items: snapshot câu hỏi tại thời điểm bắt đầu ──
-- Snapshot version để điểm không đổi khi nội dung có version mới (§9.2).
CREATE TABLE practice.attempt_items (
    id UUID PRIMARY KEY,
    attempt_id UUID NOT NULL,
    assessment_item_id UUID,
    exercise_version_id UUID NOT NULL,
    stimulus_version_id UUID,
    -- TODO(BL-16): quy ước scoring_policy_version_id NULL = chấm mặc định theo
    --              question_type — chưa ghi rõ trong đặc tả.
    scoring_policy_version_id UUID,
    section_code VARCHAR(32),
    question_type VARCHAR(40) NOT NULL,
    display_order INTEGER NOT NULL,
    max_score NUMERIC(12,4) NOT NULL,
    score NUMERIC(12,4),
    grading_status VARCHAR(24) NOT NULL,
    graded_at TIMESTAMPTZ,
    row_version BIGINT NOT NULL DEFAULT 0,
    -- TODO(BL-07): các cột *_version_id chưa có FK cross-schema tới assessment,
    --              dù §7.1 cho phép. Open Decision 22.2 chưa chốt.
    CONSTRAINT fk_attempt_item_attempt FOREIGN KEY (attempt_id)
        REFERENCES practice.practice_attempts(id) ON DELETE RESTRICT,
    CONSTRAINT uk_attempt_item_order UNIQUE (attempt_id, display_order),
    CONSTRAINT chk_attempt_item_order CHECK (display_order > 0),
    CONSTRAINT chk_attempt_item_score CHECK (
        max_score > 0 AND (score IS NULL OR score BETWEEN 0 AND max_score)
    ),
    CONSTRAINT chk_attempt_item_version CHECK (row_version >= 0)
);


-- ── §11.2 exercise_responses: đáp án hiện tại, một dòng cho mỗi item ──
-- PK = attempt_item_id ⇒ autosave là UPSERT single-row (§5.2), không sinh
-- lịch sử mỗi lần gõ.
CREATE TABLE practice.exercise_responses (
    attempt_item_id UUID PRIMARY KEY,
    raw_response JSONB NOT NULL,
    normalized_response JSONB,
    answered_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL,
    last_client_request_id UUID,
    row_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_response_item FOREIGN KEY (attempt_item_id)
        REFERENCES practice.attempt_items(id) ON DELETE RESTRICT,
    CONSTRAINT chk_response_payload CHECK (jsonb_typeof(raw_response) = 'object'),
    CONSTRAINT chk_response_version CHECK (row_version >= 0)
);


-- ── §11.3 grading_runs: lịch sử chấm, append-only ──
-- Regrade tạo run mới, không sửa run cũ (ADR-005).
CREATE TABLE practice.grading_runs (
    id UUID PRIMARY KEY,
    attempt_id UUID NOT NULL,
    -- TODO(BL-15): chưa nêu cách sinh run_number. SELECT MAX+1 sẽ đụng
    --              uk_grading_run khi hai lần regrade chạy song song.
    run_number INTEGER NOT NULL,
    run_type VARCHAR(24) NOT NULL,
    status VARCHAR(24) NOT NULL,
    grader_code VARCHAR(80) NOT NULL,
    grader_version VARCHAR(40) NOT NULL,
    input_snapshot JSONB NOT NULL,
    output_snapshot JSONB,
    supersedes_run_id UUID,
    external_request_id VARCHAR(120),
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    error_code VARCHAR(80),
    error_message TEXT,
    CONSTRAINT fk_grading_attempt FOREIGN KEY (attempt_id)
        REFERENCES practice.practice_attempts(id) ON DELETE RESTRICT,
    CONSTRAINT fk_grading_supersedes FOREIGN KEY (supersedes_run_id)
        REFERENCES practice.grading_runs(id) ON DELETE RESTRICT,
    CONSTRAINT uk_grading_run UNIQUE (attempt_id, run_number),
    -- Unique theo cặp để attempt_topic_results FK được (grading_run_id, attempt_id),
    -- chống lệch attempt giữa topic result và grading run.
    CONSTRAINT uk_grading_run_attempt UNIQUE (id, attempt_id),
    CONSTRAINT chk_grading_run_number CHECK (run_number > 0),
    CONSTRAINT chk_grading_payload CHECK (
        jsonb_typeof(input_snapshot) = 'object'
        AND (output_snapshot IS NULL OR jsonb_typeof(output_snapshot) = 'object')
    )
);

CREATE UNIQUE INDEX uk_grading_external_request
ON practice.grading_runs (external_request_id)
WHERE external_request_id IS NOT NULL;


-- ── §11.3 attempt_topic_results: đóng góp điểm theo topic ──
-- Nguồn để progress rebuild projection (§10.3).
CREATE TABLE practice.attempt_topic_results (
    attempt_id UUID NOT NULL,
    topic_node_id UUID NOT NULL,
    grading_run_id UUID NOT NULL,
    calculation_version INTEGER NOT NULL,
    earned_score NUMERIC(12,4) NOT NULL,
    max_score NUMERIC(12,4) NOT NULL,
    -- TODO(BL-11): ngữ nghĩa của weight chưa rõ — trọng số topic trong blueprint,
    --              hay trung bình mapping weight của các exercise thuộc topic?
    weight NUMERIC(12,6) NOT NULL,
    correct_count INTEGER NOT NULL,
    total_count INTEGER NOT NULL,
    duration_seconds INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (attempt_id, topic_node_id, grading_run_id),
    CONSTRAINT fk_topic_result_run FOREIGN KEY (grading_run_id, attempt_id)
        REFERENCES practice.grading_runs(id, attempt_id) ON DELETE RESTRICT,
    CONSTRAINT chk_topic_result_score CHECK (
        max_score > 0 AND earned_score BETWEEN 0 AND max_score
    ),
    CONSTRAINT chk_topic_result_counts CHECK (
        calculation_version > 0
        AND correct_count BETWEEN 0 AND total_count
        AND duration_seconds >= 0
    )
);
