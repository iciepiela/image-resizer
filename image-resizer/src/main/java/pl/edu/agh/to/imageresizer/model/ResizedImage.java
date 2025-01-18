package pl.edu.agh.to.imageresizer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;


@Data
@AllArgsConstructor
@NoArgsConstructor
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
    @Column("directory_key")
    private String directoryKey;
    private int width;
    private int height;

    public ResizedImage(String imageKey, String name, String base64, String sessionKey,String directoryKey, Integer width, Integer height) {
        this.imageKey = imageKey;
        this.name = name;
        this.base64 = base64;
        this.sessionKey = sessionKey;
        this.directoryKey = directoryKey;
        this.width = width;
        this.height = height;
    }
}
