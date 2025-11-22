package com.example.custom_protect;

import com.example.custom_protect.dto.LoginRequest;
import com.example.custom_protect.exception.AuthenticationException;
import com.example.custom_protect.jwt.JwtUtils;
import com.example.custom_protect.model.User;
import com.example.custom_protect.repository.UserRepository;
import com.example.custom_protect.service.AuthService;
import com.example.custom_protect.utils.PasswordEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    private User user;
    private final String username = "roma";
    private final String rawPassword = "secret123";
    private final String encodedPassword = "ENCODED_SECRET_123";

    @BeforeEach
    void setUp() {
        user = new User();
        user.setName(username);
        user.setPassword(encodedPassword);
    }

    @Test
    void authenticate_Success() {
        LoginRequest request = new LoginRequest(username, rawPassword);
        String token = "mocked.jwt.token";

        when(userRepository.findByName(username)).thenReturn(Optional.of(user));
        when(jwtUtils.createToken(username)).thenReturn(token);

        try (MockedStatic<PasswordEncoder> pw = mockStatic(PasswordEncoder.class)) {
            pw.when(() -> PasswordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);

            var response = authService.authenticate(request);

            assertThat(response).isNotNull();
            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(response.getBody()).isEqualTo(token);

            verify(jwtUtils).createToken(username);
            verify(userRepository).findByName(username);
        }
    }

    @Test
    void authenticate_UserNotFound_ThrowsAuthenticationException() {
        LoginRequest request = new LoginRequest("unknown", "any");
        when(userRepository.findByName("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.authenticate(request))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Неверный логин или пароль");

        verify(userRepository).findByName("unknown");
        verifyNoInteractions(jwtUtils);
    }

    @Test
    void authenticate_WrongPassword_ThrowsAuthenticationException() {
        LoginRequest request = new LoginRequest(username, "wrongPassword");
        when(userRepository.findByName(username)).thenReturn(Optional.of(user));

        try (MockedStatic<PasswordEncoder> pw = mockStatic(PasswordEncoder.class)) {
            pw.when(() -> PasswordEncoder.matches("wrongPassword", encodedPassword)).thenReturn(false);

            assertThatThrownBy(() -> authService.authenticate(request))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessage("Неверный логин или пароль");

            verify(userRepository).findByName(username);
            verifyNoInteractions(jwtUtils);
        }
    }

    @Test
    void validateToken_Valid_DoesNotThrow() {
        String token = "valid.token";
        when(jwtUtils.isTokenValid(token)).thenReturn(true);

        assertThatCode(() -> authService.validateToken(token)).doesNotThrowAnyException();

        verify(jwtUtils).isTokenValid(token);
    }

    @Test
    void validateToken_Invalid_ThrowsAuthenticationException() {
        String token = "invalid.token";
        when(jwtUtils.isTokenValid(token)).thenReturn(false);

        assertThatThrownBy(() -> authService.validateToken(token))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Токен не валиден: протух или подпись некорректна.");

        verify(jwtUtils).isTokenValid(token);
    }

    @Test
    void hasAnyRole_MatchingRole_ReturnsTrue() {
        String token = "token";
        when(jwtUtils.extractUsername(token)).thenReturn(username);
        when(userRepository.existsByNameAndRolesAuthorityIn(eq(username), anySet())).thenReturn(true);

        boolean result = authService.hasAnyRole(token, new String[]{"ROLE_USER"});

        assertThat(result).isTrue();

        verify(jwtUtils).extractUsername(token);
        verify(userRepository).existsByNameAndRolesAuthorityIn(eq(username), anySet());
    }

    @Test
    void hasAnyRole_NoMatchingRole_ReturnsFalse() {
        String token = "token";
        when(jwtUtils.extractUsername(token)).thenReturn(username);
        when(userRepository.existsByNameAndRolesAuthorityIn(eq(username), anySet())).thenReturn(false);

        boolean result = authService.hasAnyRole(token, new String[]{"ROLE_ADMIN"});

        assertThat(result).isFalse();

        verify(jwtUtils).extractUsername(token);
        verify(userRepository).existsByNameAndRolesAuthorityIn(eq(username), anySet());
    }

    @Test
    void hasAnyRole_UnknownRoleString_ReturnsFalse() {
        String token = "token";
        when(jwtUtils.extractUsername(token)).thenReturn(username);

        boolean result = authService.hasAnyRole(token, new String[]{"NOT_EXIST"});
        assertThat(result).isFalse();

        verify(jwtUtils).extractUsername(token);
        verifyNoInteractions(userRepository);
    }

    @Test
    void hasAnyRole_NullOrEmptyRequiredRoles_ReturnsFalse() {
        String token = "token";
        when(jwtUtils.extractUsername(token)).thenReturn(username);
        assertThat(authService.hasAnyRole(token, null)).isFalse();
        verify(jwtUtils, times(1)).extractUsername(token);
        verifyNoInteractions(userRepository);
    }

    @Test
    void hasAnyRole_NullUsername_ReturnsFalse() {
        String token = "token";
        when(jwtUtils.extractUsername(token)).thenReturn(null);

        boolean result = authService.hasAnyRole(token, new String[]{"ROLE_USER"});

        assertThat(result).isFalse();
        verify(jwtUtils).extractUsername(token);
        verifyNoInteractions(userRepository);
    }
}
