package pl.edu.agh.to.imageresizer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name="resized_images")
public class ResizedImage {
    @Id
    private long image_id;
    private String imageKey;
    private String name;
    private String base64;

    public ResizedImage(String key, String name, String resizedBase64) {
        this.imageKey = key;
        this.name = name;
        this.base64 = resizedBase64;
    }
}
