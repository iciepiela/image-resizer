package pl.edu.agh.to.imageresizer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "original_images")
public class OriginalImage {
    @Id
    private long imageId;
    private String name;
    private String base64;
    private String sessionKey;
    private String imageKey;
    private int width;
    private int height;
    private long parentDirectoryId;

    public OriginalImage(String name, String base64, String sessionKey, String imageKey, int width, int height, long parentDirectory) {
        this.name = name;
        this.base64 = base64;
        this.width = width;
        this.height = height;
        this.sessionKey = sessionKey;
        this.imageKey = imageKey;
        this.parentDirectoryId = parentDirectory;
    }
}