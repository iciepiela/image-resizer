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
create table resized_images
(
    image_id       serial
        primary key,
    original_image bigint
        constraint fk_original_image
            references original_images
            on delete cascade,
    image_key      varchar(255),
    session_key    varchar(255),
    width          integer,
    height         integer,
    name           varchar(255),
    base64         text
);