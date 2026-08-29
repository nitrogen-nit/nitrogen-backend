package vn.nitrogen.common.error;

import org.springframework.http.HttpStatus;

/**
 * Mã lỗi ổn định trả ra client.
 *
 * <p>Đây là <b>contract</b>: web bắt theo mã, không bắt theo message. Đổi hoặc bỏ
 * một mã là breaking change — thêm mã mới thì không.
 */
public enum ErrorCode {

    // ── attempt runtime (§5) ──
    ATTEMPT_NOT_FOUND(HttpStatus.NOT_FOUND),
    ATTEMPT_NOT_EDITABLE(HttpStatus.CONFLICT),
    ATTEMPT_ALREADY_SUBMITTED(HttpStatus.CONFLICT),
    ATTEMPT_DEADLINE_PASSED(HttpStatus.CONFLICT),
    ATTEMPT_ACTIVE_EXISTS(HttpStatus.CONFLICT),
    STALE_VERSION(HttpStatus.CONFLICT),

    // ── content/version (§13) ──
    VERSION_NOT_PUBLISHED(HttpStatus.CONFLICT),
    VERSION_IMMUTABLE(HttpStatus.CONFLICT),

    // ── payload (§13.3) ──
    RESPONSE_SCHEMA_INVALID(HttpStatus.BAD_REQUEST),
    UNSUPPORTED_SCHEMA_VERSION(HttpStatus.BAD_REQUEST),

    // ── chung ──
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST),
    ACCESS_DENIED(HttpStatus.FORBIDDEN),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
