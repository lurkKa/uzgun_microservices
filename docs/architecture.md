# Структура и следующие endpoint

Это один deployable Spring Boot сервис с пакетами по ответственности. Имя Maven artifact и корневой
Java package сохранены для совместимости текущего проекта. Наличие пакетов не означает отдельные микросервисы.

```text
src/main/java/com/uz/uzgun_microservices/
  App.java
  user/
    api/                 # HTTP controller, API DTO, OpenAPI annotations
    application/         # use cases: CheckoutPermissionService
    domain/              # User и его enum; сейчас JPA-модель без лишнего дублирования
    persistence/         # Spring Data repository, запрос права
  checkout/
    eligibility/         # отдельные обычные правила + явный порядок
    integration/risk/    # порт RiskClient, HTTP adapter и его настройки
  security/              # JWT, AuthContext, JSON 401/403
  shared/api/            # envelope, ошибки в JSON, correlation filter
  shared/error/          # exception mapping и стабильные коды
  config/                # OpenAPI
src/main/resources/
  application.yaml
  application-prod.yaml
  db/migration/          # неизменяемая V1, добавленная V2
src/test/java/            # повторяет структуру main + полный CheckoutPermissionIT
```

## Направление зависимостей

`user.api → user.application → user.persistence → PostgreSQL`.
Endpoint не зависит от `checkout.eligibility` и не обращается в риск-сервис.
Shared-пакеты содержат только общую HTTP-инфраструктуру. Не добавляй в них правила заказов/платежей.
В текущем одном сервисе eligibility использует `User`; при реальном выделении checkout потребуется
собственный snapshot/контракт клиента вместо импорта JPA entity из другого сервиса.

## Как добавить ручку

1. Найти точный path/operationId, схемы и предлагаемый flow в атласе. Записать факты отдельно от допущений.
2. Для user-ручки добавить use case в `user/application`, API DTO и controller в `user/api`.
   Для нового домена завести соседний пакет (`order`, `payment`), не расширять универсальный UserService.
3. Новые колонки/таблицы — очередная Flyway-миграция, не редактирование уже применённой V1/V2.
4. Идентичность брать из проверенного JWT. DTO клиента не должен выбирать чужой userId без отдельной авторизации.
5. Добавить контрактные HTTP-тесты и интеграционный тест с PostgreSQL для новых запросов/миграций.
6. Если есть HTTP-зависимость, отделить adapter, ограничить ожидание, проверить неопределённые ответы.
7. При создании заказа отдельно решить атомарность, идемпотентность, цены, остатки, оплату и повторную проверку прав.
   Эти механизмы не нужны в read-only GET и сейчас не объявлены реализованными.

## Когда выделять отдельный микросервис

После появления самостоятельного use case, владельца данных и необходимости независимого деплоя.
Тогда создать отдельный Maven module/Boot application со своей схемой и миграциями. Текущий корень
не превращён преждевременно в набор пустых сервисов. Не давай соседнему сервису прямую запись в users.

## Эксплуатационные границы

- liveness не зависит от БД; readiness включает БД.
- Prometheus требует JWT с OPS; нужно настроить аутентифицированный scrape либо отдельный защищённый management ingress.
- Swagger включается явно, по умолчанию отключён.
- Нет production issuer, managed secrets, TLS ingress и внешнего audit sink в этом репозитории.
- Управление grant/revoke, срок действия права и аудит оператора пока не имеют API. До массового использования
  нужна согласованная операционная процедура либо отдельный административный use case.
- Нагрузочное тестирование, SLO и подтверждение реальной политики Uzum не выполнялись.
