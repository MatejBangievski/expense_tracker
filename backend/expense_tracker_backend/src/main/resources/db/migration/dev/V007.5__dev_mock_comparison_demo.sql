-- Credentials: demo@demo.com / demo1234
INSERT INTO app_user (display_name, email, password_hash, monthly_salary, total_saved)
VALUES ('Demo User', 'demo@demo.com', '$2y$10$cn/u2o0AN/7uHnHs2GeOYO7qy5owX2oaa8SyHPAqqj8OKISkx5W6.', 3000.00, 0.00);

INSERT INTO expense (user_id, category_id, description, amount, expense_date)
SELECT u.id, c.id, x.description, x.amount, x.expense_date
FROM app_user u
CROSS JOIN (VALUES
    ('Groceries',               'Weekly grocery run',      300.00, DATE '2024-03-05'),
    ('Fuel',                    'Gas station fill-up',     120.00, DATE '2024-03-10'),
    ('Electricity',             'Electricity bill',         80.00, DATE '2024-09-15'),
    ('Dining Out',              'Dinner out',               60.00, DATE '2024-09-20'),

    ('Groceries',               'Weekly grocery run',      210.00, DATE '2025-01-08'),
    ('Streaming Subscriptions', 'Streaming bundle',         27.98, DATE '2025-01-02'),
    ('Groceries',               'Weekly grocery run',      240.00, DATE '2025-04-10'),
    ('Fuel',                    'Gas station fill-up',      90.00, DATE '2025-04-12'),
    ('Dining Out',              'Birthday dinner',          75.00, DATE '2025-04-18'),
    ('Groceries',               'Weekly grocery run',      260.00, DATE '2025-07-05'),
    ('Public Transit',          'Monthly bus pass',         40.00, DATE '2025-07-01'),
    ('Video Games',             'Season pass',              59.99, DATE '2025-07-20'),
    ('Groceries',               'Weekly grocery run',      280.00, DATE '2025-11-09'),
    ('Electricity',             'Electricity bill',         95.00, DATE '2025-11-15'),

    ('Groceries',               'Weekly grocery run',      220.00, DATE '2026-01-07'),
    ('Internet',                'Internet bill',            35.00, DATE '2026-01-03'),
    ('Groceries',               'Weekly grocery run',      205.00, DATE '2026-02-11'),
    ('Fuel',                    'Gas station fill-up',      88.00, DATE '2026-02-14'),
    ('Groceries',               'Weekly grocery run',      250.00, DATE '2026-03-09'),
    ('Dining Out',              'Lunch with friends',       65.00, DATE '2026-03-15'),
    ('Groceries',               'Weekly grocery run',      270.00, DATE '2026-05-06'),
    ('Streaming Subscriptions', 'Streaming bundle',         27.98, DATE '2026-05-01'),
    ('Groceries',               'Weekly grocery run',      300.00, DATE '2026-07-08'),
    ('Electricity',             'Electricity bill',         70.00, DATE '2026-07-10'),
    ('Video Games',             'New release',              49.99, DATE '2026-07-22'),

    ('Groceries',               'Weekly grocery run',       72.00, DATE '2026-08-04'),
    ('Fuel',                    'Gas station fill-up',      50.00, DATE '2026-08-05'),
    ('Groceries',               'Weekly grocery run',       68.00, DATE '2026-08-11'),
    ('Dining Out',              'Team lunch',               40.00, DATE '2026-08-13'),
    ('Groceries',               'Weekly grocery run',       75.00, DATE '2026-08-18'),
    ('Public Transit',          'Monthly bus pass',         40.00, DATE '2026-08-17'),
    ('Groceries',               'Weekly grocery run',       66.00, DATE '2026-08-24'),
    ('Streaming Subscriptions', 'Streaming bundle',         15.99, DATE '2026-08-24')
) AS x(category_name, description, amount, expense_date)
JOIN category c ON c.name = x.category_name
WHERE u.email = 'demo@demo.com';