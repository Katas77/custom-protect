
package com.example.custom_protect;

import com.example.custom_protect.dto.RegisterRequest;
import com.example.custom_protect.exception.UserAlreadyExistsException;
import com.example.custom_protect.model.Role;
import com.example.custom_protect.model.User;
import com.example.custom_protect.model.en.RoleType;
import com.example.custom_protect.repository.UserRepository;
import com.example.custom_protect.service.UserService;
import com.example.custom_protect.utils.PasswordEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
class UserServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void registerUser_success() {
        RegisterRequest req = new RegisterRequest("ivan", "pass123", "ivan@example.com");

        ResponseEntity<String> resp = userService.registerUser(req);

        assertEquals(200, resp.getStatusCodeValue(), "Ожидается HTTP 200 при успешной регистрации");
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().contains("ivan"), "Тело ответа должно содержать имя пользователя");

        assertTrue(userRepository.existsByEmail("ivan@example.com"));
        assertTrue(userRepository.existsByName("ivan"));

        User saved = userRepository.findByEmail("ivan@example.com").orElseThrow();
        assertEquals("ivan", saved.getName());
        assertNotEquals("pass123", saved.getPassword(), "Пароль должен быть захеширован");
        assertNotNull(saved.getRoles());
        assertFalse(saved.getRoles().isEmpty(), "Пользователь должен иметь роль по умолчанию");
    }

    @Test
    void registerUser_userAlreadyExists_throws() {
        User user = User.builder()
                .name("ivan")
                .email("ivan@example.com")
                .password(PasswordEncoder.encodePassword("secret"))
                .build();
        Role role = Role.from(RoleType.ROLE_USER);
        role.setUser(user);
        user.setRoles(List.of(role));
        userRepository.save(user);

        RegisterRequest duplicate = new RegisterRequest("ivan", "pass123", "ivan@example.com");

        assertThrows(UserAlreadyExistsException.class, () -> userService.registerUser(duplicate));
    }

    @Test
    void findById_and_deleteById() {
        User user = User.builder()
                .name("maria")
                .email("maria@example.com")
                .password(PasswordEncoder.encodePassword("pwd"))
                .build();
        Role role = Role.from(RoleType.ROLE_USER);
        role.setUser(user);
        user.setRoles(List.of(role));

        User saved = userRepository.save(user);

        ResponseEntity<User> found = userService.findById(saved.getId());
        assertEquals(200, found.getStatusCodeValue());
        assertEquals(saved.getId(), found.getBody().getId());

        ResponseEntity<Void> deleted = userService.deleteById(saved.getId());
        assertEquals(204, deleted.getStatusCodeValue());
        assertFalse(userRepository.existsById(saved.getId()));

        // удаление несуществующего
        ResponseEntity<Void> notFound = userService.deleteById(9999L);
        assertEquals(404, notFound.getStatusCodeValue());
    }
}
