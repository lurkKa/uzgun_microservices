# Контракт и предложенный checkout flow

## Что подтверждено источниками

- [OpenAPI](../../uzgun/architecture/openapi.json): `GET /user/permittedToCheckout`, operationId
  `isCustomerPermittedToCheckout`, ответ `GenericResponsePermissionToCheckoutDto`, поле `permitted`.
  В исходной схеме `permitted` не помечен required. Наш сервер всегда отдаёт явный boolean.
- [Behavior](../../uzgun/architecture/behavior.md): раздел «permitted не является полной политикой покупки».
  Summary говорит об обходе ограничений; `false` и отсутствие payload нельзя трактовать одинаково.
- [Flow](../../uzgun/architecture/flows.md): предварительная проверка, затем изучение цены,
  вариантов оплаты и создания заказа. Атлас прямо указывает, что обязательность и порядок связей не доказаны.
- [Purchase journey](../../uzgun/architecture/journey-purchase-sequence-all.mmd): вызов условный (`opt`).

Источник не устанавливает хранилище права, JWT claims, роль CUSTOMER, риск-сервис, причины выдачи права,
правила наличия телефона/имени или разрешённые к обходу ограничения.

## Принятое решение проекта

Пользователь согласовал явное право обхода с default false. Источник права — boolean-колонка пользователя.
JWT определяет идентичность и доступ к endpoint, а не само право обхода. Поэтому изменение права в БД
видно при следующем запросе без перевыпуска JWT. Endpoint ничего не выдаёт и не изменяет.

```mermaid
sequenceDiagram
    participant C as Клиент
    participant U as User API (реализован)
    participant D as PostgreSQL
    participant O as Создание заказа (ещё не реализовано)
    opt Клиент проверяет право обхода перед оформлением
        C->>U: GET /user/permittedToCheckout + JWT
        U->>U: Проверить JWT и CUSTOMER, получить sub
        U->>D: SELECT checkout_bypass_permitted WHERE id = sub
        D-->>U: true / false / пользователь отсутствует
        U-->>C: 200 payload.permitted / 400 / 503
    end
    alt permitted = true
        Note over C: Выбрать согласованную ветвь обхода
    else permitted = false
        Note over C: Обычные проверки оформления; не общий запрет покупки
    else Нет достоверного ответа
        Note over C: Не предоставлять обход; показать временную ошибку/повтор
    end
    Note over C,O: Цена и варианты оплаты — соседние этапы предложенного flow, пока не реализованы
    C->>O: POST /user/v2/order (будущий контракт)
    Note over O: Заново проверить право и обязательные инварианты заказа на сервере
```

Диаграмма описывает проектное решение с учётом предложенного flow, не доказанную последовательность
production Uzum. Успешный GET не резервирует товар, не фиксирует цену, не создаёт заказ и не обещает
успешный checkout. Клиентский boolean нельзя доверенно передавать в будущий order endpoint как доказательство.

## HTTP

| Ситуация | HTTP | Тело / смысл |
|---|---:|---|
| Есть явный grant | 200 | `payload.permitted: true` |
| Grant отсутствует | 200 | `payload.permitted: false`; обычная ветвь checkout |
| Пользователь отсутствует | 400 | `CUSTOMER_NOT_FOUND`; сохранён статус исходной реализации |
| Нет/невалидный JWT, некорректный sub | 401 | `UNAUTHORIZED` |
| Нет CUSTOMER | 403 | `FORBIDDEN` |
| БД/транзакция недоступна | 503 | `SERVICE_UNAVAILABLE`, без payload |
| Неожиданная ошибка | 500 | `INTERNAL_ERROR`, без внутренних подробностей |

400/503 присутствуют в исходном OpenAPI; точное значение 400 и security-ответы — решения нашей реализации.
Ответы сохраняют envelope `payload/errors/timestamp/trace/error`. `error` — legacy-текст первой ошибки.
Timestamp — UTC Instant, trace совпадает с `X-Trace-Id`; успех содержит `Cache-Control: no-store`.

```json
{
  "payload": {"permitted": false},
  "timestamp": "2026-10-27T13:00:00Z",
  "trace": "a09683ca-9c5f-40da-bdc1-9335191e87a1"
}
```

## Обычная eligibility и риск

Сохранена отдельно как предложенная политика текущего проекта, не как установленное поведение API:
CUSTOMER → active → phone → firstName → risk. Первый отказ прекращает проверки. Пустой набор правил запрещён.
Значение bypass grant не влияет на эту цепочку; выбор ветви и неотменяемые инварианты определит будущий order use case.

Risk adapter принимает только JSON boolean `blocked`. Пустой ответ, отсутствующее/неверно типизированное поле,
испорченный JSON, HTTP-ошибка, таймаут и открытый circuit breaker приводят к unavailable, а не разрешению.
Circuit breaker применяется программно, поэтому не зависит от наличия AOP-прокси. Есть ограниченные
connect/read timeout и метрики breaker. Автоматические повторы не добавлены.
