package pl.edu.agh.to.imageresizer.services;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import pl.edu.agh.to.imageresizer.controllers.ImageController;
import pl.edu.agh.to.imageresizer.dto.ImageDto;
import pl.edu.agh.to.imageresizer.model.ImageSize;
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
    public static final String ERROR = "ERROR";
    public static final int ERROR_WIDTH_AND_HEIGHT = 0;
    private static final int PAGE_SIZE = 10;
    private final OriginalImageRepository originalImageRepository;
    private final ResizedImageRepository resizedImageRepository;
    private final ImageResizer imageResizer;
    private final Logger logger = LoggerFactory.getLogger(ImageController.class);


    public ImageService(OriginalImageRepository originalImageRepository, ResizedImageRepository resizedImageRepository, ImageResizer imageResizer) {
        this.originalImageRepository = originalImageRepository;
        this.resizedImageRepository = resizedImageRepository;
        this.imageResizer = imageResizer;
    }

    @PostConstruct
    public void reprocessAllImages() {
        Flux.just(ImageSize.values())
                .flatMap(this::processImageSize)
                .subscribe(
                        result -> logger.info("Image processed successfully."),
                        error -> logger.error("Error processing image", error)
                );
    }

    private Flux<Boolean> processImageSize(ImageSize imageSize) {
        return processPage(0, imageSize);
    }

    private Flux<Boolean> processPage(int page, ImageSize imageSize) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);

        return originalImageRepository.findOriginalImagesWithoutResizedImageOfSize(imageSize.getWidth(), imageSize.getHeight(), pageable)
                .flatMap(originalImage -> resizeSingleThumbnail(imageSize, originalImage))
                .concatWith(hasNextPage(page, imageSize)
                        .flatMap(hasNext -> {
                            if (hasNext) {
                                return processPage(page + 1, imageSize);
                            } else {
                                return Flux.just(false);
                            }
                        })
                );
    }

    private Flux<Boolean> hasNextPage(int currentPage, ImageSize imageSize) {
        Pageable pageable = PageRequest.of(currentPage, PAGE_SIZE);

        return originalImageRepository.findOriginalImagesWithoutResizedImageOfSize(imageSize.getWidth(), imageSize.getHeight(), pageable)
                .hasElements()
                .flatMapMany(hasElements -> {
                    if (hasElements) {
                        return Flux.just(true);
                    } else {
                        return Flux.just(false);
                    }
                });
    }

    public Flux<ResizedImage> getAllResizedImages(ImageSize imageSize) {
        return Flux.concat(
                resizedImageRepository.findResizedImagesByWidthAndHeight(imageSize.getWidth(), imageSize.getHeight()),
                resizedImageRepository.findResizedImagesByWidthAndHeight(ERROR_WIDTH_AND_HEIGHT, ERROR_WIDTH_AND_HEIGHT)
        );

    }

    public Flux<ResizedImage> getResizedImagesForSessionKey(String sessionKey, ImageSize imageSize) {
        return Flux.just(sessionKey)
                .flatMap(key ->
                        Flux.merge(
                                resizedImageRepository.findResizedImagesBySessionKeyAndWidthAndHeight(key,
                                        imageSize.getWidth(), imageSize.getHeight()),
                                resizedImageRepository.findResizedImagesBySessionKeyAndWidthAndHeight(key,
                                        ERROR_WIDTH_AND_HEIGHT, ERROR_WIDTH_AND_HEIGHT)));

    }

    public Flux<ResizedImage> getResizedImagesByImageKey(String imageKey, ImageSize imageSize) {
        return Flux.just(imageKey)
                .flatMap(key ->
                        Flux.merge(
                                resizedImageRepository
                                        .findResizedImagesByImageKeyAndWidthAndHeight(key, imageSize.getWidth(), imageSize.getHeight()),
                                resizedImageRepository
                                        .findResizedImagesByImageKeyAndWidthAndHeight(key, ERROR_WIDTH_AND_HEIGHT, ERROR_WIDTH_AND_HEIGHT)
                        )
                );


    }


    public Mono<OriginalImage> getOriginalImage(String key) {
        return resizedImageRepository.findResizedImageByImageKey(key)
                .next()
                .flatMap(image -> originalImageRepository.findById(image.getOriginalImageId()))
                .delayElement(Duration.ofMillis(500));
    }

    public Mono<Boolean> resizeAndSaveOriginalImage(ImageDto imageDto, String sessionKey) {
        return getImageDimensions(imageDto.base64())
                .flatMap(dimensions -> saveOriginalImage(imageDto, dimensions, sessionKey))
                .flatMap(savedOriginalImage -> resizeImage(imageDto, sessionKey, savedOriginalImage)
                        .all(result -> result))
                .onErrorResume(e -> saveErrorOriginalImage(imageDto, sessionKey)
                        .flatMap(savedImage -> saveErrorResizedImage(imageDto, sessionKey, savedImage))
                        .then(Mono.just(false)));
    }

    private Flux<Boolean> resizeSingleThumbnail(ImageSize imageSize, OriginalImage originalImage) {
        return resizedImageRepository.findResizedImagesByOriginalImageIdAndWidthAndHeight(
                        originalImage.getImageId(),
                        imageSize.getWidth(),
                        imageSize.getHeight())
                .hasElements()
                .flatMapMany(imageExists -> imageExists ? Flux.just(true) : resizeImage(originalImage, imageSize));
    }

    private Mono<OriginalImage> saveOriginalImage(ImageDto imageDto, Pair<Integer, Integer> dimensions, String sessionKey) {
        OriginalImage originalImage = new OriginalImage(imageDto.name(),
                imageDto.base64(),
                sessionKey,
                imageDto.imageKey(),
                dimensions.getFirst(),
                dimensions.getSecond());

        return originalImageRepository.save(originalImage)
                .onErrorResume(e -> {
                    OriginalImage errorImage = new OriginalImage(imageDto.name(), ERROR,
                            sessionKey,
                            imageDto.imageKey(),
                            ERROR_WIDTH_AND_HEIGHT, ERROR_WIDTH_AND_HEIGHT);
                    return originalImageRepository.save(errorImage);
                });
    }

    private Flux<Boolean> resizeImage(OriginalImage savedOriginalImage, ImageSize imageSize) {
        ImageDto imageDto = new ImageDto(
                savedOriginalImage.getImageKey(),
                savedOriginalImage.getName(),
                savedOriginalImage.getBase64(),
                savedOriginalImage.getWidth(), savedOriginalImage.getHeight()
        );
        return saveResizedImage(imageResizer.resize(imageDto, savedOriginalImage.getSessionKey(), Flux.just(imageSize)),
                savedOriginalImage, imageDto, savedOriginalImage.getSessionKey());
    }

    private Flux<Boolean> resizeImage(ImageDto imageDto, String sessionKey, OriginalImage savedOriginalImage) {
        return saveResizedImage(imageResizer.resize(imageDto, sessionKey), savedOriginalImage, imageDto, sessionKey);
    }

    private Flux<Boolean> saveResizedImage(Flux<ResizedImage> resizedImages, OriginalImage savedOriginalImage, ImageDto imageDto, String sessionKey) {
        return resizedImages.flatMap(resizedImage -> {
                    resizedImage.setOriginalImageId(savedOriginalImage.getImageId());
                    return resizedImageRepository.save(resizedImage)
                            .then(Mono.just(true));
                })
                .onErrorResume(e -> saveErrorResizedImage(imageDto, sessionKey, savedOriginalImage));
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

    private Mono<Boolean> saveErrorResizedImage(ImageDto imageDto, String sessionKey, OriginalImage savedOriginalImage) {
        ResizedImage resizedImage = new ResizedImage(
                imageDto.imageKey(),
                imageDto.name(),
                ERROR,
                sessionKey,
                ERROR_WIDTH_AND_HEIGHT,
                ERROR_WIDTH_AND_HEIGHT
        );
        resizedImage.setOriginalImageId(savedOriginalImage.getImageId());
        return resizedImageRepository.save(resizedImage)
                .then(Mono.just(true));
    }

    private Mono<OriginalImage> saveErrorOriginalImage(ImageDto imageDto, String sessionKey) {
        OriginalImage originalImage = new OriginalImage(
                imageDto.name(),
                ERROR,
                sessionKey,
                imageDto.imageKey(),
                ERROR_WIDTH_AND_HEIGHT,
                ERROR_WIDTH_AND_HEIGHT
        );
        return originalImageRepository.save(originalImage);
    }

}
