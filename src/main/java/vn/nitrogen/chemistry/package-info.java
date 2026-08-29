/**
 * Module Chemistry — reference/knowledge base hoá học: nguyên tố, đồng vị, chất, tên gọi.
 *
 * <p>{@code allowedDependencies} bắt đầu ở mức tối thiểu: chỉ hạ tầng dùng
 * chung. Cần gọi module khác thì phải khai báo tường minh ở đây — mỗi lần
 * thêm một tên vào danh sách là một quyết định kiến trúc hiện rõ trong diff,
 * không phải một import lặng lẽ.
 *
 * <p>Sở hữu PostgreSQL schema {@code chemistry}. Module khác chỉ được chạm tới module
 * này qua named interface {@code api} và {@code dto}; mọi thứ còn lại là nội bộ.
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"common", "platform"},
        displayName = "Chemistry")
package vn.nitrogen.chemistry;
