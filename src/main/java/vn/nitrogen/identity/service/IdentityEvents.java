package vn.nitrogen.identity.service;

/**
 * Tên event identity ghi vào outbox. Payload mô tả ở
 * {@code contracts/json-schema/messages/}.
 */
final class IdentityEvents {

    static final String USER_AGGREGATE = "User";

    /** Event công khai: không mang email hay token. */
    static final String USER_REGISTERED = "identity.UserRegistered";
    static final String USER_REGISTERED_ROUTING_KEY = "identity.user.registered";

    /**
     * Yêu cầu gửi email xác minh. Mang token thô vì bên gửi email cần nó để dựng
     * link; chỉ consumer gửi email được bind routing key này.
     */
    static final String EMAIL_VERIFICATION_REQUESTED = "identity.EmailVerificationRequested";
    static final String EMAIL_VERIFICATION_REQUESTED_ROUTING_KEY = "identity.email-verification.requested";

    static final int SCHEMA_VERSION = 1;

    private IdentityEvents() {
    }
}
