package com.ticket.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard API response envelope — all endpoints return this wrapper")
public class ApiResult<T> {

    @Schema(description = "Business status code. 200 = success, 4xxxx = client error, 50000 = server error.", example = "200")
    private int code;

    @Schema(description = "Human-readable message", example = "success")
    private String message;

    @Schema(description = "Response payload — type varies by endpoint. Null on error or void responses.")
    private T data;

    private ApiResult(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(200, "success", data);
    }

    public static <T> ApiResult<T> success() {
        return new ApiResult<>(200, "success", null);
    }

    public static <T> ApiResult<T> error(int code, String message) {
        return new ApiResult<>(code, message, null);
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
    public T getData() { return data; }
}
