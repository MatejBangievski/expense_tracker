CREATE TABLE app_user (
                          id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                          display_name    VARCHAR(120) NOT NULL,
                          email           VARCHAR(190) NOT NULL UNIQUE,
                          password_hash   VARCHAR(255) NOT NULL,
                          monthly_salary  NUMERIC(12, 2) NOT NULL DEFAULT 0,
                          total_saved     NUMERIC(14, 2) NOT NULL DEFAULT 0
);

CREATE TABLE category (
                          id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                          name                VARCHAR(80) NOT NULL,
                          parent_category_id  BIGINT REFERENCES category(id) ON DELETE CASCADE
);

CREATE TABLE plan (
                      id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                      user_id         BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
                      name            VARCHAR(160) NOT NULL,
                      start_date      DATE NOT NULL,
                      end_date        DATE NOT NULL,
                      total_budget    NUMERIC(12, 2) NOT NULL,
                      CONSTRAINT chk_plan_dates CHECK (end_date >= start_date)
);

CREATE TABLE plan_item (
                           id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                           plan_id         BIGINT NOT NULL REFERENCES plan(id) ON DELETE CASCADE,
                           category_id     BIGINT REFERENCES category(id) ON DELETE SET NULL,
                           description     VARCHAR(300) NOT NULL,
                           planned_date    DATE NOT NULL,
                           planned_amount  NUMERIC(12, 2) NOT NULL
);

CREATE INDEX idx_plan_item_plan ON plan_item (plan_id);

CREATE TABLE expense (
                         id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                         user_id         BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
                         category_id     BIGINT NOT NULL REFERENCES category(id) ON DELETE RESTRICT,
                         plan_id         BIGINT REFERENCES plan(id) ON DELETE SET NULL,
                         description     VARCHAR(500),
                         amount          NUMERIC(12, 2) NOT NULL,
                         expense_date    DATE NOT NULL
);

CREATE INDEX idx_expense_user_category_date ON expense (user_id, category_id, expense_date);
CREATE INDEX idx_expense_user_date ON expense (user_id, expense_date);
CREATE INDEX idx_expense_plan ON expense (plan_id);

CREATE TABLE budget (
                        id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                        user_id         BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
                        category_id     BIGINT NOT NULL REFERENCES category(id) ON DELETE RESTRICT,
                        budget_month    DATE NOT NULL,
                        monthly_limit   NUMERIC(12, 2) NOT NULL,
                        CONSTRAINT uq_budget_user_month_category UNIQUE (user_id, budget_month, category_id)
);

CREATE TABLE monthly_saving (
                                id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                user_id         BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
                                saving_month    DATE NOT NULL,
                                total_income    NUMERIC(12, 2) NOT NULL,
                                total_spent     NUMERIC(12, 2) NOT NULL,
                                total_saved     NUMERIC(12, 2) NOT NULL,
                                CONSTRAINT uq_saving_user_month UNIQUE (user_id, saving_month)
);

CREATE INDEX idx_saving_user_month ON monthly_saving (user_id, saving_month);

CREATE TABLE recommendation_message (
                                        id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                        user_id         BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
                                        message         TEXT NOT NULL,
                                        model_name      VARCHAR(80)
);

CREATE INDEX idx_recommendation_user ON recommendation_message (user_id);