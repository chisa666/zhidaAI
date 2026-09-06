package chisa.zhida.common;

public final class Response<T> {
    private final boolean success;
    private final String message;
    private final String errorCode;
    private final T data;
    public Response(boolean success, String message, String errorCode, T data) { this.success = success; this.message = message; this.errorCode = errorCode; this.data = data; }
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getErrorCode() { return errorCode; }
    public T getData() { return data; }
    public static <T> Response<T> success(T data) { return new Response<>(true, null, null, data); }
    public static <T> Response<T> success() { return success(null); }
    public static <T> Response<T> fail(String message) { return new Response<>(false, message, "10000", null); }
}
