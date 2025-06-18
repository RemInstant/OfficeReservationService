package org.reminstant.controller.advice;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.reminstant.exception.*;
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

@Slf4j
//@Primary
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

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<Object> handleConstraintViolationException(ConstraintViolationException ex,
                                                                   WebRequest request) {
    StringBuilder builder = new StringBuilder();
    for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
      String location = violation.getPropertyPath().toString();
      location = location.substring(location.indexOf(".") + 1);
      location = location.substring(location.indexOf(".") + 1);
      builder.append(location).append(": ").append(violation.getMessage()).append("; ");
    }
    builder.delete(builder.length() - 2, builder.length());

    String path = request.getDescription(false).replace("uri=", "");

    return buildResponse(HttpStatus.BAD_REQUEST, null, URI.create(path), builder.toString());
  }

  @ExceptionHandler(InvalidCredentialsException.class)
  public ResponseEntity<Object> handleInvalidCredentialsException(InvalidCredentialsException ex,
                                                                  HttpServletRequest request) {
    return buildResponse(HttpStatus.BAD_REQUEST, null,
        URI.create(request.getRequestURI()), ex.getMessage());
  }

  @ExceptionHandler(OccupiedUsernameException.class)
  public ResponseEntity<Object> handleOccupiedUsernameException(HttpServletRequest request) {
    return buildResponse(HttpStatus.CONFLICT, null,
        URI.create(request.getRequestURI()), "Login is occupied");
  }

  @ExceptionHandler(AlreadyAuthorizedException.class)
  public ResponseEntity<Object> handleAlreadyAuthorizedException(HttpServletRequest request) {
    return buildResponse(HttpStatus.BAD_REQUEST, null,
        URI.create(request.getRequestURI()), "Already authorized users cannot authorize");
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

  @ExceptionHandler(ReservationNotFoundException.class)
  public ResponseEntity<Object> handleReservationNotFoundException(ReservationNotFoundException ex,
                                                                   HttpServletRequest request) {
    String detail = "No reservation with id '%s'".formatted(ex.getReservationId());
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
