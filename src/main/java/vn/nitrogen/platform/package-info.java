/**
 * Hạ tầng kỹ thuật cắt ngang: security, messaging, outbox publisher, job
 * framework, observability. Không chứa business.
 *
 * <p>OPEN vì mọi module đều phải dùng {@code MessageEnvelope}, job API và
 * security context — bắt chúng đi qua named interface chỉ tạo nhiễu.
 */
@org.springframework.modulith.ApplicationModule(
        type = org.springframework.modulith.ApplicationModule.Type.OPEN,
        displayName = "Platform")
package vn.nitrogen.platform;
