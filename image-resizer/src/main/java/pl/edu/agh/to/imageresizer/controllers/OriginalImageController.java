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

    @GetMapping(value = "/original", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ResponseEntity<byte[]>> getImages() {
        return originalImageService.getAllImages()
                .doOnNext(elem -> log.info("Image emitted"))
                .map(element -> ResponseEntity.ok().body(element));
    }

    @PostMapping(value = "/upload", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ResponseEntity<ImageDto>> uploadImages(@RequestBody List<ImageDto> body) {
        log.info(body.toString());
        return Flux.fromIterable(body)
                .map(imageData -> ResponseEntity.ok()
                        .body(imageData))
                .delayElements(Duration.ofSeconds(1));
    }

}
