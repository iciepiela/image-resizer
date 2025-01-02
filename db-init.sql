-- Create table for Originalmage
CREATE TABLE original_images
(
    image_id SERIAL PRIMARY KEY,
    name    VARCHAR(255),
    image_key      VARCHAR(255),
    session_key      VARCHAR(255),
    base64  TEXT,
    width INTEGER,
    height INTEGER
);

-- Create table for ResizedImage
CREATE TABLE IF NOT EXISTS resized_images
(
    image_id       SERIAL PRIMARY KEY,
    original_image BIGINT,
    image_key      VARCHAR(255),
    session_key      VARCHAR(255),
    width INTEGER,
    height INTEGER,
    name          VARCHAR(255),

    CONSTRAINT fk_original_image FOREIGN KEY (original_image)
        REFERENCES original_images (image_id)
        ON DELETE CASCADE
);