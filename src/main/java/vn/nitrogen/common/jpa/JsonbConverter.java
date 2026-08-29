package vn.nitrogen.common.jpa;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Collections;
import java.util.Map;
import vn.nitrogen.common.error.BusinessException;
import vn.nitrogen.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * Map cột JSONB sang {@code Map<String, Object>}.
 *
 * <p>Dùng cho payload có schema_version tự mô tả (raw_response,
 * input_snapshot/output_snapshot, outbox payload). KHÔNG dùng để thay quan hệ
 * cốt lõi (§7).
 *
 * <p>Converter cố tình không tự động áp dụng: khai báo {@code @Convert} tại chỗ
 * để mỗi cột JSONB là một quyết định có ý thức.
 */
@Converter
public class JsonbConverter implements AttributeConverter<Map<String, Object>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> TYPE = new TypeReference<>() {
    };

    @Override
    public String convertToDatabaseColumn(Map<String, Object> attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR, "Không serialize được JSONB");
        }
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return MAPPER.readValue(dbData, TYPE);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR, "Không đọc được JSONB");
        }
    }
}
