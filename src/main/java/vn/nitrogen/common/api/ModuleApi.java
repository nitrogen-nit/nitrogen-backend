package vn.nitrogen.common.api;

/**
 * Marker cho facade công khai của một module (§4.3).
 *
 * <p>Theo khuôn Artemis: {@code api/} KHÔNG phải interface nghiệp vụ mà là class
 * facade cụ thể, bọc repository/service nội bộ và chỉ trả DTO ra ngoài.
 *
 * <p>Marker rỗng có chủ đích — nó tồn tại để ArchUnit nhận diện được đâu là điểm
 * vào hợp lệ của module, không phải để định nghĩa hành vi chung.
 *
 * <p>Tách facade theo use case, không gộp thành một God-API: module bị nhiều bên
 * đọc (như {@code practice}) nên có nhiều facade nhỏ, mỗi cái phục vụ một nhóm
 * người gọi.
 */
public interface ModuleApi {
}
