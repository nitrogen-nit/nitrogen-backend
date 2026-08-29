# nitrogen-backend

Nitrogen Spring Boot Modular Monolith backend — nền tảng học và luyện thi Hoá học THPT.

Nguồn sự thật về thiết kế: *Nitrogen System Design & Database Design Specification v1.0*
(repo `nitrogen-docs`). Các tham chiếu §x.y trong code trỏ về tài liệu đó.

| | |
|---|---|
| Baseline | Spring Boot 4.1.1 · Spring Modulith 2.1.1 · Java 21 · Maven |
| Database | PostgreSQL — schema theo module, Flyway là cơ chế đổi schema duy nhất |
| Messaging | RabbitMQ — durable event đi qua outbox `integration.outbox_events` (§11.4) |
| Object storage | MinIO / S3 |

## Chạy

```bash
# Cần PostgreSQL và RabbitMQ ở local, hoặc trỏ NITROGEN_DB_URL sang nơi khác
./mvnw spring-boot:run -Dspring-boot.run.profiles=web
./mvnw spring-boot:run -Dspring-boot.run.profiles=worker
```

Khi chạy với RDS hoặc database ngoài local, tạo `.env.local` từ `.env.example`.
Spring Boot tự đọc file này qua `spring.config.import`, kể cả khi bấm Run trong
IntelliJ. Không commit `.env.local`.

```bash
cp .env.example .env.local
# sửa .env.local bằng endpoint/user/password thật
scripts/run-web-dev.sh
```

Khi chạy bằng IntelliJ Run/Debug Configuration, để Working directory là root của
module `nitrogen-backend`. Nếu `.env.local` có `spring.profiles.active=web`, ô
Active profiles có thể để trống; còn không thì nhập `web`.

Một artifact, hai chế độ chạy — **không** phải hai ứng dụng:

| Profile | Vai trò |
|---|---|
| `web` | REST, sync grading, scheduler auto-submit, outbox publisher |
| `worker` | RabbitMQ consumer (PDF / AI / batch regrade) |
| `prod` | `flyway.enabled=false` — migration là step CI/CD riêng (§16.2.4) |

Profile `web` và `worker` đều kéo theo profile `core` (khai báo ở
`spring.profiles.group`), nơi các facade cross-module `@Profile("core")` được đăng ký.

## Test

```bash
./mvnw verify              # tất cả — cần Docker cho Testcontainers
./mvnw verify -Pno-docker  # bỏ qua test gắn @Tag("docker"); chỉ còn kiến trúc + contract
```

Integration test dùng PostgreSQL thật qua Testcontainers, không dùng H2: thiết kế
phụ thuộc vào partial unique index, JSONB + `jsonb_typeof`, `FOR UPDATE SKIP LOCKED`
và FK composite — test trên engine khác chỉ cho cảm giác an toàn giả.

## Cấu trúc

```
contracts/                    # nguồn sự thật cho OpenAPI + JSON Schema
├── openapi/nitrogen-api.yaml
└── json-schema/
    ├── responses/            # 🌐 public — đóng gói cho web
    ├── answer-spec/          # 🔒 backend-only (§18: answer key không ra client)
    ├── scoring-rule/         # 🔒 backend-only
    └── messages/             # 🔒 backend-only

src/main/java/vn/nitrogen/
├── common/                   # base entity, UUIDv7, error model, JSONB converter
├── platform/                 # security, messaging, outbox, job, observability
└── <12 module nghiệp vụ>/
```

`contracts/` nằm **ngang hàng** `src/`, không nằm trong `src/main/resources`. Backend
đọc JSON Schema từ đó qua `<resource>` trong `pom.xml` — không giữ bản copy, vì một
bản copy là tái tạo drift ngay bên trong repo.

### 12 module ↔ 12 schema

`identity` · `curriculum` · `chemistry` · `content` · `assessment` · `examination` ·
`practice` · `progress` · `flashcard` · `simulation` · `integration` · `administration`

Mỗi module sở hữu đúng một schema PostgreSQL cùng tên. Không module nào ghi vào
schema của module khác (§8).

Layer bên trong module — **chỉ tạo thư mục khi có nội dung**, không tạo rỗng:

```
api/ config/ domain/ dto/ exception/ repository/ service/ messaging/ util/ web/
```

## Thêm một module mới

