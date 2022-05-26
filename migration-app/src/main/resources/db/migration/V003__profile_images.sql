
CREATE TABLE profile_images (
    user_id VARCHAR(127) PRIMARY KEY,
    file_id VARCHAR(36) NOT NULL,

    CONSTRAINT fk_profile_image_user_id FOREIGN KEY (user_id) REFERENCES chat_users(id)
);