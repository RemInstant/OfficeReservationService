package org.reminstant.controller;

import org.reminstant.dto.http.ErrorWrapper;
import org.reminstant.dto.http.JwtTokenWrapper;
import org.reminstant.dto.http.UsernamePasswordData;
import org.reminstant.service.AppUserService;
import org.reminstant.service.JwtService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.Duration;

@RestController
public class CredentialsController {

  private final AppUserService appUserService;
  private final JwtService jwtService;

  CredentialsController(AppUserService appUserService, JwtService jwtService) {
    this.appUserService = appUserService;
    this.jwtService = jwtService;
  }

  @PostMapping(value = "${api.sign-up}")
  ResponseEntity<Object> signUp(@RequestBody UsernamePasswordData data) {
    try {
      appUserService.registerUser(data.username(), data.password());
    } catch (Exception ex) {
      return ResponseEntity.badRequest()
          .contentType(MediaType.APPLICATION_JSON)
          .body(new ErrorWrapper(ex.getMessage()));
    }

    return ResponseEntity.ok().build();
  }

  @PostMapping(value = "${api.sign-in}")
  ResponseEntity<Object> signIn(@RequestBody UsernamePasswordData data, Principal principal) {
    if (principal != null) {
      return ResponseEntity.badRequest()
          .contentType(MediaType.APPLICATION_JSON)
          .body(new ErrorWrapper("Already authorized"));
    }

    if (!appUserService.verifyUser(data.username(), data.password())) {
      return ResponseEntity.badRequest()
          .contentType(MediaType.APPLICATION_JSON)
          .body(new ErrorWrapper("Invalid credentials"));
    }

    String jwtToken = jwtService.generateToken(data.username(), Duration.ofDays(1));

    return ResponseEntity.ok(new JwtTokenWrapper(jwtToken));
  }

}
