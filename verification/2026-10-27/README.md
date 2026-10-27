# Проверки от 27 октября 2026

**42 теста прошли, 0 failures, 0 errors, 0 skipped.** Maven BUILD SUCCESS.
Дата и часовой пояс отчёта: Europe/Moscow. Полная сборка на Java 21.

| Набор | Тестов | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| CheckoutPermissionIT | 4 | 0 | 0 | 0 |
| CheckoutEligibilityConfigurationTest | 3 | 0 | 0 | 0 |
| CheckoutPolicyTest | 7 | 0 | 0 | 0 |
| HttpRiskClientTest | 12 | 0 | 0 | 0 |
| CustomerControllerTest | 13 | 0 | 0 | 0 |
| CheckoutPermissionServiceTest | 3 | 0 | 0 | 0 |

## Открыть результаты

- [HTML-покрытие JaCoCo](coverage/index.html) — открой в браузере; содержит навигацию по классам и исходникам.
- [Unit/MVC JUnit XML и текстовые отчёты](surefire-reports/)
- [Интеграционные JUnit XML и текстовые отчёты](failsafe-reports/)
- [Полный лог Maven](maven-verify.log)
- [Лог сборки Docker](docker-build.log)
- [Результат запуска Docker-образа](docker-smoke.log)
- [Машинная сводка](summary.json)
- [SHA-256 проверенных исходников и конфигурации](source-sha256.json)
- [Исходники всех Java-тестов](../../src/test/java/com/uz/uzgun_microservices/)
- [Повторяемый smoke-скрипт](../../scripts/smoke-image.py)

ERROR в Maven-логе может быть ожидаемым результатом теста недоступности БД;
итог смотрите в JUnit reports и BUILD SUCCESS. Проверки ошибок намеренно вызывают обработчики исключений.
В XML могут присутствовать локальные пути среды выполнения и тестовые значения; реальный issuer и токены не использовались.

## Повторить

Из корня `uzgun_microservices`:

```bash
mvn clean verify -Pintegration
```

Для Docker smoke сначала собери образ (нужен BuildKit), затем запусти скрипт:

```bash
docker build -t uzgun-review-app .
SMOKE_IMAGE=uzgun-review-app python3 scripts/smoke-image.py
```

Скрипт поднимает отдельный PostgreSQL, сеть и контейнер приложения; удаляет их в finally.
Он не использует Compose volume и рабочие данные. Issuer в smoke — тестовый адрес;
проверка настоящих подписей JWT, issuer, audience и expiry находится в CheckoutPermissionIT.
Smoke проверяет запуск production-профиля, health, JSON 401, non-root, read-only filesystem и structured logging.

Эта папка — сохранённый снимок результатов, а не динамический отчёт. `mvn clean` её не удаляет.
Новые результаты Maven создаются в `target`; при следующих изменениях сделай новый снимок.
