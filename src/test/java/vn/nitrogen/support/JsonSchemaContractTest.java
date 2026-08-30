package vn.nitrogen.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import java.io.InputStream;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * JSON Schema trong {@code contracts/} nằm đúng trên classpath và validate được
 * payload mẫu ở §13.2.
 *
 * <p>Test này canh gác cấu hình {@code <resource>} trong pom: nếu ai đó gỡ nó
 * ra, schema biến mất khỏi classpath và mọi validation ở runtime im lặng hỏng.
 */
@Tag("unit")
class JsonSchemaContractTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final JsonSchemaFactory FACTORY =
            JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);

    @ParameterizedTest
    @CsvSource({
            "single-choice.v1.json,    '{\"schema_version\":1,\"type\":\"SINGLE_CHOICE\",\"selected_option\":\"B\"}'",
            "true-false-group.v1.json, '{\"schema_version\":1,\"type\":\"TRUE_FALSE_GROUP\",\"statements\":{\"a\":true,\"b\":false,\"c\":true,\"d\":false}}'",
            "quantity.v1.json,         '{\"schema_version\":1,\"type\":\"QUANTITY\",\"value\":\"2.50\",\"unit\":\"mol\"}'"
    })
    void acceptsSpecExamplePayload(String schemaFile, String payload) throws Exception {
        assertThat(validate(schemaFile, payload)).isEmpty();
    }

    @Test
    void rejectsUnknownField() throws Exception {
        String payload = """
                {"schema_version":1,"type":"SINGLE_CHOICE","selected_option":"B","cheat":true}
                """;

        assertThat(validate("single-choice.v1.json", payload)).isNotEmpty();
    }

    @Test
    void rejectsWrongSchemaVersion() throws Exception {
        // schema_version là const, không phải minimum: payload của version khác
        // phải đi qua schema của version đó, không được lọt qua schema này.
        String payload = """
                {"schema_version":2,"type":"SINGLE_CHOICE","selected_option":"B"}
                """;

        assertThat(validate("single-choice.v1.json", payload)).isNotEmpty();
    }

    private Set<com.networknt.schema.ValidationMessage> validate(String schemaFile, String payload)
            throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/json-schema/responses/" + schemaFile)) {
            assertThat(in)
                    .as("json-schema/responses/%s phải có trên classpath — kiểm <resource> trong pom.xml",
                            schemaFile)
                    .isNotNull();

            JsonSchema schema = FACTORY.getSchema(in);
            JsonNode node = MAPPER.readTree(payload);
            return schema.validate(node);
        }
    }
}
