package chisa.zhida.common;

import java.util.List;

public record PageResponse<T>(boolean success, String message, String errorCode, List<T> data,
                             long total, long size, long current, long pages) {
    public static <T> PageResponse<T> of(List<T> data, long total, long current, long size) {
        long pages = size == 0 ? 0 : (total + size - 1) / size;
        return new PageResponse<>(true, null, null, data, total, size, current, pages);
    }
}
