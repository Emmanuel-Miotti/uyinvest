-- Net-quantity lookups and per-asset transaction queries always filter on both columns together.
CREATE INDEX idx_transactions_portfolio_asset ON transactions (portfolio_id, asset_id);
