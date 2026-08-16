INSERT INTO expense (user_id, category_id, description, amount, expense_date)
SELECT u.id, c.id, x.description, x.amount, x.expense_date
FROM app_user u
CROSS JOIN (VALUES
    ('Groceries',               'Weekly grocery run',      48.20, DATE '2026-06-03'),
    ('Groceries',               'Weekly grocery run',      52.30, DATE '2026-06-18'),
    ('Dining Out',              'Quick lunch',             15.00, DATE '2026-06-10'),
    ('Fuel',                    'Gas station fill-up',     38.00, DATE '2026-06-07'),
    ('Public Transit',          'Monthly bus pass',        40.00, DATE '2026-06-01'),
    ('Streaming Subscriptions', 'Netflix',                 15.99, DATE '2026-06-01'),
    ('Streaming Subscriptions', 'Disney+',                 11.99, DATE '2026-06-01'),
    ('Electricity',             'June electricity bill',   55.40, DATE '2026-06-02'),
    ('Internet',                'June internet bill',      35.00, DATE '2026-06-02')
) AS x(category_name, description, amount, expense_date)
JOIN category c ON c.name = x.category_name AND c.user_id IS NULL
WHERE u.email = 'dev.tester@example.com';

INSERT INTO expense (user_id, category_id, description, amount, expense_date)
SELECT u.id, c.id, x.description, x.amount, x.expense_date
FROM app_user u
CROSS JOIN (VALUES
    ('Groceries',      'Big weekly shop',       110.20, DATE '2026-06-05'),
    ('Groceries',      'Farmers market',         98.75, DATE '2026-06-20'),
    ('Dining Out',     'Anniversary dinner',     62.40, DATE '2026-06-12'),
    ('Public Transit', 'Monthly metro pass',     40.00, DATE '2026-06-01'),
    ('Video Games',    'Summer sale bundle',     74.99, DATE '2026-06-15'),
    ('Electricity',    'June electricity bill',  80.30, DATE '2026-06-04'),
    ('Internet',       'June internet bill',     39.90, DATE '2026-06-04')
) AS x(category_name, description, amount, expense_date)
JOIN category c ON c.name = x.category_name AND c.user_id IS NULL
WHERE u.email = 'ana.petrova@example.com';