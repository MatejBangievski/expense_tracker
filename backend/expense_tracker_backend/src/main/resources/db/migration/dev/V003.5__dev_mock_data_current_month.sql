INSERT INTO expense (user_id, category_id, description, amount, expense_date)
SELECT u.id, c.id, x.description, x.amount, x.expense_date
FROM app_user u
CROSS JOIN (VALUES
    ('Groceries',               'Weekly grocery run',       71.20, DATE '2026-08-02'),
    ('Groceries',               'Weekly grocery run',       64.85, DATE '2026-08-06'),
    ('Dining Out',              'Team lunch',               32.00, DATE '2026-08-03'),
    ('Fuel',                    'Gas station fill-up',      50.00, DATE '2026-08-04'),
    ('Streaming Subscriptions', 'Netflix',                  15.99, DATE '2026-08-01'),
    ('Streaming Subscriptions', 'Disney+',                  11.99, DATE '2026-08-01'),
    ('Electricity',             'August electricity bill',  68.50, DATE '2026-08-02'),
    ('Internet',                'August internet bill',     35.00, DATE '2026-08-02')
) AS x(category_name, description, amount, expense_date)
JOIN category c ON c.name = x.category_name
WHERE u.email = 'dev.tester@example.com';


INSERT INTO app_user (display_name, email, password_hash, monthly_salary, total_saved)
VALUES ('Ana Petrova', 'ana.petrova@example.com', '$2a$10$mockMockMockMockMockMockMockMockMockMockMockMockMoc', 2800.00, 1250.00);

INSERT INTO expense (user_id, category_id, description, amount, expense_date)
SELECT u.id, c.id, x.description, x.amount, x.expense_date
FROM app_user u
CROSS JOIN (VALUES
    ('Groceries',      'Big weekly shop',        95.30, DATE '2026-07-03'),
    ('Groceries',      'Farmers market',         88.40, DATE '2026-07-17'),
    ('Dining Out',     'Birthday dinner',        54.20, DATE '2026-07-08'),
    ('Public Transit', 'Monthly metro pass',     40.00, DATE '2026-07-01'),
    ('Video Games',    'Season pass',            59.99, DATE '2026-07-12'),
    ('Electricity',    'July electricity bill',  72.10, DATE '2026-07-05'),
    ('Internet',       'July internet bill',     39.90, DATE '2026-07-05')
) AS x(category_name, description, amount, expense_date)
JOIN category c ON c.name = x.category_name
WHERE u.email = 'ana.petrova@example.com';

INSERT INTO expense (user_id, category_id, description, amount, expense_date)
SELECT u.id, c.id, x.description, x.amount, x.expense_date
FROM app_user u
CROSS JOIN (VALUES
    ('Groceries',               'Big weekly shop',          102.75, DATE '2026-08-03'),
    ('Dining Out',              'Sushi night',               41.60, DATE '2026-08-05'),
    ('Dining Out',              'Coffee and pastries',       28.30, DATE '2026-08-07'),
    ('Fuel',                    'Gas station fill-up',       55.00, DATE '2026-08-02'),
    ('Streaming Subscriptions', 'Netflix',                   15.99, DATE '2026-08-01'),
    ('Electricity',             'August electricity bill',   70.00, DATE '2026-08-04')
) AS x(category_name, description, amount, expense_date)
JOIN category c ON c.name = x.category_name
WHERE u.email = 'ana.petrova@example.com';
