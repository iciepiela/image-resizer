package pl.edu.agh.to.imageresizer.controllers;

import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.edu.agh.to.imageresizer.model.ImageDto;
import pl.edu.agh.to.imageresizer.services.ImageService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/images")
@AllArgsConstructor
@Slf4j
public class ImageController {
    private final ImageService imageService;
    private final String COMPLETE_REQUEST = "COMPLETE_REQUEST";

    @GetMapping(value = "/resized", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ResponseEntity<ImageDto>> getImagesBySessionKey(@RequestParam String sessionKey) {
        return imageService.getResizedImagesForSessionKey(sessionKey)
                .concatWith(Flux.just(new ImageDto(COMPLETE_REQUEST, COMPLETE_REQUEST, COMPLETE_REQUEST)))
                .map(element -> ResponseEntity.ok().body(element));
    }

    @GetMapping(value = "/resized/all", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ResponseEntity<ImageDto>> getAllImages() {
        return imageService.getAllResizedImages()
                .concatWith(Flux.just(new ImageDto(COMPLETE_REQUEST, COMPLETE_REQUEST, COMPLETE_REQUEST)))
                .map(element -> ResponseEntity.ok().body(element));
    }

    @PostMapping(value = "/upload", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<String>> uploadImages(@RequestBody List<ImageDto> images, HttpSession httpSession) {
        String sessionKey = httpSession.getId();
        return Flux.fromIterable(images)
                .flatMap(image -> imageService.resizeAndSaveOriginalImage(image, sessionKey))
                .then(Mono.just(ResponseEntity.ok(sessionKey)))
                .onErrorResume(e -> {
                    e.printStackTrace();
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null));
                });

    }
}
