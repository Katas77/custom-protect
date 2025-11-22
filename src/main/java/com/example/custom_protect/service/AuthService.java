package com.example.custom_protect.service;

import com.example.custom_protect.dto.LoginRequest;
import com.example.custom_protect.exception.AuthenticationException;
import com.example.custom_protect.jwt.JwtUtils;
import com.example.custom_protect.model.User;
import com.example.custom_protect.model.en.RoleType;
import com.example.custom_protect.repository.UserRepository;
import com.example.custom_protect.utils.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;

    public ResponseEntity<String> authenticate(LoginRequest request) {
        Optional<User> userOpt = userRepository.findByName(request.name());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (PasswordEncoder.matches(request.password(), user.getPassword())) {
                String token = jwtUtils.createToken(user.getName()); // токен содержит только имя
                return ResponseEntity.ok(token);
            }
        }
        throw new AuthenticationException("Неверный логин или пароль");
    }

    public void validateToken(String token) {
        if (!jwtUtils.isTokenValid(token)) {
            throw new AuthenticationException("Токен не валиден: протух или подпись некорректна.");
        }
    }
    public boolean hasAnyRole(String token, String[] requiredRoles) {
        String username = getUsernameFromToken(token);
        if (username == null || requiredRoles == null || requiredRoles.length == 0) {
            return false;
        }

        Set<RoleType> roles;
        try {
            roles = Arrays.stream(requiredRoles)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(String::toUpperCase)
                    .map(RoleType::valueOf)
                    .collect(Collectors.toSet());
        } catch (IllegalArgumentException ex) {
            // Если пришла неизвестная роль — считаем, что совпадений нет
            return false;
        }

        return userRepository.existsByNameAndRolesAuthorityIn(username, roles);
    }

    private String getUsernameFromToken(String token) {
        return jwtUtils.extractUsername(token);
    }
}