# Custom JWT security без spring-boot-starter-security

- механизм аутентификации/авторизации надёжен и корректно работает как для внешних вызовов, так и при вызове внутри приложения.

---

## 1. Краткое описание
Проект реализует минимальный кастомный механизм аутентификации/авторизации на основе JWT без использования `spring-boot-starter-security`. Для защиты конечных точек используется AOP (аспекты), которые проверяют JWT и роли через методы `AuthService`:
- `@JwtAuth` — проверка подлинности токена (Bearer).
- `@JwtAuthWithRoles(allowedRoles = {...})` — проверка наличия хотя бы одной роли.

Ключевые изменения: устранёна проблема self-invocation — все аннотированные методы теперь вызываются через прокси (рефакторинг в отдельный сервис/бин), либо внутренняя логика использует программные вызовы `AuthService.hasAnyRole(...)`. Также проверка ролей выполняется на уровне БД (exists‑запрос), чтобы избежать LazyInitializationException.

---

## 2. Фичи
- JWT: создание и валидация (`JwtUtils`).
- AOP-аспект (`JwtAuthAspect`) централизует проверку токена и ролей.
- Проверка ролей через эффективный репозиторный exists‑запрос (без инициализации ленивых коллекций).
- Решение проблемы self-invocation: защищённая логика вынесена в отдельный проксируемый бин (`SecuredService`), при необходимости доступны безопасные альтернативы.
- Минимальные зависимости — подходит без Spring Security.
- Поддержка BCrypt для хеширования паролей.
- Примеры публичных, защищённых и ролевых эндпойнтов.

---

## 3. Конфигурация (application.properties / env)

```properties
# JWT
jwt.secret=${JWT_SECRET:}                 # обязательно задать в prod
jwt.expiration-ms=${JWT_EXPIRATION_MS:3600000}
```

Рекомендация: JWT_SECRET хранить в env/секретном хранилище; ключ минимум 32 байта (256 бит) либо Base64.

---

## 4. Как теперь решена проблема self-invocation

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

## 5. Примеры использования аннотаций

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

Пример SecuredService:

```java
@Service
public class SecuredService {

    @JwtAuth
    public String secureMethod() {
        return "This is secured by JWT only";
    }

    @JwtAuthWithRoles(allowedRoles = {"ROLE_ADMIN"})
    public String adminMethod() {
        return "This is admin only";
    }
}
```

Такой вызов идёт через прокси и аспект применяется корректно — self‑invocation больше не проблема.

---



## 6. Как тестировать (curl)

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

