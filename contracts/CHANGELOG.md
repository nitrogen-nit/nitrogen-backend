# Contract changelog

Lịch sử mọi thay đổi contract. Đây là hồ sơ để tra cứu khi regrade dữ liệu cũ:
bản ghi `raw_response` ghi hôm nay còn nằm trong DB nhiều năm (§14.2 — archive
lạnh, không purge tuỳ tiện), nên contract sinh ra nó phải còn đọc được.

## Quy tắc (§13.3)

- KHÔNG đổi ngữ nghĩa của một `schema_version` đã phát hành. Cần đổi thì tạo
  version mới và viết adapter đọc version cũ.
- Thêm field tuỳ chọn: không phá vỡ tương thích.
- Đổi kiểu, đổi tên, bỏ field bắt buộc, thu hẹp tập giá trị: **breaking**.
- `answer-spec/` và `scoring-rule/` KHÔNG được đóng gói vào client cho web (§18).

## Chưa phát hành

- `responses/single-choice.v1.json` — khởi tạo
- `responses/true-false-group.v1.json` — khởi tạo
- `responses/quantity.v1.json` — khởi tạo
