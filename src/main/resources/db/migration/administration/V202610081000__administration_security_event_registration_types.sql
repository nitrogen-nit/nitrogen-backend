-- Mở rộng tập event_type cho luồng đăng ký và xác minh email (NIT-20).
-- Chỉ thêm giá trị, không bỏ giá trị cũ: bản ghi đã ghi vẫn thoả constraint mới.
ALTER TABLE administration.security_events
    DROP CONSTRAINT chk_security_event_type;

ALTER TABLE administration.security_events
    ADD CONSTRAINT chk_security_event_type
        CHECK (event_type IN (
                              'LOGIN_SUCCESS',
                              'LOGIN_FAILURE',
                              'ACCOUNT_LOCKED',
                              'UNAUTHORIZED_ACCESS',
                              'RATE_LIMITED',
                              'REGISTRATION_SUCCESS',
                              'REGISTRATION_DUPLICATE_EMAIL',
                              'EMAIL_VERIFICATION_SUCCESS',
                              'EMAIL_VERIFICATION_FAILURE'
            ));
