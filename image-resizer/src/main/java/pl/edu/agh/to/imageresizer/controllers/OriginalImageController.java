    package pl.edu.agh.to.imageresizer.controllers;

    import lombok.AllArgsConstructor;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.http.MediaType;
    import org.springframework.http.ResponseEntity;
    import org.springframework.web.bind.annotation.GetMapping;
    import org.springframework.web.bind.annotation.RequestMapping;
    import org.springframework.web.bind.annotation.RestController;
    import pl.edu.agh.to.imageresizer.services.OriginalImageService;
    import reactor.core.publisher.Flux;

    @RestController
    @RequestMapping("/images")
    @AllArgsConstructor
    @Slf4j
    public class OriginalImageController {
        private final OriginalImageService originalImageService;

        @GetMapping(value = "/original", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
        public Flux<ResponseEntity<byte []>> getImages() {
            return originalImageService.getAllImages()
                    .doOnNext(elem->log.info("Image emitted"))
                    .map(element -> ResponseEntity.ok().body(element));
        }

    }
