package vn.nitrogen.integration.api;

/** Business side effect chạy cùng transaction với bản ghi chống xử lý trùng. */
@FunctionalInterface
public interface ProcessedMessageAction {

    void run() throws Exception;
}
