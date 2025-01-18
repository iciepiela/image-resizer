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

CREATE TABLE directories
(
    directory_id SERIAL PRIMARY KEY, 
    name VARCHAR(255),
    directory_key VARCHAR(255), 
    parent_key VARCHAR(255),
    session_key VARCHAR(255),
    image_count INTEGER DEFAULT 0,
    sub_directories_count INTEGER DEFAULT 0
);

-- Create table for ResizedImage
create table resized_images
(
    image_id       serial
        primary key,
    image_key      varchar(255),
    session_key    varchar(255),
    width          integer,
    height         integer,
    name           varchar(255),
    base64         TEXT
    original_image bigint
        constraint fk_original_image
            references original_images
            on delete cascade, 
    directory_key varchar(255)
        constraint fk_directory
            references directories
            on delete cascade
);