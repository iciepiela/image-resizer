create table directories
(
    directory_id        serial
        primary key,
    name                varchar(255),
    directory_key                varchar(255),
    image_count INTEGER DEFAULT 0,
    sub_directories_count INTEGER DEFAULT 0,
    parent_directory_id bigint NULL
        constraint fk_pdirectory
            references directories
            on delete cascade
);

CREATE TABLE original_images
(
    image_id SERIAL PRIMARY KEY,
    name    VARCHAR(255),
    image_key      VARCHAR(255),
    session_key      VARCHAR(255),
    base64  TEXT,
    width INTEGER,
    height INTEGER,
    parent_directory_id BIGINT,

    CONSTRAINT fk_directory FOREIGN KEY (parent_directory_id)
        REFERENCES directories (directory_id)
        ON DELETE CASCADE
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
    base64        TEXT,

    CONSTRAINT fk_original_image FOREIGN KEY (original_image)
        REFERENCES original_images (image_id)
        ON DELETE CASCADE
);