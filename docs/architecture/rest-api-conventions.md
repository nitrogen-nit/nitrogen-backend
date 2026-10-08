# REST API conventions

Nitrogen exposes REST APIs through the `web` layer of each business module.
The OpenAPI contract lives in `contracts/openapi/nitrogen-api.yaml`; controller
code must not drift away from that contract.

## Paths

- Base path is `/api/v1`.
- Resources use plural nouns: `/api/v1/practice-attempts`.
- Path segments use kebab-case.
- Do not put verbs in URLs. Use HTTP methods for actions.
- Command endpoints are allowed only when a resource-shaped URL is unclear; document the reason in the API contract and PR.

## DTO boundary

- Request and response bodies use DTOs from `dto` packages.
- Controllers must not accept or return JPA entities, including through wrappers such as `ResponseEntity<Entity>`, `List<Entity>`, `Page<Entity>` or `Optional<Entity>`.
- Do not use `Map<String, Object>` as a public REST contract unless the shape is intentionally dynamic and documented in OpenAPI or JSON Schema.
- Use Bean Validation annotations on request DTOs.

## Status codes

- `200 OK`: successful read or idempotent update with response body.
- `201 Created`: resource created; include `Location` when the resource has a stable URL.
- `202 Accepted`: async command accepted.
- `204 No Content`: successful command without body.
- `400 Bad Request`: invalid input.
- `401 Unauthorized`: no valid authentication.
- `403 Forbidden`: authenticated but not allowed.
- `404 Not Found`: resource does not exist or is not visible to the caller.
- `409 Conflict`: state conflict, duplicate command or optimistic lock conflict.
- `422 Unprocessable Entity`: syntactically valid request that violates domain rules.

## Errors

- Error responses are RFC 7807 `application/problem+json` produced by `GlobalExceptionHandler`:

  | Field | Meaning |
  |---|---|
  | `type` | `https://docs.nitrogen.vn/errors/<error_code>` |
  | `title`, `status`, `detail` | Standard ProblemDetail fields |
  | `errorCode` | Stable `ErrorCode` name; clients branch on this, never on `detail` |
  | `fields` | Validation failures only: `"<field>: <message>"` |

- Services throw `BusinessException(ErrorCode, status, message)`; controllers never build error responses themselves.
- Bean Validation failures and unreadable bodies map to `400 VALIDATION_FAILED`; unexpected exceptions map to `500 INTERNAL_ERROR` with a generic message.
- Do not expose stack traces, SQL details, table names or driver exceptions.
- Every response, including errors, carries the `X-Correlation-ID` header so logs and client errors can be connected.

## Pagination, filtering and sorting

- Use `page`, `size` and `sort` for pageable endpoints.
- `sort` uses stable field names from the DTO/API contract, not database column names.
- Filters must be explicit query parameters. Avoid generic catch-all maps in controllers.
- Default page size must be documented and bounded.

## Correlation and idempotency

- Accept `X-Correlation-ID` from callers when valid; generate one when missing (see [Observability](../observability.md)).
- Return the correlation ID in the `X-Correlation-ID` response header.
- Important command endpoints should be idempotent and document the replay semantics. The current implementation takes the key in the request body: `StartPracticeAttemptRequest.idempotencyKey`.

## Security

- Every endpoint requires authentication by default; anonymous requests get `401`, not `403`.
- Public endpoints are listed explicitly in `platform/security/SecurityConfig`; today `POST /api/v1/auth/registrations`, `POST /api/v1/auth/email-verifications`, `GET /actuator/health/**`, `/actuator/info`, `/actuator/prometheus` and OpenAPI/Swagger UI. CSRF protection is skipped only for the two public auth endpoints.
- JWT resource server and role-based authorization are not configured yet.

## Enforcement

Automated rules currently enforce:

- `@RestController` classes live under `web`.
- REST controllers do not depend on repositories or `EntityManager`.
- REST controllers do not keep mutable instance state.
- REST method signatures do not expose JPA/domain types, including through generic wrappers.
- Controller transaction boundaries are forbidden.

Review checklist covers rules that are intentionally not automated, such as URL wording, command endpoint justification and whether business logic is too large for a controller.
