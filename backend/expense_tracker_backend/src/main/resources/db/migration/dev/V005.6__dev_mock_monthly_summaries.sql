INSERT INTO monthly_summary (user_id, summary_month, total_spent, total_income)
SELECT e.user_id,
       date_trunc('month', e.expense_date)::date,
       SUM(e.amount),
       u.monthly_salary
FROM expense e
JOIN app_user u ON u.id = e.user_id
GROUP BY e.user_id, date_trunc('month', e.expense_date), u.monthly_salary;

INSERT INTO monthly_summary_category (monthly_summary_id, category_id, total_amount, expense_count)
SELECT s.id,
       e.category_id,
       SUM(e.amount),
       COUNT(*)
FROM expense e
JOIN monthly_summary s
  ON s.user_id = e.user_id
 AND s.summary_month = date_trunc('month', e.expense_date)::date
GROUP BY s.id, e.category_id;