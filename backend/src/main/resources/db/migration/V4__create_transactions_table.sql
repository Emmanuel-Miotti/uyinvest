CREATE TABLE transactions (
    id                UUID PRIMARY KEY,
    portfolio_id      UUID           NOT NULL,
    asset_id          UUID           NOT NULL,
    type              VARCHAR(10)    NOT NULL,
    quantity          NUMERIC(20,8)  NOT NULL,
    price             NUMERIC(20,8)  NOT NULL,
    commission        NUMERIC(19,4)  NOT NULL DEFAULT 0,
    currency          VARCHAR(3)     NOT NULL,
    transaction_date  TIMESTAMPTZ    NOT NULL,
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT fk_transactions_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolios (id) ON DELETE CASCADE,
    CONSTRAINT fk_transactions_asset FOREIGN KEY (asset_id) REFERENCES assets (id),
    CONSTRAINT chk_transactions_type CHECK (type IN ('BUY', 'SELL')),
    CONSTRAINT chk_transactions_quantity_positive CHECK (quantity > 0),
    CONSTRAINT chk_transactions_price_positive CHECK (price > 0),
    CONSTRAINT chk_transactions_commission_non_negative CHECK (commission >= 0)
);

CREATE INDEX idx_transactions_portfolio_id ON transactions (portfolio_id);
CREATE INDEX idx_transactions_asset_id ON transactions (asset_id);
CREATE INDEX idx_transactions_date ON transactions (transaction_date);
