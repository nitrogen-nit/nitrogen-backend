package vn.nitrogen.identity.dto;

/**
 * Phản hồi của đăng ký — cố ý giống hệt nhau dù email đã tồn tại hay chưa.
 *
 * <p>Không có user id: trả id cho đăng ký mới mà không trả cho email trùng là
 * đủ để dò ra email nào đã có tài khoản.
 */
public record RegistrationAcceptedResponse(String status) {

    public static final String VERIFICATION_PENDING = "VERIFICATION_PENDING";

    public static RegistrationAcceptedResponse verificationPending() {
        return new RegistrationAcceptedResponse(VERIFICATION_PENDING);
    }
}
