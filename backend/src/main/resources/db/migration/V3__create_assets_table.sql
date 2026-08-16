CREATE TABLE assets (
    id          UUID PRIMARY KEY,
    symbol      VARCHAR(20)  NOT NULL,
    name        VARCHAR(150) NOT NULL,
    type        VARCHAR(20)  NOT NULL,
    currency    VARCHAR(3)   NOT NULL,
    sector      VARCHAR(100),
    active      BOOLEAN      NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_assets_symbol UNIQUE (symbol),
    CONSTRAINT chk_assets_type CHECK (type IN ('STOCK', 'ETF', 'BOND', 'FUND', 'CRYPTO', 'CASH'))
);

CREATE INDEX idx_assets_type ON assets (type);
