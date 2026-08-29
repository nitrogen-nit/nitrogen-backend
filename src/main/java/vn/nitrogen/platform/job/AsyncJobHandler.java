package vn.nitrogen.platform.job;

/**
 * Hợp đồng cho một loại job bất đồng bộ (chấm bài, xuất PDF, rebuild progress).
 *
 * <p>Handler phải idempotent: cùng một {@code jobId} có thể được giao lại sau
 * khi worker chết giữa chừng.
 *
 * <p>TODO: bổ sung JobContext (payload, retry count, deadline) khi hiện thực
 * job framework.
 */
public interface AsyncJobHandler {

    /** Mã loại job, khớp {@code integration.async_jobs.job_type}. */
    String jobType();

    /**
     * Thực thi job.
     *
     * @param jobId khoá idempotency, cũng là khoá chính của bản ghi async_jobs
     */
    void handle(java.util.UUID jobId);
}