1. Tạo package `vn/nitrogen/<module>/package-info.java`:

   ```java
   @org.springframework.modulith.ApplicationModule(
           allowedDependencies = {"common", "platform"},
           displayName = "Tên module")
   package vn.nitrogen.<module>;
   ```

   Muốn gọi module khác thì thêm tên module đó vào `allowedDependencies` —
   `ModularityTest` fail nếu có phụ thuộc chưa khai báo.

2. Nếu module có `api/` hoặc `dto/`, thêm `package-info.java` cho từng package
   với `@NamedInterface("api")` / `@NamedInterface("dto")`. Không có nó,
   Spring Modulith coi cả hai là nội bộ và module khác không import được.

3. Tạo `src/main/resources/db/migration/<module>/V<ts>__<module>_create_schema.sql`.

4. Copy một file trong `src/test/java/vn/nitrogen/architecture/<module>/`, đổi
   package và `MODULE_PACKAGE`. File đó không chứa luật nào — nó chỉ khai báo
   package, và module mới lập tức chịu toàn bộ luật hiện có.

## Quy ước migration

```
src/main/resources/db/migration/<module>/V<yyyyMMddHHmm>__<module>_<mô tả>.sql
```

Version phải **duy nhất toàn repository** — không đánh số độc lập theo thư mục.
Dùng timestamp nên hai nhánh song song không đụng version khi merge.

Cấu hình là `spring.flyway.locations=classpath:db/migration` — **không** có `/**`.
Flyway chỉ hỗ trợ wildcard cho location kiểu `filesystem:`; với `classpath:` nó
quét đệ quy sẵn. Thêm `/**` khiến nó không khớp thư mục nào và chạy 0 migration
mà không báo lỗi.

Không sửa migration đã chạy trên môi trường chung; đổi schema thì viết migration
mới theo expand/contract (§16.3).

## Ranh giới module

Enforce tự động ở hai tầng, bổ sung cho nhau:

| Công cụ | Bắt được gì |
|---|---|
| Spring Modulith `verify()` | phụ thuộc vòng, phụ thuộc không khai báo trong `allowedDependencies` |
| ArchUnit | truy cập ngoài `api/`+`dto/`, sai tầng, JPA association xuyên module, external call trong transaction, `@ManyToOne` EAGER, Lombok `@Data` trên entity |

Luật chặt hơn Artemis một bậc có chủ đích: Artemis cho phép truy cập `domain/` xuyên
module, Nitrogen cấm — §4.3 quy định chỉ lưu foreign key UUID và tra cứu qua module
API, nên mở `domain/` sẽ mở lại đúng cánh cửa đó.

## Quyết định đã chốt trong skeleton này

| Quyết định | Lý do |
|---|---|
| **Không** dùng event publication registry của Spring Modulith | Cơ chế durable event của Nitrogen là outbox `integration.outbox_events`, đặc tả nguyên văn ở §11.4. Hai cơ chế song song sẽ chia đôi đường phát event. Registry còn cần bảng `event_publication` mà đặc tả không mô tả — chính là BL-06. |
| `flyway.schemas = flyway_history` | Bảng lịch sử của Flyway tách khỏi 12 schema nghiệp vụ; `MigrateFromEmptyDbTest` kiểm số schema mà không phải trừ đi bảng hạ tầng. |
| Request không có credential trả **401**, không phải 403 | Mặc định của Spring Security cho anonymous là 403, khiến client không phân biệt được "chưa đăng nhập" với "đăng nhập rồi nhưng không đủ quyền". |
| `management.health.rabbit.enabled=false` ở `web`, `true` ở `worker` | Web replica vẫn phục vụ được request đọc khi broker chết (outbox đỡ phần ghi event). Worker mất broker là mất khả năng làm việc. |
| Bean `OpenAPI` trong `platform/openapi` | Để spec springdoc sinh ra khớp header `contracts/openapi/nitrogen-api.yaml`; không có nó, contract-check lệch ngay dòng đầu. |

## Trạng thái

Đây là **skeleton**: cấu trúc, cấu hình và ràng buộc kiến trúc đã sẵn sàng; chưa có
business logic. `identity.users` mới ở mức tối thiểu để FK của `practice_attempts` có
đích tham chiếu.

DDL `practice` và `integration` theo nguyên văn đặc tả §11, kèm comment `TODO(BL-xx)`
tại các điểm báo cáo đánh giá đã ghi nhận (BL-02, BL-04, BL-07, BL-11, BL-13, BL-15,
BL-16) — giữ nguyên để schema và tài liệu không lệch nhau cho tới khi các mục đó
được chốt.
