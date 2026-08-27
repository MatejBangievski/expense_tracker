INSERT INTO monthly_saving (user_id, saving_month, total_budget_limit, total_income, total_spent, total_saved, recommendation_message)
SELECT id, DATE '2026-06-01', 700.00, 3000.00, 500.00, 200.00, NULL
FROM app_user WHERE email = 'demo@demo.com';

INSERT INTO monthly_saving (user_id, saving_month, total_budget_limit, total_income, total_spent, total_saved, recommendation_message)
SELECT id, DATE '2026-07-01', 600.00, 3000.00, 450.00, 150.00, NULL
FROM app_user WHERE email = 'demo@demo.com';