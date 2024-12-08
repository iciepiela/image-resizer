package pl.edu.agh.to.imageresizer.services;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import pl.edu.agh.to.imageresizer.model.ImageDto;
import pl.edu.agh.to.imageresizer.model.ResizedImage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Duration;

@Service
@AllArgsConstructor
public class ImageService {
    private final OriginalImageRepository originalImageRepository;
    private final ResizedImageRepository resizedImageRepository;

//    public Flux<ImageDto> getAllImages() {
//        return originalImageRepository.findAll()
//                .map(OriginalImage::getPath)
//                .flatMap(path -> {
//                    String fullPath = path.replaceFirst("^~", System.getProperty("user.home"));
//                    Path filePath = Paths.get(fullPath);
//                    return Mono.fromCallable(() -> Files.readAllBytes(filePath))
//                            .onErrorResume(e -> {
//                                System.err.println("Error reading file: " + filePath + ", " + e.getMessage());
//                                return Mono.empty();
//                            })
//                            .repeat(3);
//                })
//                .map(el->new ImageDto("name", new String(Base64.getEncoder().encode(el))))
//                .delayElements(Duration.ofSeconds(1)) ;
//    }


    public Flux<ImageDto> getAllResizedImages() {
        return resizedImageRepository.findAll()
                .map(el -> new ImageDto(el.getImageKey(),el.getName(),el.getBase64()))
                .delayElements(Duration.ofSeconds(1));
    }

    public Mono<Boolean> resizeAndSaveOriginalImage(ImageDto imageDto) {
        return Mono.fromCallable(() -> {
            String base64Data = imageDto.getBase64();
            String base64String = base64Data.split(",")[1];
            byte[] imageBytes = java.util.Base64.getDecoder().decode(base64String);            ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);
            BufferedImage originalImage = ImageIO.read(inputStream);

            BufferedImage resizedImage = new BufferedImage(200, 200, originalImage.getType());
            resizedImage.getGraphics().drawImage(originalImage, 0, 0, 200, 200, null);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(resizedImage, "jpg", outputStream);
            byte[] resizedImageBytes = outputStream.toByteArray();
            String resizedBase64 = java.util.Base64.getEncoder().encodeToString(resizedImageBytes);

            return new ResizedImage(imageDto.getKey(),imageDto.getName(), resizedBase64);

            }).flatMap(resizedImage -> resizedImageRepository.save(resizedImage).then(Mono.just(true)))
                    .onErrorResume(e -> {
                e.printStackTrace();
                return Mono.just(false);
            });
    }

}
