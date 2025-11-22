# Индивидуальный  механизм аутентификации/авторизации на базе JWT без spring-boot-starter-security. Поддерживаются проверки подписи, срока жизни и ролей.

---

##  Краткое описание

- Проверка подписи HS256 
- Аннотации для защиты контроллеров:
- @JwtAuth — требует валидный access-token
- @JwtAuthWithRoles(allowedRoles = {...}) — проверяет роли

##  Примеры использования аннотаций

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
### 🔁 Этот класс клиент  тестирования.  Он отправляет HTTP-запросы к нашему  Spring Boot-приложению и обрабатывает полученные ответы.

```java
package com.example.applicationRoma.clientOk;

import okhttp3.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class AuthTestClient {

  private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

  private static final OkHttpClient client = new OkHttpClient.Builder()
          .connectTimeout(10, TimeUnit.SECONDS)
          .readTimeout(30, TimeUnit.SECONDS)
          .callTimeout(1, TimeUnit.MINUTES)
          .build();

  private static final ObjectMapper mapper = new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  private static final String BASE_URL = "http://localhost:8080/api/v1";

  private static final String TEST_NAME = "user";
  private static final String TEST_PASSWORD = "password";
  private static final String TEST_EMAIL = "ser@example.com";

  public static void main(String[] args) {
    try {
      System.out.println("=== Регистрация ===");
      registerUser();

      System.out.println("\n=== Логин ===");
      AuthResponse auth = loginUser();
      if (auth == null || auth.accessToken() == null || auth.accessToken().isBlank()) {
        System.err.println("Не удалось получить access token — прерывание тестов");
        return;
      }

      System.out.println("\n=== Тест эндпоинтов ===");
      testEndpoints(auth.accessToken());

    } catch (Exception e) {
      System.err.println("Ошибка в клиенте: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private static void registerUser() {
    RegisterRequest req = new RegisterRequest(TEST_NAME, TEST_PASSWORD, TEST_EMAIL);
    try {
      String json = mapper.writeValueAsString(req);
      RequestBody body = RequestBody.create(json, JSON);
      Request request = new Request.Builder()
              .url(BASE_URL + "/auth/register")
              .post(body)
              .build();

      String resp = executeRequestAllowingUnsuccessful(request);
      System.out.println("Регистрация ответ: " + resp);
    } catch (IOException e) {
      System.err.println("Ошибка при регистрации: " + e.getMessage());
    }
  }

  private static AuthResponse loginUser() {
    LoginRequest req = new LoginRequest(TEST_NAME, TEST_PASSWORD);
    try {
      String json = mapper.writeValueAsString(req);
      RequestBody body = RequestBody.create(json, JSON);
      Request request = new Request.Builder()
              .url(BASE_URL + "/auth/login")
              .post(body)
              .build();

      String resp = executeRequestAllowingUnsuccessful(request);
      System.out.println("Логин ответ: " + resp);

      try {
        return mapper.readValue(resp, AuthResponse.class);
      } catch (Exception ex) {
        String token = resp.trim();
        if ((token.startsWith("\"") && token.endsWith("\"")) || (token.startsWith("'") && token.endsWith("'"))) {
          token = token.substring(1, token.length() - 1);
        }
        return new AuthResponse(token, null);
      }

    } catch (IOException e) {
      System.err.println("Ошибка при логине: " + e.getMessage());
      return null;
    }
  }

  private static void testEndpoints(String accessToken) {
    try {
      System.out.println("\n-> GET /test/public (публичный)");
      Request reqPublic = new Request.Builder()
              .url(BASE_URL + "/test/public")
              .get()
              .build();
      System.out.println(executeRequestAllowingUnsuccessful(reqPublic));
    } catch (IOException e) {
      System.err.println("Ошибка при вызове public: " + e.getMessage());
    }

    try {
      System.out.println("\n-> GET /test/secure (требуется JWT)");
      Request reqSecure = new Request.Builder()
              .url(BASE_URL + "/test/secure")
              .get()
              .header("Authorization", "Bearer " + accessToken)
              .build();
      System.out.println(executeRequestAllowingUnsuccessful(reqSecure));
    } catch (IOException e) {
      System.err.println("Ошибка при вызове secure: " + e.getMessage());
    }
    try {
      System.out.println("\n-> GET /test/admin (ROLE_ADMIN)");
      Request reqAdmin = new Request.Builder()
              .url(BASE_URL + "/test/admin")
              .get()
              .header("Authorization", "Bearer " + accessToken)
              .build();
      System.out.println(executeRequestAllowingUnsuccessful(reqAdmin));
    } catch (IOException e) {
      System.err.println("Ошибка при вызове admin: " + e.getMessage());
    }

    try {
      System.out.println("\n-> GET /test/authenticated (ROLE_USER|ROLE_ADMIN)");
      Request reqAuth = new Request.Builder()
              .url(BASE_URL + "/test/authenticated")
              .get()
              .header("Authorization", "Bearer " + accessToken)
              .build();
      System.out.println(executeRequestAllowingUnsuccessful(reqAuth));
    } catch (IOException e) {
      System.err.println("Ошибка при вызове authenticated: " + e.getMessage());
    }
  }

  private static String executeRequestAllowingUnsuccessful(Request request) throws IOException {
    try (Response response = client.newCall(request).execute()) {
      ResponseBody rb = response.body();
      String body = rb != null ? rb.string() : "";
      System.out.println("HTTP " + response.code() + " " + response.message());
      return body;
    }
  }
  public record LoginRequest(String name, String password) {}
  public record RegisterRequest(String name, String password, String email) {}
  public record AuthResponse(String accessToken, String refreshToken) {}
}

```
---

##  Контакты
- Разработчик: Роман
- Версия: 1.0.0
- Дата: 22 Ноябрь 2025  
  ✉ krp77@mail.ru

---

