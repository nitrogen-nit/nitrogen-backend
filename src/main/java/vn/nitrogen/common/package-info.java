/**
 * Hạ tầng dùng chung, không chứa business: base entity, UUIDv7, error model,
 * JPA converter, web exception handler.
 *
 * <p>Khai báo OPEN để mọi module nghiệp vụ dùng trực tiếp mà không phải đi qua
 * named interface — đây là thư viện nội bộ, không phải module nghiệp vụ.
 */
@org.springframework.modulith.ApplicationModule(
        type = org.springframework.modulith.ApplicationModule.Type.OPEN,
        displayName = "Common")
package vn.nitrogen.common;
