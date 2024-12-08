-- Create table for OriginalImage
CREATE TABLE IF NOT EXISTS original_image (
    image_id SERIAL PRIMARY KEY,
    path VARCHAR(255) NOT NULL
);

-- Create table for ResizedImage
CREATE TABLE IF NOT EXISTS resized_images (
    image_id SERIAL PRIMARY KEY,
    path VARCHAR(255) NOT NULL
);
