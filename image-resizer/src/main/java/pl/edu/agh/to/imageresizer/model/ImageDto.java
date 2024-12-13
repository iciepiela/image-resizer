package pl.edu.agh.to.imageresizer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@Data
@AllArgsConstructor
@ToString
public class ImageDto {
    private String imageKey;
    private String name;
    private String base64;
    private Integer width;
    private Integer height;
}
