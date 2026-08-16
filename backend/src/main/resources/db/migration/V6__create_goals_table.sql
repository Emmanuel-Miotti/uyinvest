CREATE TABLE goals (
    id              UUID PRIMARY KEY,
    user_id         UUID          NOT NULL,
    name            VARCHAR(150)  NOT NULL,
    target_amount   NUMERIC(19,4) NOT NULL,
    current_amount  NUMERIC(19,4) NOT NULL DEFAULT 0,
    currency        VARCHAR(3)    NOT NULL,
    target_date     DATE,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT fk_goals_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_goals_target_amount_positive CHECK (target_amount > 0),
    CONSTRAINT chk_goals_current_amount_non_negative CHECK (current_amount >= 0)
);

CREATE INDEX idx_goals_user_id ON goals (user_id);
