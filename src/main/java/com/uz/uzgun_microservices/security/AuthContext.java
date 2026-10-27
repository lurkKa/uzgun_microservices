package com.uz.uzgun_microservices.security;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class AuthContext {
  public long currentUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth instanceof JwtAuthenticationToken jwt) {
      try {
        long userId = Long.parseLong(jwt.getToken().getSubject());
        if (userId <= 0) throw new BadCredentialsException("Invalid subject in token");
        return userId;
      } catch (NumberFormatException e) {
        throw new BadCredentialsException("Invalid subject in token");
      }
    }
    throw new AuthenticationCredentialsNotFoundException("No authenticated user");
  }
}
