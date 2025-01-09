package pl.edu.agh.to.imageresizer.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pl.edu.agh.to.imageresizer.dto.ImageDto;
import pl.edu.agh.to.imageresizer.model.ImageSize;
import pl.edu.agh.to.imageresizer.model.ResizedImage;
import reactor.core.publisher.Flux;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class ImageResizer {
    private static final Logger logger = LoggerFactory.getLogger(ImageResizer.class);

    public Flux<ResizedImage> resize(ImageDto imageDto, String sessionKey) {
        return resize(imageDto, sessionKey, Flux.just(ImageSize.values()));
    }

    public Flux<ResizedImage> resize(ImageDto imageDto, String sessionKey, Flux<ImageSize> size) {
        return size
                .map(imageSize -> {
                    try {
                        return new ResizedImage(
                                imageDto.imageKey(),
                                imageDto.name(),
                                getResizedBase64(imageDto.base64(), imageSize.getWidth(), imageSize.getHeight()),
                                sessionKey,
                                imageSize.getWidth(),
                                imageSize.getHeight()
                        );
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to resize image: " + e.getMessage(), e);
                    }
                })
                .onErrorResume(e -> {
                    logger.error(e.getMessage());
                    return Flux.just(new ResizedImage(
                            imageDto.imageKey(),
                            imageDto.name(),
                            ImageService.ERROR,
                            sessionKey,
                            ImageService.ERROR_WIDTH_AND_HEIGHT,
                            ImageService.ERROR_WIDTH_AND_HEIGHT
                    ));
                });
    }

    private BufferedImage getOriginalImage(String base64Data) throws IOException {
        byte[] imageBytes = java.util.Base64.getDecoder().decode(truncateBase64Prefix(base64Data));
        ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);
        return ImageIO.read(inputStream);
    }

    private String getResizedBase64(String base64Data, int width, int height) throws IOException {
        BufferedImage originalImage = getOriginalImage(base64Data);
        BufferedImage resizedImage = new BufferedImage(width, height, originalImage.getType());
        resizedImage.getGraphics().drawImage(originalImage, 0, 0, width, height, null);
        System.out.println(width + " " + height);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(resizedImage, getFormatName(base64Data), outputStream);
        byte[] resizedImageBytes = outputStream.toByteArray();
        return java.util.Base64.getEncoder().encodeToString(resizedImageBytes);

    }

    private String getFormatName(String base64Data) {
        return base64Data.split(";")[0].split("/")[1];
    }

    private String truncateBase64Prefix(String base64Data) {
        return base64Data.split(",")[1];
    }

}
