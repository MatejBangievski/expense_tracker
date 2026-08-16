ALTER TABLE monthly_summary RENAME TO period_summary;
ALTER TABLE period_summary RENAME COLUMN summary_month TO period_start;

ALTER TABLE period_summary ADD COLUMN period_type VARCHAR(10) NOT NULL DEFAULT 'MONTH';
ALTER TABLE period_summary ALTER COLUMN period_type DROP DEFAULT;

ALTER TABLE period_summary DROP CONSTRAINT uq_monthly_summary_user_month;
ALTER TABLE period_summary ADD CONSTRAINT uq_period_summary_user_type_start
    UNIQUE (user_id, period_type, period_start);

ALTER TABLE period_summary DROP CONSTRAINT chk_monthly_summary_first_day;
ALTER TABLE period_summary ADD CONSTRAINT chk_period_summary_aligned CHECK (
    CASE period_type
        WHEN 'WEEK'  THEN EXTRACT(ISODOW FROM period_start) = 1
        WHEN 'MONTH' THEN EXTRACT(DAY FROM period_start) = 1
        WHEN 'YEAR'  THEN EXTRACT(DAY FROM period_start) = 1 AND EXTRACT(MONTH FROM period_start) = 1
    END
);

ALTER TABLE monthly_summary_category RENAME TO period_summary_category;
ALTER TABLE period_summary_category RENAME COLUMN monthly_summary_id TO period_summary_id;

ALTER TABLE monthly_comparison RENAME TO period_comparison;