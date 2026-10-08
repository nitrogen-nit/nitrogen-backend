package vn.nitrogen.identity.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.nitrogen.identity.dto.RegisterUserRequest;
import vn.nitrogen.identity.dto.RegistrationAcceptedResponse;
import vn.nitrogen.identity.dto.VerifyEmailRequest;
import vn.nitrogen.identity.service.EmailVerificationService;
import vn.nitrogen.identity.service.UserRegistrationService;

/**
 * Endpoint xác thực công khai — gọi được khi chưa đăng nhập.
 *
 * <p>Đăng ký trả {@code 202}: tài khoản chưa dùng được cho tới khi xác minh
 * email, và phản hồi không được cho biết email đã tồn tại hay chưa.
 */
@RestController
@Profile("web")
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserRegistrationService registrations;
    private final EmailVerificationService emailVerifications;

    public AuthController(UserRegistrationService registrations, EmailVerificationService emailVerifications) {
        this.registrations = registrations;
        this.emailVerifications = emailVerifications;
    }

    @PostMapping("/registrations")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public RegistrationAcceptedResponse register(
            @Valid @RequestBody RegisterUserRequest request,
            HttpServletRequest httpRequest) {
        return registrations.register(
                request,
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader(HttpHeaders.USER_AGENT));
    }

    @PostMapping("/email-verifications")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request,
            HttpServletRequest httpRequest) {
        emailVerifications.verify(
                request.token(),
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader(HttpHeaders.USER_AGENT));
    }
}
