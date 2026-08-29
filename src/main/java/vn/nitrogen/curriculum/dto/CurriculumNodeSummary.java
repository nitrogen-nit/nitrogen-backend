package vn.nitrogen.curriculum.dto;

import java.util.UUID;

/**
 * Một node trong cây chương trình.
 *
 * @param parentId null nếu là node gốc của một curriculum
 * @param depth    độ sâu tính từ gốc, dùng để render cây mà không phải đệ quy
 */
public record CurriculumNodeSummary(
        UUID id, UUID curriculumId, UUID parentId, String code, String title, int depth) {
}
