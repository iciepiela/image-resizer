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
    private Long imageId;
    private String name;
    private String base64;
    private Integer width;
    private Integer height;

    public OriginalImage(String name, String base64, Integer width, Integer height) {
        this.name = name;
        this.base64 = base64;
        this.width = width;
        this.height = height;
    }
}