package pl.edu.agh.to.imageresizer.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table(name = "original_images")
public class OriginalImage {
    @Id
    private long imageId;
    private String name;
    private String base64;
    private int width;
    private int height;

    public OriginalImage() {
    }

    public OriginalImage(long imageId, String name, String base64, int width, int height) {
        this.imageId = imageId;
        this.name = name;
        this.base64 = base64;
        this.width = width;
        this.height = height;
    }


    public OriginalImage(String name, String base64, int width, int height) {
        this.name = name;
        this.base64 = base64;
        this.width = width;
        this.height = height;
    }

    public long getImageId() {
        return imageId;
    }

    public String getName() {
        return name;
    }

    public String getBase64() {
        return base64;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}