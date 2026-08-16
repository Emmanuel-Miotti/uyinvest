CREATE TABLE portfolios (
    id             UUID PRIMARY KEY,
    name           VARCHAR(150) NOT NULL,
    description    TEXT,
    user_id        UUID         NOT NULL,
    base_currency  VARCHAR(3)   NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_portfolios_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_portfolios_user_id ON portfolios (user_id);
