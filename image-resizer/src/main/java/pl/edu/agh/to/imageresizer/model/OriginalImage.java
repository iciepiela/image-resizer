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
    private int width;
    private int height;

    public OriginalImage(String name, String base64, int width, int height) {
        this.name = name;
        this.base64 = base64;
        this.width = width;
        this.height = height;
    }
}