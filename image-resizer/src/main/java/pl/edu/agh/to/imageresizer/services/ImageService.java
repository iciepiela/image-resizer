package pl.edu.agh.to.imageresizer.services;

import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import pl.edu.agh.to.imageresizer.dto.ImageDto;
import pl.edu.agh.to.imageresizer.model.OriginalImage;
import pl.edu.agh.to.imageresizer.model.ResizedImage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.time.Duration;

@Service
public class ImageService {
    private final OriginalImageRepository originalImageRepository;
    private final ResizedImageRepository resizedImageRepository;
    private final ImageResizer imageResizer;

    public ImageService(OriginalImageRepository originalImageRepository, ResizedImageRepository resizedImageRepository, ImageResizer imageResizer) {
        this.originalImageRepository = originalImageRepository;
        this.resizedImageRepository = resizedImageRepository;
        this.imageResizer = imageResizer;
    }

    public Flux<ResizedImage> getAllResizedImages() {
        return resizedImageRepository.findAll();
    }

    public Flux<ResizedImage> getResizedImagesForSessionKey(String sessionKey) {
        return Flux.just(sessionKey)
                .flatMap(resizedImageRepository::findResizedImagesBySessionKey)
                .delayElements(Duration.ofSeconds(1));
    }

    public Mono<OriginalImage> getOriginalImage(String key) {
        return resizedImageRepository.findResizedImageByImageKey(key)
                .flatMap(image -> originalImageRepository.findById(image.getOriginalImageId()))
                .delayElement(Duration.ofSeconds(1));
    }

    private Mono<OriginalImage> saveOriginalImage(ImageDto imageDto,Pair<Integer,Integer> dimensions) {
        OriginalImage originalImage = new OriginalImage(imageDto.name(), imageDto.base64(), dimensions.getFirst(), dimensions.getSecond());
        return originalImageRepository.save(originalImage);
    }

    private Mono<Boolean> resizeAndSaveResizedImage(ImageDto imageDto, String sessionKey,OriginalImage savedOriginalImage) {
        return imageResizer.resize(imageDto, sessionKey)
                .flatMap(resizedImage -> {
                    resizedImage.setOriginalImageId(savedOriginalImage.getImageId());
                    return resizedImageRepository.save(resizedImage)
                            .then(Mono.just(true));
                });
    }

    public Mono<Boolean> resizeAndSaveOriginalImage(ImageDto imageDto, String sessionKey) {
        return getImageDimensions(imageDto.base64())
                .flatMap(dimensions -> saveOriginalImage(imageDto,dimensions))
                .flatMap(savedOriginalImage -> resizeAndSaveResizedImage(imageDto, sessionKey,savedOriginalImage))
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
