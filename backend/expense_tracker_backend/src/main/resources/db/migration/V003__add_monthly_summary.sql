CREATE TABLE monthly_summary (
                                 id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                 user_id        BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
                                 summary_month  DATE NOT NULL,
                                 total_spent    NUMERIC(12, 2) NOT NULL DEFAULT 0,
                                 total_income   NUMERIC(12, 2),
                                 CONSTRAINT uq_monthly_summary_user_month UNIQUE (user_id, summary_month),
                                 CONSTRAINT chk_monthly_summary_first_day CHECK (EXTRACT(DAY FROM summary_month) = 1)
);

CREATE TABLE monthly_summary_category (
                                 id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                 monthly_summary_id  BIGINT NOT NULL REFERENCES monthly_summary(id) ON DELETE CASCADE,
                                 category_id         BIGINT NOT NULL REFERENCES category(id) ON DELETE RESTRICT,
                                 total_amount        NUMERIC(12, 2) NOT NULL,
                                 expense_count       INTEGER NOT NULL,
                                 CONSTRAINT uq_monthly_summary_category UNIQUE (monthly_summary_id, category_id),
                                 CONSTRAINT chk_monthly_summary_category_count CHECK (expense_count >= 0)
);