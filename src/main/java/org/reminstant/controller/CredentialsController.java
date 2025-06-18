package org.reminstant.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.reminstant.dto.http.request.UsernamePasswordDto;
import org.reminstant.dto.http.response.JwtTokenDto;
import org.reminstant.dto.http.response.ProblemDetailDto;
import org.reminstant.exception.AlreadyAuthorizedException;
import org.reminstant.exception.InvalidCredentialsException;
import org.reminstant.model.AppUser;
import org.reminstant.service.AppUserService;
import org.reminstant.service.JwtService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.Duration;
import java.util.Map;

@Validated
@RestController
@Tag(name = "Авторизация", description = "Обработка процессов, связанных с авторизацией пользователя")
public class CredentialsController {

  private final AppUserService appUserService;
  private final JwtService jwtService;

  CredentialsController(AppUserService appUserService, JwtService jwtService) {
    this.appUserService = appUserService;
    this.jwtService = jwtService;
  }

  @PostMapping(value = "${api.credentials.sign-up}")
  @Operation(summary = "Регистрация нового аккаунта")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "OK", content = @Content),
      @ApiResponse(responseCode = "400", description = "Логин занят / Невалидные данные", content = @Content(
          schema = @Schema(implementation = ProblemDetailDto.class),
          mediaType = MediaType.APPLICATION_JSON_VALUE))
  })
  ResponseEntity<Object> signUp(@Valid @RequestBody UsernamePasswordDto data) {
    appUserService.registerUser(data.username(), data.password());

    return ResponseEntity.ok().build();
  }

  @PostMapping(value = "${api.credentials.sign-in}")
  @Operation(summary = "Получение токена авторизации по логину и паролю")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "OK", content = @Content(
          schema = @Schema(implementation = JwtTokenDto.class),
          mediaType = MediaType.APPLICATION_JSON_VALUE)),
      @ApiResponse(responseCode = "400", description = "Недействительные учётные данные / Авторизация уже есть", content = @Content(
          schema = @Schema(implementation = ProblemDetailDto.class),
          mediaType = MediaType.APPLICATION_JSON_VALUE))
  })
  ResponseEntity<Object> signIn(@RequestBody UsernamePasswordDto data, Principal principal) {
    if (principal != null) {
      throw new AlreadyAuthorizedException();
    }

    AppUser user = appUserService.getUser(data.username());

    if (!appUserService.verifyUser(user, data.password())) {
      throw new InvalidCredentialsException("Invalid credentials");
    }

    Map<String, Object> claims = Map.of("Role", user.getRole());

    String jwtToken = jwtService.generateToken(claims, data.username(), Duration.ofDays(1));

    return ResponseEntity.ok(new JwtTokenDto(jwtToken));
  }

}
