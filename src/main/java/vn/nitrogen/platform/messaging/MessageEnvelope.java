package vn.nitrogen.platform.messaging;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Envelope chuẩn cho mọi message qua RabbitMQ (§17.3).
 *
 * <p>Đây là <b>contract xuyên phiên bản</b>, không chỉ xuyên process: lúc rolling
 * deploy, message do backend phiên bản mới phát có thể bị worker phiên bản cũ tiêu
 * thụ. Vì vậy {@code schemaVersion} phải được consumer kiểm tra tường minh, và
 * ngữ nghĩa của một schemaVersion đã phát hành thì không được đổi (§13.3).
 *
 * @param messageId     khoá dedup phía consumer — ghi vào integration.processed_messages
 * @param messageType   tên loại message, ví dụ DocumentRenderRequested
 * @param schemaVersion phiên bản cấu trúc payload
 * @param correlationId nối các message thuộc cùng một luồng nghiệp vụ
 * @param causationId   messageId đã sinh ra message này
 * @param aggregateType loại aggregate nguồn, ví dụ DOCUMENT_EXPORT
 * @param aggregateId   id aggregate nguồn
 * @param occurredAt    thời điểm sự kiện xảy ra (UTC), không phải lúc publish
 * @param payload       nội dung, validate theo contracts/json-schema/messages/
 */
public record MessageEnvelope(
        UUID messageId,
        String messageType,
        int schemaVersion,
        UUID correlationId,
        UUID causationId,
        String aggregateType,
        UUID aggregateId,
        Instant occurredAt,
        JsonNode payload) {
}
