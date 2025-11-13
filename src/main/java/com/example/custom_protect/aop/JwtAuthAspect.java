package com.example.custom_protect.aop;

import com.example.custom_protect.service.AuthService;
import com.example.custom_protect.exception.AuthenticationException;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Aspect
@Component
@RequiredArgsConstructor
public class JwtAuthAspect {

    private final AuthService authService;

    @Before("@annotation(JwtAuth)")
    public void checkJwtToken() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new AuthenticationException("Authorization header is missing or invalid.");
        }

        String token = authHeader.substring(7);
        authService.validateToken(token);
    }

    @Before("@annotation(jwtAuthWithRoles)")
    public void checkJwtTokenWithRoles(JwtAuthWithRoles jwtAuthWithRoles) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new AuthenticationException("Authorization header is missing or invalid.");
        }

        String token = authHeader.substring(7);
        authService.validateToken(token);

        String[] allowedRoles = jwtAuthWithRoles.allowedRoles();
        if (allowedRoles.length > 0) {
            boolean hasRole = authService.hasAnyRole(token, allowedRoles);
            if (!hasRole) {
                throw new AuthenticationException("Access denied: insufficient roles.");
            }
        }
    }
}