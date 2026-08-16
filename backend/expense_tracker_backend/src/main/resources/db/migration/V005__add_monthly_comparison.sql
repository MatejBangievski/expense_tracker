CREATE TABLE monthly_comparison (
                                    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                    user_id              BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
                                    current_summary_id   BIGINT NOT NULL REFERENCES monthly_summary(id) ON DELETE CASCADE,
                                    previous_summary_id  BIGINT NOT NULL REFERENCES monthly_summary(id) ON DELETE CASCADE,
                                    comparison_message   TEXT NOT NULL,
                                    CONSTRAINT uq_monthly_comparison_summaries UNIQUE (current_summary_id, previous_summary_id),
                                    CONSTRAINT chk_monthly_comparison_distinct CHECK (current_summary_id <> previous_summary_id)
);

CREATE INDEX idx_monthly_comparison_user ON monthly_comparison (user_id);