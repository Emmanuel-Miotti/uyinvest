CREATE TABLE dividends (
    id            UUID PRIMARY KEY,
    portfolio_id  UUID          NOT NULL,
    asset_id      UUID          NOT NULL,
    amount        NUMERIC(19,4) NOT NULL,
    currency      VARCHAR(3)    NOT NULL,
    payment_date  DATE          NOT NULL,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT fk_dividends_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolios (id) ON DELETE CASCADE,
    CONSTRAINT fk_dividends_asset FOREIGN KEY (asset_id) REFERENCES assets (id),
    CONSTRAINT chk_dividends_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_dividends_portfolio_id ON dividends (portfolio_id);
CREATE INDEX idx_dividends_asset_id ON dividends (asset_id);
CREATE INDEX idx_dividends_payment_date ON dividends (payment_date);
