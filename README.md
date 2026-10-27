# Uzgun User Service

Java 21 / Spring Boot 4 / PostgreSQL. Реализована одна ручка:
`GET /user/permittedToCheckout` (`isCustomerPermittedToCheckout`).

Она возвращает **явное право обхода ограничений checkout**, а не итоговую возможность купить товар.
По умолчанию право отсутствует (`false`). Это согласованная политика этого проекта; исходный
атлас не раскрывает внутреннюю реализацию Uzum.

- [Контракт и место в предложенном flow](docs/checkout-permission.md)
- [Обзор изменений и границы готовности](docs/refactor-review.md)
- [Сохранённые тестовые отчёты, покрытие и Docker-проверки](verification/2026-10-27/README.md)
- [Архитектура и добавление следующих ручек](docs/architecture.md)

## Проверка

Нужны JDK 21+, Maven 3.6.3+; для интеграционных тестов — работающий Docker.

```bash
mvn clean verify
mvn clean verify -Pintegration
```

Первая команда выполняет unit/MVC/HTTP-client тесты и проверяет форматирование.
Вторая дополнительно запускает полный сервис с отдельным PostgreSQL через Testcontainers,
проверяет миграцию V1 → V2 и реальные подписанные JWT. Рабочая база приложения не используется.
Форматирование: `mvn spotless:apply`. Отчёт покрытия: `target/site/jacoco/index.html`.

## Запуск через Compose

```bash
cp .env.example .env
```

В `.env` заполни настоящий `JWT_ISSUER_URI`, доступный из контейнера, и настрой `JWT_AUDIENCE`
под токены своего issuer. Для существующего PostgreSQL volume используй его прежние DB_USER/DB_PASSWORD:
изменение этих переменных не меняет пароль в уже созданной базе. `.env` не попадает в Git и Docker context.

```bash
docker compose config --quiet
docker compose up --build -d
docker compose logs -f app
```

Dockerfile требует BuildKit (использует cache mounts); для сборки через Docker CLI нужен buildx.
Compose-проверки не заменяют внешний ingress/TLS, secret store и настройки issuer.
Образ запускается непривилегированным пользователем; Compose включает read-only filesystem,
`/tmp` в tmpfs и `no-new-privileges`. Docker build запускает unit/MVC-тесты;
интеграционные тесты запускаются отдельно в CI с доступом к Docker.

```bash
curl -i http://localhost:8080/actuator/health/liveness
curl -i http://localhost:8080/actuator/health/readiness
curl -i http://localhost:8080/user/permittedToCheckout
curl -i -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://localhost:8080/user/permittedToCheckout
```

Без токена последняя ручка отвечает JSON `401`, без login page.
Требования к JWT: доверенный `iss`, подходящий `aud`, положительный числовой `sub = users.id`,
`roles: ["CUSTOMER"]`. Это локальный identity-контракт, не восстановленный факт из атласа.

Swagger отключён по умолчанию. Для локальной разработки `API_DOCS_ENABLED=true` открывает
`/swagger-ui.html` и `/v3/api-docs`. `/actuator/prometheus` требует `ROLE_OPS`.
В профиле `prod` логи имеют структурированный формат ECS, включая MDC `traceId`.

## Схема и выдача права

V1 сохранена без изменений. V2 добавляет `users.checkout_bypass_permitted NOT NULL DEFAULT FALSE`.
Все существующие пользователи получают `false`; новая таблица или копирование пользователей не нужны.
Hibernate только проверяет схему (`validate`), Flyway управляет изменениями.

Публичной ручки выдачи права нет. Пока grant/revoke выполняет только доверенный оператор БД
по согласованной процедуре с внешним аудитом. Для тестового пользователя в изолированной базе:

```sql
UPDATE users
SET checkout_bypass_permitted = TRUE,
    version = version + 1,
    updated_at = now()
WHERE id = 123;
```

Для отзыва — `FALSE`. Проверка права не кэшируется. Не используй массовый `UPDATE` без `WHERE`.
После применения V2 не редактируй её: следующие изменения схемы — новая миграция.

## Отдельные обычные проверки

`checkout/eligibility` сохраняет исходные проверки роли, активности, телефона, имени и риска.
Они **не вызываются** из `/user/permittedToCheckout` и по умолчанию не подключены к Spring-контексту.
Для будущего use case создания заказа их можно явно включить:

```dotenv
CHECKOUT_ELIGIBILITY_ENABLED=true
RISK_BASE_URL=http://risk-service:8080
```

При включении URL и положительные таймауты обязательны. Контракт `/risk/users/{id}` — проектная
заготовка, а не API, подтверждённый атласом. До использования при создании заказа требуется
согласовать бизнес-политику и реальные зависимости. Приложение не предоставляет заглушку,
которая автоматически разрешает риск-проверку.
