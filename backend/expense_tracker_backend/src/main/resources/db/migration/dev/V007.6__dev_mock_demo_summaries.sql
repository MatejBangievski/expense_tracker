INSERT INTO period_summary (user_id, period_type, period_start, total_spent, total_income)
SELECT e.user_id, 'MONTH', date_trunc('month', e.expense_date)::date, SUM(e.amount), u.monthly_salary
FROM expense e
JOIN app_user u ON u.id = e.user_id
WHERE u.email = 'demo@demo.com'
GROUP BY e.user_id, date_trunc('month', e.expense_date), u.monthly_salary;

INSERT INTO period_summary (user_id, period_type, period_start, total_spent, total_income)
SELECT e.user_id, 'YEAR', date_trunc('year', e.expense_date)::date, SUM(e.amount), u.monthly_salary
FROM expense e
JOIN app_user u ON u.id = e.user_id
WHERE u.email = 'demo@demo.com'
GROUP BY e.user_id, date_trunc('year', e.expense_date), u.monthly_salary;

INSERT INTO period_summary_category (period_summary_id, category_id, total_amount, expense_count)
SELECT s.id, e.category_id, SUM(e.amount), COUNT(*)
FROM expense e
JOIN app_user u ON u.id = e.user_id
JOIN period_summary s ON s.user_id = e.user_id
 AND s.period_type = 'MONTH'
 AND s.period_start = date_trunc('month', e.expense_date)::date
WHERE u.email = 'demo@demo.com'
GROUP BY s.id, e.category_id;

INSERT INTO period_summary_category (period_summary_id, category_id, total_amount, expense_count)
SELECT s.id, e.category_id, SUM(e.amount), COUNT(*)
FROM expense e
JOIN app_user u ON u.id = e.user_id
JOIN period_summary s ON s.user_id = e.user_id
 AND s.period_type = 'YEAR'
 AND s.period_start = date_trunc('year', e.expense_date)::date
WHERE u.email = 'demo@demo.com'
GROUP BY s.id, e.category_id;