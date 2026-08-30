package vn.nitrogen.common.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import vn.nitrogen.common.error.BusinessException;
import vn.nitrogen.common.error.ErrorCode;

@Tag("unit")
class JsonbConverterTest {

    private final JsonbConverter converter = new JsonbConverter();

    @Test
    void keepsNullDatabaseValueAsNull() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }

    @Test
    void treatsMissingEntityValueAsEmptyMap() {
        assertThat(converter.convertToEntityAttribute(null)).isEmpty();
        assertThat(converter.convertToEntityAttribute("  ")).isEmpty();
    }

    @Test
    void roundTripsJsonObject() {
        Map<String, Object> payload = Map.of(
                "schema_version", 1,
                "type", "QUANTITY",
                "unit", "mol");

        String json = converter.convertToDatabaseColumn(payload);

        assertThat(converter.convertToEntityAttribute(json)).containsAllEntriesOf(payload);
    }

    @Test
    void wrapsInvalidJsonReadAsBusinessException() {
        assertThatThrownBy(() -> converter.convertToEntityAttribute("{invalid"))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR);
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    assertThat(ex).hasMessage("Không đọc được JSONB");
                });
    }

    @Test
    void wrapsInvalidJsonWriteAsBusinessException() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("self", payload);

        assertThatThrownBy(() -> converter.convertToDatabaseColumn(payload))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR);
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    assertThat(ex).hasMessage("Không serialize được JSONB");
                });
    }
}
