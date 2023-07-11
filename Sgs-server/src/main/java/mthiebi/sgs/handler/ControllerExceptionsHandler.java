package mthiebi.sgs.handler;

import lombok.extern.slf4j.Slf4j;
import mthiebi.sgs.ExceptionKeys;
import mthiebi.sgs.SGSException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static mthiebi.sgs.handler.ExceptionUtils.getCustomValidationField;
import static mthiebi.sgs.handler.ExceptionUtils.isCustomValidationError;

@Slf4j
@ControllerAdvice(basePackages = "com.sgs")
public class ControllerExceptionsHandler {

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ConstraintViolationException.class)
    public List<ErrorInfo> handleConstraintViolationException(ConstraintViolationException ex) {
        return ex.getConstraintViolations().stream()
                .map(v -> {
                    ErrorInfo error = ErrorInfo.builder()
                            .field(v.getPropertyPath().toString())
                            .message(v.getMessage()).build();
                    if (error.getField() != null) {
                        String[] properties = error.getField().split("\\.");
                        error.setField(properties[properties.length - 1]);
                    }
                    return error;
                })
                .collect(Collectors.toList());
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(AccessDeniedException.class)
    public List<ErrorInfo> handleAccessDeniedException(AccessDeniedException ex) {
        ErrorInfo error = ErrorInfo.builder().message(ex.getMessage()).build();
        return List.of(error);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public List<ErrorInfo> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        return ex.getBindingResult().getAllErrors().stream()
                .filter(error -> error instanceof FieldError)
                .map(error -> (FieldError) error)
                .map(fieldError -> buildErrorInfoForDataValidationException(fieldError, request))
                .collect(Collectors.toList());
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public List<ErrorInfo> handleMissingServletRequestParameter(MissingServletRequestParameterException ex) {
        ErrorInfo error = ErrorInfo.builder()
                .message(String.format("Required parameter missing: (name = '%s', type = '%s')", ex.getParameterName(), ex.getParameterType()))
                .build();
        return List.of(error);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public List<ErrorInfo> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        ErrorInfo error = ErrorInfo.builder().message(ex.getMessage()).build();
        return List.of(error);
    }

    @ExceptionHandler(SGSException.class)
    public ResponseEntity<List<ErrorInfo>> handleLcmsException(SGSException ex, HttpServletRequest request) {
        ResponseEntity.BodyBuilder resp = getResponseEntityBuilder(ex);
        ErrorInfo error = ErrorInfo.builder()
                .message(ex.getMessage())
                .field(ex.getField())
                .build();
        String minifiedStackTrace = Arrays.stream(ex.getStackTrace())
                .map(StackTraceElement::toString)
                .filter(element -> element.startsWith("com.azry"))
                .collect(Collectors.joining(";"));
        String errorToLog = String.format("SGS exception[%s] trace[%s]", error.toString(), minifiedStackTrace);
        log.error(errorToLog);
        log.debug("SGS exception", ex);
        return resp.body(List.of(error));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<List<ErrorInfo>> handleRuntimeException(RuntimeException ex, HttpServletRequest request) {
        ResponseEntity.BodyBuilder resp = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR);
        log.error(ex.getMessage(), ex);
        String runtimeExceptionMessage = getRuntimeExceptionMessage(ex);
        ErrorInfo error = ErrorInfo.builder()
                .message(runtimeExceptionMessage)
                .build();
        return resp.body(List.of(error));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<List<ErrorInfo>> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        Throwable cause = ex.getCause();
        if (!(cause instanceof org.hibernate.exception.ConstraintViolationException) || cause.getCause() == null) {
            throw ex;
        }
        String message;
        Throwable rootCause = cause.getCause();
        if (StringUtils.containsIgnoreCase(rootCause.getMessage(), "DUPLICATE")) {
            message = ExceptionKeys.DATA_DUPLICATION;
        } else if (StringUtils.containsIgnoreCase(rootCause.getMessage(), "REFERENCE")) {
            message = ExceptionKeys.DATA_IN_USE;
        } else {
            throw ex;
        }
        log.error(ex.getMessage(), ex);
        ErrorInfo error = ErrorInfo.builder()
                .message(message)
                .build();
        return ResponseEntity.badRequest()
                .body(List.of(error));
    }

    private ResponseEntity.BodyBuilder getResponseEntityBuilder(SGSException ex) {
        switch (ex.getExceptionCode()) {
            case BAD_REQUEST:
                return ResponseEntity.badRequest();
            case UNAUTHORIZED:
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED);
            case FORBIDDEN:
                return ResponseEntity.status(HttpStatus.FORBIDDEN);
            case NOT_FOUND:
                return ResponseEntity.status(HttpStatus.NOT_FOUND);
            case CONFLICT:
                return ResponseEntity.status(HttpStatus.CONFLICT);
            default:
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    private String getRuntimeExceptionMessage(RuntimeException ex) {
        return ExceptionKeys.UNEXPECTED_ERROR;
    }


    private ErrorInfo buildErrorInfoForDataValidationException(FieldError fieldError, ServletRequest request) {
        String field = fieldError.getField();
        String defaultMessage = fieldError.getDefaultMessage();
        if (isCustomValidationError(fieldError)) {
            field = getCustomValidationField();
        }

        return ErrorInfo.builder()
                .field(field)
                .message(ExceptionKeys.INVALID_DATA)
                .build();
    }
}