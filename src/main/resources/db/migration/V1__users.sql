CREATE TABLE users (
    id                      BIGSERIAL PRIMARY KEY,
    account_id              BIGINT,
    role                    VARCHAR(20)  NOT NULL,
    active                  BOOLEAN      NOT NULL DEFAULT TRUE,
    phone                   VARCHAR(13),
    email                   VARCHAR(254),
    first_name              VARCHAR(100),
    last_name               VARCHAR(100),
    patronymic              VARCHAR(100),
    sex                     VARCHAR(10),
    birth_date              DATE,
    preferred_delivery      VARCHAR(30),
    created_from_tezkor_app BOOLEAN      NOT NULL DEFAULT FALSE,
    version                 BIGINT       NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_users_phone CHECK (phone IS NULL OR phone ~ '^\+998[0-9]{9}$')
);
CREATE UNIQUE INDEX ux_users_phone ON users (phone) WHERE phone IS NOT NULL;
CREATE INDEX ix_users_account_id ON users (account_id);