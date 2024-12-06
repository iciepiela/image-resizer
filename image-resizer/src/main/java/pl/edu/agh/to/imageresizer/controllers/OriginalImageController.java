package pl.edu.agh.to.imageresizer.controllers;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.edu.agh.to.imageresizer.model.ImageDto;
import pl.edu.agh.to.imageresizer.services.OriginalImageService;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/images")
@AllArgsConstructor
@Slf4j
public class OriginalImageController {
    private final OriginalImageService originalImageService;
    private final String COMPLETE_REQUEST="COMPLETE_REQUEST";

    @GetMapping(value = "/original", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ResponseEntity<ImageDto>> getImages() {
        return originalImageService.getAllImages()
                .concatWith(Flux.just(new ImageDto(COMPLETE_REQUEST, COMPLETE_REQUEST)))
                .doOnNext(elem -> log.info("Image emitted"))
                .map(element -> ResponseEntity.ok().body(element));
    }

    @PostMapping(value = "/upload", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> uploadImages(@RequestBody List<ImageDto> images) {
        // TODO: process images from body and return true when they're ready
        return ResponseEntity.ok().body(Boolean.TRUE);
    }

}
