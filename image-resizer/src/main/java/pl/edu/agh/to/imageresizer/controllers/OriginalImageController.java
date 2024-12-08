package pl.edu.agh.to.imageresizer.controllers;

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
public class OriginalImageController {
    private final ImageService imageService;
    private final String COMPLETE_REQUEST = "COMPLETE_REQUEST";

    @GetMapping(value = "/original", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ResponseEntity<ImageDto>> getImages() {
        return imageService.getAllResizedImages()
                .concatWith(Flux.just(new ImageDto(COMPLETE_REQUEST,COMPLETE_REQUEST, COMPLETE_REQUEST)))
                .doOnNext(elem -> log.info("Image emitted"))
                .map(element -> ResponseEntity.ok().body(element));
    }

    @PostMapping(value = "/upload", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Boolean>> uploadImages(@RequestBody List<ImageDto> images) {
        return Flux.fromIterable(images)
                .flatMap(imageService::resizeAndSaveOriginalImage)
                .then(Mono.just(ResponseEntity.ok(true)))
                .onErrorResume(e -> {
                    e.printStackTrace();
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(false));
                });

    }
}
