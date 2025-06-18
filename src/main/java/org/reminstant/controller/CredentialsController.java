package org.reminstant.controller;

import io.jsonwebtoken.JwtException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.reminstant.dto.http.request.UsernamePasswordDto;
import org.reminstant.dto.http.request.JwtTokenDto;
import org.reminstant.dto.http.response.ProblemDetailDto;
import org.reminstant.exception.AlreadyAuthorizedException;
import org.reminstant.exception.InvalidCredentialsException;
import org.reminstant.model.AppUser;
import org.reminstant.service.AppUserService;
import org.reminstant.service.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.Duration;

@Validated
@RestController
@Tag(name = "Авторизация", description = "Обработка процессов, связанных с авторизацией пользователя")
public class CredentialsController {
  private final AppUserService appUserService;
  private final JwtService jwtService;

  @Value("${token.ttl.access}")
  private int accessTokenTTL;
  @Value("${token.ttl.refresh}")
  private int refreshTokenTTL;

  CredentialsController(AppUserService appUserService, JwtService jwtService) {
    this.appUserService = appUserService;
    this.jwtService = jwtService;
  }

  @PostMapping(value = "${api.credentials.sign-up}")
  @Operation(summary = "Регистрация нового аккаунта")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "OK", content = @Content),
      @ApiResponse(responseCode = "400", description = "Логин занят / Невалидные данные", content = @Content(
          schema = @Schema(implementation = ProblemDetailDto.class),
          mediaType = MediaType.APPLICATION_JSON_VALUE))
  })
  ResponseEntity<Object> signUp(@Valid @RequestBody UsernamePasswordDto data) {
    appUserService.registerUser(data.username(), data.password());

    return ResponseEntity.noContent().build();
  }

  @PostMapping(value = "${api.credentials.sign-in}")
  @Operation(summary = "Получение токенов авторизации по логину и паролю")
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

    String accessToken = jwtService.generateAccessToken(user, Duration.ofSeconds(accessTokenTTL));
    String refreshToken = jwtService.generateRefreshToken(user, Duration.ofSeconds(refreshTokenTTL));

    return ResponseEntity.ok(new JwtTokenDto(accessToken, refreshToken));
  }

  @PostMapping(value = "${api.credentials.sign-out}")
  @Operation(
      summary = "Деактивация авторизационных токенов",
      security = @SecurityRequirement(name = "BearerAuth"))
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "OK", content = @Content),
      @ApiResponse(responseCode = "401", description = "Невалидный/истёкший токен доступа", content = @Content),
      @ApiResponse(responseCode = "403", description = "Нет доступа (отсутствует авторизация)", content = @Content)
  })
  ResponseEntity<Object> signOut(@RequestBody JwtTokenDto data, Principal principal) {
    jwtService.blacklistToken(data.accessToken(), Duration.ofSeconds(accessTokenTTL));
    appUserService.incrementUserTokenVersion(principal.getName());

    return ResponseEntity.noContent().build();
  }

  @PostMapping(value = "${api.credentials.refresh-token}")
  @Operation(summary = "Обновление пары токенов по refresh-токену")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "OK", content = @Content(
          schema = @Schema(implementation = JwtTokenDto.class),
          mediaType = MediaType.APPLICATION_JSON_VALUE)),
      @ApiResponse(responseCode = "400", description = "Недействительные учётные данные", content = @Content(
          schema = @Schema(implementation = ProblemDetailDto.class),
          mediaType = MediaType.APPLICATION_JSON_VALUE))
  })
  ResponseEntity<Object> refreshToken(@RequestBody JwtTokenDto data) {
    if (jwtService.isRefreshTokenInvalid(data.refreshToken())) {
      throw new InvalidCredentialsException("Invalid refresh token");
    }

    String username;
    String role;
    try {
      username = jwtService.extractUsername(data.refreshToken());
      role = jwtService.extractRole(data.refreshToken());
    } catch (JwtException ex) {
      throw new InvalidCredentialsException("Invalid refresh token");
    }

    jwtService.blacklistToken(data.accessToken(), Duration.ofSeconds(accessTokenTTL));

    String accessToken = jwtService.generateAccessToken(username, role, Duration.ofSeconds(accessTokenTTL));
    String refreshToken = jwtService.generateRefreshToken(username, role, Duration.ofSeconds(refreshTokenTTL));

    return ResponseEntity.ok(new JwtTokenDto(accessToken, refreshToken));
  }
}
