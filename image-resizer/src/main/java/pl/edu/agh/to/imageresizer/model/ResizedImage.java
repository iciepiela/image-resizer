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
    private Long imageId;
    private String path;
}
