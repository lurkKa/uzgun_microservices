package com.uz.uzgun_microservices.security;

import com.uz.uzgun_microservices.shared.error.ErrorCodes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApiAccessDeniedHandler implements AccessDeniedHandler {
  private final SecurityErrorWriter writer;

  @Override
  public void handle(HttpServletRequest req, HttpServletResponse res, AccessDeniedException ex)
      throws IOException {
    writer.write(res, HttpStatus.FORBIDDEN, ErrorCodes.FORBIDDEN, "Access denied");
  }
}
