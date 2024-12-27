package pl.edu.agh.to.imageresizer.services;

import org.springframework.stereotype.Service;
import pl.edu.agh.to.imageresizer.dto.ImageDto;
import pl.edu.agh.to.imageresizer.model.ResizedImage;
import reactor.core.publisher.Mono;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class ImageResizer {
    private static final int SMALL_WIDTH = 200;
    private static final int SMALL_HEIGHT = 200;
    private static final int MEDIUM_WIDTH = 400;
    private static final int MEDIUM_HEIGHT = 400;
    private static final int LARGE_WIDTH = 800;
    private static final int LARGE_HEIGHT = 800;

    public Mono<ResizedImage> resize(ImageDto imageDto, String sessionKey) {
        return Mono.fromCallable(() -> {
            BufferedImage originalImage = getOriginalImage(imageDto.base64());
            String resizedBase64small = getResizedBase64(originalImage, imageDto.base64(), SMALL_WIDTH, SMALL_HEIGHT);
            String resizedBase64medium = getResizedBase64(originalImage, imageDto.base64(), MEDIUM_WIDTH, MEDIUM_HEIGHT);
            String resizedBase64large = getResizedBase64(originalImage, imageDto.base64(), LARGE_WIDTH, LARGE_HEIGHT);

            return new ResizedImage(imageDto.imageKey(), imageDto.name(),sessionKey,
                    SMALL_WIDTH, SMALL_HEIGHT, resizedBase64small,
                    MEDIUM_WIDTH, MEDIUM_HEIGHT, resizedBase64medium,
                    LARGE_WIDTH, LARGE_HEIGHT, resizedBase64large);});
    }

    private BufferedImage getOriginalImage(String base64Data) throws IOException {
        byte[] imageBytes = java.util.Base64.getDecoder().decode(truncateBase64Prefix(base64Data));
        ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);
        return ImageIO.read(inputStream);
    }

    private String getResizedBase64(BufferedImage originalImage, String base64Data,int width, int height) throws IOException {
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
