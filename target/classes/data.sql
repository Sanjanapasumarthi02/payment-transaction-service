-- Pre-load 5 initial users with 5000 balance each
INSERT INTO users (username, email, balance, created_at) VALUES ('alice', 'alice@example.com', 5000.00, CURRENT_TIMESTAMP);
INSERT INTO users (username, email, balance, created_at) VALUES ('bob', 'bob@example.com', 5000.00, CURRENT_TIMESTAMP);
INSERT INTO users (username, email, balance, created_at) VALUES ('charlie', 'charlie@example.com', 5000.00, CURRENT_TIMESTAMP);
INSERT INTO users (username, email, balance, created_at) VALUES ('diana', 'diana@example.com', 5000.00, CURRENT_TIMESTAMP);
INSERT INTO users (username, email, balance, created_at) VALUES ('eve', 'eve@example.com', 5000.00, CURRENT_TIMESTAMP);
