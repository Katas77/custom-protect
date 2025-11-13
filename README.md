# README — Custom JWT security без spring-boot-starter-security


## 1. Краткое описание
Проект реализует минимальный кастомный механизм аутентификации/авторизации на основе JWT без использования `spring-boot-starter-security`. Для защиты конечных точек используется AOP (асперекты), которые проверяют JWT и роли через методы `AuthService`:
- @JwtAuth — проверка подлинности токена (Bearer).
- @JwtAuthWithRoles(allowedRoles = {...}) — проверка наличия хотя бы одной роли.

---

## 2. Фичи
- Аутентификация через JWT (создание/валидация в `JwtUtils`).
- Централизованная проверка токена и ролей через AOP-аспект (`JwtAuthAspect`).
- Минимальные зависимости — можно обойтись без полного Spring Security.
- Поддержка BCrypt (рекомендуется) или jBCrypt (если хотите полностью отказаться от артефактов Spring).
- Примеры публичного, защищённого и ролевого эндпойнтов.

---



## 3. Конфигурация (application.properties / env)
Пример переменных, которые нужно задать (в application.properties или в окружении):

```properties
# JWT
jwt.secret=${JWT_SECRET:}                 # строка или Base64-ключ (обязательно установить в prod)
jwt.expiration-ms=${JWT_EXPIRATION_MS:3600000}
```

Рекомендация: в production задавайте JWT_SECRET через env/секретный хранилище; ключ должен быть минимум 32 байта (256 бит) или Base64-encoded.

---

## 4. Аннотации 

```java
    @GetMapping("/public")
public String publicEndpoint() {
    return "This is public";
}

@GetMapping("/secure")
@JwtAuth
public String secureEndpoint() {
    return "This is secured by JWT only";
}

@GetMapping("/admin")
@JwtAuthWithRoles(allowedRoles = {"ROLE_ADMIN"})
public String adminEndpoint() {
    return "This is admin only";
}

@GetMapping("/authenticated")
@JwtAuthWithRoles(allowedRoles = {"ROLE_USER", "ROLE_ADMIN"})
public String userOrAdminEndpoint() {
    return "This is available to USER or ADMIN";
}
```



---

## 5. Как работает AOP-решение (логика)
- При вызове метода с @JwtAuth или @JwtAuthWithRoles аспект получает заголовок Authorization.
- Если заголовок отсутствует или не "Bearer ...", бросаем AuthenticationException.
- Аспект вызывает `AuthService.validateToken(token)` для базовой валидации (подпись + срок).
- Для ролей аспект вызывает `AuthService.hasAnyRole(token, allowedRoles)` — этот метод извлекает роли из токена (claim "roles") или загружает роли пользователя и сверяет.

Важно: AOP запускается после разрешения Spring MVC и до выполнения метода контроллера. Однако:
- AOP не перехватит вызовы, если метод вызывается внутри того же бина напрямую (self-invocation).
- Если вам нужна фильтрация на уровне HTTP (например, для статических ресурсов), лучше использовать Filter.

---

## 6. Примеры curl для тестирования

Регистрация:
curl -X POST -H "Content-Type: application/json" -d '{"name":"john","password":"pass","email":"john@example.com"}' http://localhost:8080/api/v1/auth/register

Логин:
curl -X POST -H "Content-Type: application/json" -d '{"name":"john","password":"pass"}' http://localhost:8080/api/v1/auth/login

Вызов публичного:
curl http://localhost:8080/public

Вызов защищённого:
curl -H "Authorization: Bearer <TOKEN>" http://localhost:8080/secure

Вызов ролевого:
curl -H "Authorization: Bearer <TOKEN>" http://localhost:8080/admin

---


##  Контакты

- **Разработчик**: [Роман]
- **Версия**: 1.0.0
- **Дата**:  22 Ноябрь 2025



---


✉ **Контакты**: [krp77@mail.ru](mailto:krp77@mail.ru)

---

