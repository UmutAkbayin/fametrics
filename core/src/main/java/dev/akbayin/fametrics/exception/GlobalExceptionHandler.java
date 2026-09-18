package dev.akbayin.fametrics.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        log.warn("Rejected invalid request to {}: {}", ex.getParameter().getExecutable(), errors);
        return errors;
    }

    /**
     * Covers a request-time failure calling the screener (or any other
     * RestClient-based upstream, e.g. FMP/SEC if they're ever called
     * synchronously in a request path in the future) — both connection
     * failures (ResourceAccessException) and non-2xx responses
     * (HttpStatusCodeException) are RestClientException subclasses. 503,
     * not 500: this isn't a bug in core, an upstream dependency is
     * temporarily unavailable — the distinction matters to a caller
     * deciding whether to retry.
     */
    @ExceptionHandler(RestClientException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Map<String, String> handleUpstreamServiceFailure(RestClientException ex) {
        log.error("A required upstream service failed", ex);
        return Map.of("error", "A required upstream service is currently unavailable. Please try again shortly.");
    }
}
