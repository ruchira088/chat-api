
CREATE TABLE credentials (
    user_id VARCHAR(127) PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    last_updated_at TIMESTAMP NOT NULL,
    salted_hashed_password VARCHAR(127) NOT NULL,

    CONSTRAINT fk_credentials_user_id FOREIGN KEY (user_id) REFERENCES chat_users (id)
);