package pl.edu.agh.to.imageresizer.controllers;

import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.edu.agh.to.imageresizer.dto.ImageDto;
import pl.edu.agh.to.imageresizer.services.ImageService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/images")
public class ImageController {
    private final ImageService imageService;
    private static final Logger logger = LoggerFactory.getLogger(ImageController.class);
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

    @GetMapping(value= "/resized/image",produces = MediaType.APPLICATION_JSON_VALUE )
    public Mono<ResponseEntity<ImageDto>> getResizedImage(@RequestParam String imageKey) {
        return imageService.getResizedImage(imageKey)
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

    @PostMapping(value = "/upload", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<String>> uploadImages(@RequestBody List<ImageDto> images, HttpSession httpSession) {
        String sessionKey = httpSession.getId();

        Flux.fromIterable(images)
                .flatMap(image -> imageService.resizeAndSaveOriginalImage(image, sessionKey)
                        .doOnNext(imageService::addNewResponse)
                )
                .subscribe();

        return Mono.just(ResponseEntity.ok(sessionKey))
                .onErrorResume(e -> {
                    e.printStackTrace();
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing images"));
                });
    }



    @GetMapping(value = "/upload/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ResponseEntity<ImageDto>> streamUploadResponses(@RequestParam String sessionKey) {
        return imageService.getUploadResponses(sessionKey)
                .map(resizedImage -> new ImageDto(
                        resizedImage.getImageKey(),
                        resizedImage.getName(),
                        resizedImage.getBase64(),
                        resizedImage.getWidth(),
                        resizedImage.getHeight()
                ))
                .map(ResponseEntity::ok)
                .concatWith(Mono.just(ResponseEntity.ok(new ImageDto(COMPLETE_REQUEST, COMPLETE_REQUEST, COMPLETE_REQUEST, 0, 0))))
                .onErrorResume(e -> {
                    e.printStackTrace();
                    return Flux.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(new ImageDto(COMPLETE_REQUEST, COMPLETE_REQUEST, COMPLETE_REQUEST, 0, 0)));
                });
    }
}
