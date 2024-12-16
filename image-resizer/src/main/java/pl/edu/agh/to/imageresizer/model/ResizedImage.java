package pl.edu.agh.to.imageresizer.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;


@SuppressWarnings({"LombokSetterMayBeUsed", "LombokGetterMayBeUsed"})
@Table(name = "resized_images")
public class ResizedImage {
    @Id
    private long imageId;
    @Column("original_image")
    private long originalImageId;
    private String imageKey;
    private String name;
    private String base64;
    private String sessionKey;
    private int width;
    private int height;


    public ResizedImage() {
    }

    public ResizedImage(Long imageId, Long originalImageId, String imageKey, String name, String base64, String sessionKey, Integer width, Integer height) {
        this.imageId = imageId;
        this.originalImageId = originalImageId;
        this.imageKey = imageKey;
        this.name = name;
        this.base64 = base64;
        this.sessionKey = sessionKey;
        this.width = width;
        this.height = height;
    }

    public ResizedImage(String imageKey, String name, String base64, String sessionKey, Integer width, Integer height) {
        this.imageKey = imageKey;
        this.name = name;
        this.base64 = base64;
        this.sessionKey = sessionKey;
        this.width = width;
        this.height = height;
    }

    public void setOriginalImageId(long originalImageId) {
        this.originalImageId = originalImageId;
    }

    public long getOriginalImageId() {
        return originalImageId;
    }

    public long getImageId() {
        return imageId;
    }

    public String getImageKey() {
        return imageKey;
    }

    public String getName() {
        return name;
    }

    public String getBase64() {
        return base64;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
