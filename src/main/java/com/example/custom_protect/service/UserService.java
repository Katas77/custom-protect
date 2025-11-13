package com.example.custom_protect.service;

import com.example.custom_protect.dto.RegisterRequest;
import com.example.custom_protect.exception.UserAlreadyExistsException;
import com.example.custom_protect.model.Role;
import com.example.custom_protect.model.User;
import com.example.custom_protect.model.en.RoleType;
import com.example.custom_protect.repository.UserRepository;

import com.example.custom_protect.utils.PasswordEncoder;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public ResponseEntity<String> registerUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email()) || userRepository.existsByName(request.name())) {
            throw new UserAlreadyExistsException("Пользователь с таким email или именем уже существует");
        }

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(PasswordEncoder.encodePassword(request.password()))
                .build();

        Role role = Role.from(RoleType.ROLE_USER);
        role.setUser(user);
        user.setRoles(List.of(role));

        userRepository.save(user);
        return ResponseEntity.ok(MessageFormat.format("Пользователь с именем {0} успешно зарегистрирован", user.getName()));
    }

    public ResponseEntity<User> findById(Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        return userOpt.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    public ResponseEntity<Void> deleteById(Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
@PostConstruct
    public void admin() {
        User user = User.builder()
                .name("admin")
                .email("admin@email")
                .password(PasswordEncoder.encodePassword("admin"))
                .build();
        Role role = Role.from(RoleType.ROLE_ADMIN);
        role.setUser(user);
        user.setRoles(List.of(role));
        userRepository.save(user);
    }

}