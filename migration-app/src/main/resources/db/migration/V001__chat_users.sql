
CREATE TABLE chat_users (
    id VARCHAR(127) PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL
);