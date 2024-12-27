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
    private String sessionKey;
    @Column("widthSmall")
    private int widthSmall;
    @Column("heightSmall")
    private int heightSmall;
    @Column("base64Small")
    private String base64Small;
    @Column("widthMedium")
    private int widthMedium;
    @Column("heightMedium")
    private int heightMedium;
    @Column("base64Medium")
    private String base64Medium;
    @Column("widthLarge")
    private int widthLarge;
    @Column("heightLarge")
    private int heightLarge;
    @Column("base64Large")
    private String base64Large;

    public ResizedImage(String imageKey, String name, String sessionKey,
                        int widthSmall, int heightSmall, String base64Small,
                        int widthMedium, int heightMedium, String base64Medium,
                        int widthLarge, int heightLarge, String base64Large) {
        this.imageKey = imageKey;
        this.name = name;
        this.sessionKey = sessionKey;
        this.widthSmall = widthSmall;
        this.heightSmall = heightSmall;
        this.base64Small = base64Small;
        this.widthMedium = widthMedium;
        this.heightMedium = heightMedium;
        this.base64Medium = base64Medium;
        this.widthLarge = widthLarge;
        this.heightLarge = heightLarge;
        this.base64Large = base64Large;
    }

    public int getWidth(ImageSize size) {
        switch (size) {
            case SMALL:
                return widthSmall;
            case MEDIUM:
                return widthMedium;
            case LARGE:
                return widthLarge;
            default:
                return 0;
        }
    }

    public int getHeight(ImageSize size) {
        switch (size) {
            case SMALL:
                return heightSmall;
            case MEDIUM:
                return heightMedium;
            case LARGE:
                return heightLarge;
            default:
                return 0;
        }
    }

    public String getBase64(ImageSize size) {
        switch (size) {
            case SMALL:
                return base64Small;
            case MEDIUM:
                return base64Medium;
            case LARGE:
                return base64Large;
            default:
                return "";
        }
    }
}
