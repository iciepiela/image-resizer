package pl.edu.agh.to.imageresizer.services;

import lombok.AllArgsConstructor;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import pl.edu.agh.to.imageresizer.model.ImageDto;
import pl.edu.agh.to.imageresizer.model.OriginalImage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.time.Duration;

@Service
@AllArgsConstructor
public class ImageService {
    private final OriginalImageRepository originalImageRepository;
    private final ResizedImageRepository resizedImageRepository;
    private final ImageResizer imageResizer;

    public Flux<ImageDto> getAllResizedImages() {
        return resizedImageRepository.findAll()
                .map(el -> new ImageDto(el.getImageKey(), el.getName(), el.getBase64()))
                .delayElements(Duration.ofSeconds(1));
    }

    public Flux<ImageDto> getResizedImagesForSessionKey(String sessionKey) {
        return Flux.just(sessionKey)
                .flatMap(resizedImageRepository::findResizedImagesBySessionKey)
                .map(el -> new ImageDto(el.getImageKey(), el.getName(), el.getBase64()))
                .delayElements(Duration.ofSeconds(1));
    }

    public Mono<Boolean> resizeAndSaveOriginalImage(ImageDto imageDto, String sessionKey) {
        return getImageDimensions(imageDto.getBase64())
                .flatMap(dimensions -> {
                    OriginalImage originalImage = new OriginalImage(imageDto.getName(), imageDto.getBase64(), dimensions.getFirst(), dimensions.getSecond());
                    return originalImageRepository.save(originalImage)
                            .flatMap(savedOriginalImage ->
                                    imageResizer.resize(imageDto, sessionKey)
                                            .flatMap(resizedImage -> {
                                                resizedImage.setOriginalImageId(savedOriginalImage.getImageId());
                                                return resizedImageRepository.save(resizedImage)
                                                        .then(Mono.just(true));
                                            })
                            );
                })
                .onErrorResume(e -> {
                    e.printStackTrace();
                    return Mono.just(false);
                });
    }

    private Mono<Pair<Integer, Integer>> getImageDimensions(String base64) {
        return Mono.fromCallable(() -> {
            String base64String = base64.split(",")[1];
            byte[] imageBytes = java.util.Base64.getDecoder().decode(base64String);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);

            BufferedImage originalImage = ImageIO.read(inputStream);
            if (originalImage == null) {
                throw new IllegalArgumentException("Invalid image data");
            }

            return Pair.of(originalImage.getWidth(), originalImage.getHeight());
        });
    }

}
