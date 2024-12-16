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
        private static final int WIDTH = 200;
        private static final int HEIGHT = 200;

        public Mono<ResizedImage> resize(ImageDto imageDto, String sessionKey) {
            return Mono.fromCallable(() -> {
                BufferedImage originalImage = getOriginalImage(imageDto.base64());
                String resizedBase64 = getResizedBase64(originalImage, imageDto.base64());

                return new ResizedImage(imageDto.imageKey(), imageDto.name(), resizedBase64, sessionKey, WIDTH, HEIGHT);
            });
        }

        private BufferedImage getOriginalImage(String base64Data) throws IOException {
            byte[] imageBytes = java.util.Base64.getDecoder().decode(truncateBase64Prefix(base64Data));
            ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);
            return ImageIO.read(inputStream);
        }

        private String getResizedBase64(BufferedImage originalImage, String base64Data) throws IOException {
            BufferedImage resizedImage = new BufferedImage(WIDTH, HEIGHT, originalImage.getType());
            resizedImage.getGraphics().drawImage(originalImage, 0, 0, 200, 200, null);

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
