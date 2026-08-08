-- Only applied when the "dev" Spring profile is active

INSERT INTO app_user (display_name, email, password_hash, monthly_salary, total_saved)
VALUES ('Dev Tester', 'dev.tester@example.com', '$2a$10$mockMockMockMockMockMockMockMockMockMockMockMockMoc', 3200.00, 500.00);

INSERT INTO category (name, parent_category_id) VALUES ('Food', NULL);
INSERT INTO category (name, parent_category_id) VALUES ('Transportation', NULL);
INSERT INTO category (name, parent_category_id) VALUES ('Entertainment', NULL);
INSERT INTO category (name, parent_category_id) VALUES ('Utilities', NULL);

INSERT INTO category (name, parent_category_id) SELECT 'Groceries', id FROM category WHERE name = 'Food';
INSERT INTO category (name, parent_category_id) SELECT 'Dining Out', id FROM category WHERE name = 'Food';
INSERT INTO category (name, parent_category_id) SELECT 'Fuel', id FROM category WHERE name = 'Transportation';
INSERT INTO category (name, parent_category_id) SELECT 'Public Transit', id FROM category WHERE name = 'Transportation';
INSERT INTO category (name, parent_category_id) SELECT 'Streaming Subscriptions', id FROM category WHERE name = 'Entertainment';
INSERT INTO category (name, parent_category_id) SELECT 'Video Games', id FROM category WHERE name = 'Entertainment';
INSERT INTO category (name, parent_category_id) SELECT 'Electricity', id FROM category WHERE name = 'Utilities';
INSERT INTO category (name, parent_category_id) SELECT 'Internet', id FROM category WHERE name = 'Utilities';

INSERT INTO expense (user_id, category_id, description, amount, expense_date)
SELECT u.id, c.id, x.description, x.amount, x.expense_date
FROM app_user u
CROSS JOIN (VALUES
    ('Groceries',               'Weekly grocery run',    62.40, DATE '2026-07-01'),
    ('Groceries',               'Weekly grocery run',    58.15, DATE '2026-07-04'),
    ('Dining Out',               'Lunch with coworkers',  18.50, DATE '2026-07-02'),
    ('Dining Out',               'Pizza night',           23.50, DATE '2026-07-05'),
    ('Fuel',                     'Gas station fill-up',   45.00, DATE '2026-07-03'),
    ('Public Transit',           'Monthly bus pass',      40.00, DATE '2026-07-01'),
    ('Streaming Subscriptions',  'Netflix',                15.99, DATE '2026-07-01'),
    ('Streaming Subscriptions',  'Disney+',                11.99, DATE '2026-07-01'),
    ('Video Games',              'New release',            29.99, DATE '2026-07-05'),
    ('Electricity',              'August electricity bill', 65.00, DATE '2026-07-02'),
    ('Internet',                 'August internet bill',   35.00, DATE '2026-07-02')
) AS x(category_name, description, amount, expense_date)
JOIN category c ON c.name = x.category_name
WHERE u.email = 'dev.tester@example.com';