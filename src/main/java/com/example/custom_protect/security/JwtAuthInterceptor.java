package com.example.custom_protect.security;


import com.example.custom_protect.exception.AuthenticationException;
import com.example.custom_protect.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {

    private final AuthService authService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Если handler не контроллерный метод — пропускаем
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod hm = (HandlerMethod) handler;

        // Сперва получаем аннотации с метода, если нет — с класса
        JwtAuth jwtAuth = hm.getMethodAnnotation(JwtAuth.class);
        if (jwtAuth == null) jwtAuth = hm.getBeanType().getAnnotation(JwtAuth.class);

        JwtAuthWithRoles jwtAuthWithRoles = hm.getMethodAnnotation(JwtAuthWithRoles.class);
        if (jwtAuthWithRoles == null) jwtAuthWithRoles = hm.getBeanType().getAnnotation(JwtAuthWithRoles.class);

        // Если ни одна из аннотаций не стоит — пропускаем
        if (jwtAuth == null && jwtAuthWithRoles == null) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new AuthenticationException("Authorization header is missing or invalid.");
        }

        String token = authHeader.substring(7);
        authService.validateToken(token);

        if (jwtAuthWithRoles != null) {
            String[] allowedRoles = jwtAuthWithRoles.allowedRoles();
            if (allowedRoles.length > 0) {
                boolean hasRole = authService.hasAnyRole(token, allowedRoles);
                if (!hasRole) {
                    throw new AuthenticationException("Access denied: insufficient roles.");
                }
            }
        }

        return true;
    }
}