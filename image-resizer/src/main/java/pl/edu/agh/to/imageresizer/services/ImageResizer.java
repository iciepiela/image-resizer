package pl.edu.agh.to.imageresizer.services;

import org.springframework.stereotype.Service;
import pl.edu.agh.to.imageresizer.model.ImageDto;
import pl.edu.agh.to.imageresizer.model.ResizedImage;
import reactor.core.publisher.Mono;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@Service
public class ImageResizer {
    public Mono<ResizedImage> resize(ImageDto imageDto, String sessionKey) {
        Integer width = 200;
        Integer height = 200;
        return Mono.fromCallable(() -> {
            String base64Data = imageDto.getBase64();
            String base64String = base64Data.split(",")[1];
            byte[] imageBytes = java.util.Base64.getDecoder().decode(base64String);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);
            BufferedImage originalImage = ImageIO.read(inputStream);

            BufferedImage resizedImage = new BufferedImage(width, height, originalImage.getType());
            resizedImage.getGraphics().drawImage(originalImage, 0, 0, 200, 200, null);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(resizedImage, "png", outputStream);
            byte[] resizedImageBytes = outputStream.toByteArray();
            String resizedBase64 = java.util.Base64.getEncoder().encodeToString(resizedImageBytes);

            return new ResizedImage(imageDto.getKey(), imageDto.getName(), resizedBase64, sessionKey, width, height);

        });
    }

}
