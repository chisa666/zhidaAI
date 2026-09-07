package chisa.zhida.common;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Response<Void>> validation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst().map(e -> e.getDefaultMessage()).orElse("参数错误");
        return ResponseEntity.badRequest().body(Response.fail(message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Response<Void>> violation(ConstraintViolationException ex) {
        return ResponseEntity.badRequest().body(Response.fail(ex.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Response<Void>> notFound(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Response.fail("接口不存在"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Response<Void>> other(Exception ex) {
        log.error("Unhandled request exception", ex);
        return ResponseEntity.internalServerError().body(Response.fail("服务暂时不可用，请稍后重试"));
    }
}