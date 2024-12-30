package pl.edu.agh.to.imageresizer.controllers;

import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.edu.agh.to.imageresizer.dto.ImageDto;
import pl.edu.agh.to.imageresizer.model.ImageSize;
import pl.edu.agh.to.imageresizer.model.ResizedImage;
import pl.edu.agh.to.imageresizer.services.ImageService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/images")
public class ImageController {
    private static final Logger logger = LoggerFactory.getLogger(ImageController.class);
    private final ImageService imageService;
    private final String COMPLETE_REQUEST = "COMPLETE_REQUEST";

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Server is running");
    }

    @GetMapping(value = "/original", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<ImageDto>> getOriginalImage(@RequestParam String imageKey) {
        return imageService.getOriginalImage(imageKey)
                .map(originalImage -> new ImageDto(
                        null,
                        originalImage.getName(),
                        originalImage.getBase64(),
                        originalImage.getWidth(),
                        originalImage.getHeight()
                ))
                .map(element -> ResponseEntity.ok().body(element));
    }

    @GetMapping(value = "/resized", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ResponseEntity<ImageDto>> getImagesBySessionKey(@RequestParam String sessionKey,@RequestParam String sizeString) {
        return imageService.getResizedImagesForSessionKey(sessionKey, ImageSize.valueOf(sizeString.toUpperCase()))
                .map(this::convertToImageDto)
                .doOnNext(image -> logger.info(image.toString()))
                .concatWith(Flux.just(new ImageDto(COMPLETE_REQUEST, COMPLETE_REQUEST, COMPLETE_REQUEST, 0, 0))
                        .delayElements(java.time.Duration.ofMillis(500)))
                .map(element -> ResponseEntity.ok().body(element));
    }

    @GetMapping(value = "/resized/all", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ResponseEntity<ImageDto>> getAllImages(@RequestParam String sizeString) {
        ImageSize size = ImageSize.valueOf(sizeString.toUpperCase());
        logger.info("Getting all images in size: {}", size);
        return imageService.getAllResizedImages(size)
                .map(this::convertToImageDto)
                .doOnNext(image -> logger.info(image.toString()))
                .concatWith(Flux.just(new ImageDto(COMPLETE_REQUEST, COMPLETE_REQUEST, COMPLETE_REQUEST, 0, 0))
                        .delayElements(java.time.Duration.ofMillis(500)))
                .map(element -> ResponseEntity.ok().body(element));
    }

    @PostMapping(value = "/upload", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<String>> uploadImages(@RequestBody List<ImageDto> images, HttpSession httpSession) {
        String sessionKey = httpSession.getId();
        Flux.fromIterable(images)
                .flatMap(image -> imageService.resizeAndSaveOriginalImage(image, sessionKey))
                .doOnError(e -> logger.error("Error during image processing", e))
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();

        return Mono.just(ResponseEntity.status(HttpStatus.OK).body(sessionKey));
    }

    private ImageDto convertToImageDto(ResizedImage resizedImage) {
        return new ImageDto(
                resizedImage.getImageKey(),
                resizedImage.getName(),
                resizedImage.getBase64(),
                resizedImage.getWidth(),
                resizedImage.getHeight()
        );
    }
}
