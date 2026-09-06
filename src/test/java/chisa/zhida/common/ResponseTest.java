package chisa.zhida.common;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResponseTest {
    @Test
    void successAndFailureKeepApiContract() {
        Response<String> success = Response.success("ok");
        assertTrue(success.isSuccess());
        assertEquals("ok", success.getData());
        assertNull(success.getErrorCode());

        Response<Void> failure = Response.fail("bad");
        assertFalse(failure.isSuccess());
        assertEquals("bad", failure.getMessage());
        assertEquals("10000", failure.getErrorCode());
    }

    @Test
    void pageResponseCalculatesCeilingPageCount() {
        PageResponse<String> page = PageResponse.of(List.of("a", "b"), 5, 1, 2);
        assertEquals(3, page.pages());
        assertEquals(2, page.size());
        assertEquals(5, page.total());
    }
}
