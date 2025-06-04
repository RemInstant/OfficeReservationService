package org.reminstant.controller.advice;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.reminstant.exception.RoomNotFoundException;
import org.reminstant.exception.UnavailableReservationException;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.*;
import org.springframework.lang.NonNull;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.format.DateTimeParseException;
import java.util.List;

@Primary
@ControllerAdvice
public class ControllerExceptionHandler extends ResponseEntityExceptionHandler {

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                @NonNull HttpHeaders headers,
                                                                @NonNull HttpStatusCode status,
                                                                @NonNull WebRequest request) {
    List<FieldError> errorList = ex.getBindingResult().getFieldErrors();

    StringBuilder builder = new StringBuilder();
    for (FieldError error : errorList.subList(0, Math.min(10, errorList.size()))) {
      builder.append(error.getField()).append(": ")
          .append(error.getDefaultMessage()).append("; ");
    }
    builder.delete(builder.length() - 2, builder.length());

    String path = request.getDescription(false).replace("uri=", "");

    return buildResponse(status, headers, URI.create(path), builder.toString());
  }

  @ExceptionHandler(DuplicateKeyException.class)
  public ResponseEntity<Object> handleDuplicateKeyException(HttpServletRequest request) {
    return buildResponse(HttpStatus.CONFLICT, null,
        URI.create(request.getRequestURI()), "Duplicate content");
  }

  @ExceptionHandler(RoomNotFoundException.class)
  public ResponseEntity<Object> handleRoomNotFoundException(RoomNotFoundException ex,
                                                            HttpServletRequest request) {
    String detail = "No room with %s '%s'".formatted(ex.getRoomKey(), ex.getRoomValue());
    return buildResponse(HttpStatus.NOT_FOUND, null,
        URI.create(request.getRequestURI()), detail);
  }

  @ExceptionHandler(DateTimeParseException.class)
  public ResponseEntity<Object> handleRoomNotFoundException(DateTimeParseException ex,
                                                            HttpServletRequest request) {
    String detail = "Date '%s' is invalid".formatted(ex.getParsedString());
    return buildResponse(HttpStatus.BAD_REQUEST, null,
        URI.create(request.getRequestURI()), detail);
  }

  @ExceptionHandler(UnavailableReservationException.class)
  public ResponseEntity<Object> handleRoomNotFoundException(UnavailableReservationException ex,
                                                            HttpServletRequest request) {
    return buildResponse(HttpStatus.BAD_REQUEST, null,
        URI.create(request.getRequestURI()), ex.getMessage());
  }


  private ResponseEntity<Object> buildResponse(HttpStatusCode status, HttpHeaders headers,
                                               URI instance, String detail) {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
    problemDetail.setInstance(instance);

    return ResponseEntity.status(status)
        .headers(headers)
        .contentType(MediaType.APPLICATION_JSON)
        .body(problemDetail);
  }
}
