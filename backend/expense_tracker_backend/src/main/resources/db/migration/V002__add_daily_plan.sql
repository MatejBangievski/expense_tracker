CREATE TABLE daily_plan (
                            id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                            plan_id           BIGINT NOT NULL REFERENCES plan(id) ON DELETE CASCADE,
                            date              DATE NOT NULL,
                            allocated_amount  NUMERIC(12, 2) NOT NULL,
                            CONSTRAINT uq_daily_plan_date UNIQUE (plan_id, date)
);

CREATE INDEX idx_daily_plan_plan ON daily_plan (plan_id);
CREATE TABLE refresh_token (
                               id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                               user_id     BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
                               token       VARCHAR(500) NOT NULL UNIQUE,
                               expires_at  TIMESTAMPTZ NOT NULL,
                               revoked     BOOLEAN NOT NULL DEFAULT FALSE,
                               created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_token_user ON refresh_token (user_id);
CREATE INDEX idx_refresh_token_token ON refresh_token (token);