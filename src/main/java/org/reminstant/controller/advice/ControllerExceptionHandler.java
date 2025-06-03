package org.reminstant.controller.advice;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
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

//  @ExceptionHandler(ConstraintViolationException.class)
//  public ResponseEntity<Object> handleConstraintViolationException(ConstraintViolationException ex,
//                                                                   WebRequest request) {
//    StringBuilder builder = new StringBuilder();
//    for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
//      String location = violation.getPropertyPath().toString();
//      location = location.substring(location.indexOf(".") + 1);
//      location = location.substring(location.indexOf(".") + 1);
//      builder.append(location).append(": ").append(violation.getMessage()).append("; ");
//    }
//    builder.delete(builder.length() - 2, builder.length());
//
//    return buildResponse(HttpStatus.BAD_REQUEST, URI.create("/123"), builder.toString());
//  }

  @ExceptionHandler(DuplicateKeyException.class)
  public ResponseEntity<Object> handleDuplicateKeyException(HttpServletRequest request) {
    return buildResponse(HttpStatus.CONFLICT, null, URI.create(request.getRequestURI()), "Duplicate content");
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
