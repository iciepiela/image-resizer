package pl.edu.agh.to.imageresizer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@AllArgsConstructor
@Table(name = "original_images")
public class OriginalImage {
    @Id
    private Long imageId;
    private String name;
    private String base64;

    public OriginalImage(String name, String base64) {
        this.name = name;
        this.base64 = base64;
    }
}