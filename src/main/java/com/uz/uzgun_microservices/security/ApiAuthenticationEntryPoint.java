package com.uz.uzgun_microservices.security;

import com.uz.uzgun_microservices.shared.error.ErrorCodes;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {
  private final SecurityErrorWriter writer;

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException, ServletException {
    response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
    writer.write(
        response, HttpStatus.UNAUTHORIZED, ErrorCodes.UNAUTHORIZED, "Authentication required");
  }
}
