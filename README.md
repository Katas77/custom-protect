# Custom JWT security без spring-boot-starter-security

- механизм аутентификации/авторизации надёжен и корректно работает как для внешних вызовов, так и при вызове внутри приложения.

---

## 1. Краткое описание
Проект реализует минимальный кастомный механизм аутентификации/авторизации на основе JWT без использования `spring-boot-starter-security`. Для защиты конечных точек используется`:

```java


@JwtAuth //— проверка подлинности токена (Bearer).

@JwtAuthWithRoles(allowedRoles = {"ROLE_ADMIN"}) //— проверка наличия хотя бы одной роли.

```

---

## 2. Конфигурация (application.properties / env)

```properties
# JWT
jwt.secret=${JWT_SECRET:}                 # обязательно задать в prod
jwt.expiration-ms=${JWT_EXPIRATION_MS:3600000}
```

Рекомендация: JWT_SECRET хранить в env/секретном хранилище; ключ минимум 32 байта (256 бит) либо Base64.

---

## 3. Как теперь решена проблема self-invocation

Основной подход (реализованный в проекте):
- Рефакторинг: логика, требующая AOP-проверок (методы с `@JwtAuth` / `@JwtAuthWithRoles`), вынесена в отдельный бин `SecuredService`. Контроллеры и другие бины вызывают эти методы через Spring, поэтому вызовы проходят через прокси, и аспект срабатывает корректно.

Почему это надёжно:
- Spring AOP работает через прокси; вызовы через прокси — корректно применяют аспекты.
- Вынесение в отдельный бин — простое, прозрачное и легко поддерживаемое решение.

Альтернативы (если нужен другой подход):
- Программный вызов: внутри того же бина вызывать `authService.hasAnyRole(...)` напрямую — надёжно и явно.
- Self‑injection с `@Lazy` или `AopContext.currentProxy()` + `@EnableAspectJAutoProxy(exposeProxy = true)` — работает, но менее предпочтительно (сложнее в поддержке).
- AspectJ weaving (compile/load-time) — мощно, но требует дополнительной настройки.


---

## 4. Примеры использования аннотаций

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





## 5. Как тестировать (curl)

Регистрация:
curl -X POST -H "Content-Type: application/json" -d '{"name":"john","password":"pass","email":"john@example.com"}' http://localhost:8080/api/v1/auth/register

Логин:
curl -X POST -H "Content-Type: application/json" -d '{"name":"john","password":"pass"}' http://localhost:8080/api/v1/auth/login

Публичный:
curl http://localhost:8080/public

Защищённый:
curl -H "Authorization: Bearer <TOKEN>" http://localhost:8080/secure

Ролевой:
curl -H "Authorization: Bearer <TOKEN>" http://localhost:8080/admin

---


##  Контакты
- Разработчик: Роман
- Версия: 1.0.0
- Дата: 22 Ноябрь 2025  
  ✉ krp77@mail.ru

---

