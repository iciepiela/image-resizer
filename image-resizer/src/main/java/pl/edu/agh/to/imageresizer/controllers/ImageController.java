package pl.edu.agh.to.imageresizer.controllers;

import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import pl.edu.agh.to.imageresizer.dto.ImageDto;
import pl.edu.agh.to.imageresizer.services.ImageService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
    public Flux<ResponseEntity<ImageDto>> getImagesBySessionKey(@RequestParam String sessionKey) {
        return imageService.getResizedImagesForSessionKey(sessionKey)
                .map(resizedImage -> new ImageDto(
                        resizedImage.getImageKey(),
                        resizedImage.getName(),
                        resizedImage.getBase64(),
                        resizedImage.getWidth(),
                        resizedImage.getHeight()
                ))
                .concatWith(Flux.just(new ImageDto(COMPLETE_REQUEST, COMPLETE_REQUEST, COMPLETE_REQUEST, 0, 0))
                        .delayElements(java.time.Duration.ofSeconds(1)))
                .map(element -> ResponseEntity.ok().body(element));
    }

    @GetMapping(value = "/resized/all", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ResponseEntity<ImageDto>> getAllImages() {
        return imageService.getAllResizedImages()
                .map(resizedImage -> new ImageDto(
                        resizedImage.getImageKey(),
                        resizedImage.getName(),
                        resizedImage.getBase64(),
                        resizedImage.getWidth(),
                        resizedImage.getHeight()
                ))
                .concatWith(Flux.just(new ImageDto(COMPLETE_REQUEST, COMPLETE_REQUEST, COMPLETE_REQUEST, 0, 0))
                        .delayElements(java.time.Duration.ofSeconds(1)))
                .map(element -> ResponseEntity.ok().body(element));
    }

    @PostMapping(value = "/upload", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ResponseEntity<ImageDto>> uploadImages(@RequestBody List<ImageDto> images) {
        return Flux.fromIterable(images)
                .flatMap(image -> imageService.resizeAndSaveOriginalImage(image, "2"))
                .map(resizedImage -> new ImageDto(
                        resizedImage.getImageKey(),
                        resizedImage.getName(),
                        resizedImage.getBase64(),
                        resizedImage.getWidth(),
                        resizedImage.getHeight()))
                .concatWith(Flux.just(new ImageDto(COMPLETE_REQUEST, COMPLETE_REQUEST, COMPLETE_REQUEST, 0, 0))) // Add the completion signal here
                .delayElements(java.time.Duration.ofSeconds(1))
                .map(element -> ResponseEntity.ok().body(element))
                .doOnNext(response -> logger.info(response.getBody().toString()))
                .onErrorResume(e -> {
                    e.printStackTrace();
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(null));
                });
    }


}
