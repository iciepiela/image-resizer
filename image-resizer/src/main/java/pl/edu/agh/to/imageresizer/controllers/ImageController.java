package pl.edu.agh.to.imageresizer.controllers;

import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.edu.agh.to.imageresizer.dto.DirectoryDto;
import pl.edu.agh.to.imageresizer.dto.ImageDto;
import pl.edu.agh.to.imageresizer.model.ImageSize;
import pl.edu.agh.to.imageresizer.model.ResizedImage;
import pl.edu.agh.to.imageresizer.services.ImageService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

import static pl.edu.agh.to.imageresizer.services.ImageService.ERROR;

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
                .map(this::getImageDtoResponseEntity);
    }

    @GetMapping(value = "/resized/by-session", params = {"sessionKey", "sizeString"}, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ResponseEntity<ImageDto>> getImagesBySessionKey(@RequestParam String sessionKey, @RequestParam String sizeString) {
        return imageService.getResizedImagesForSessionKey(sessionKey, ImageSize.valueOf(sizeString.toUpperCase()))
                .map(this::convertToImageDto)
                .doOnNext(image -> logger.info(image.toString()))
                .concatWith(Flux.just(new ImageDto(COMPLETE_REQUEST, COMPLETE_REQUEST, COMPLETE_REQUEST, 0, 0)))
                .map(this::getImageDtoResponseEntity);
    }

    @GetMapping(value = "/resized/by-directory",params={"sizeString","directoryKey"}, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ResponseEntity<ImageDto>> getImagesBySessionKeyAndDirectoryKey(@RequestParam String sizeString, @RequestParam String directoryKey) {
        return imageService.getResizedImageByDirectoryKeyAndSize(directoryKey, ImageSize.valueOf(sizeString.toUpperCase()))
                .map(this::convertToImageDto)
                .doOnNext(image -> logger.info(image.toString()))
                .concatWith(Flux.just(new ImageDto(COMPLETE_REQUEST, COMPLETE_REQUEST, COMPLETE_REQUEST, 0, 0)))
                .map(this::getImageDtoResponseEntity);
    }
    @GetMapping(value = "/resized/by-directory-all",params={"sizeString","directoryKey"}, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ResponseEntity<ImageDto>> getAllImagesByDirectoryKey(@RequestParam String sizeString, @RequestParam String directoryKey) {
        return imageService.getAllResizedImagesForDirectoryKey(directoryKey, ImageSize.valueOf(sizeString.toUpperCase()))
                .map(this::convertToImageDto)
                .doOnNext(image -> logger.info(image.toString()))
                .concatWith(Flux.just(new ImageDto(COMPLETE_REQUEST, COMPLETE_REQUEST, COMPLETE_REQUEST, 0, 0)))
                .map(this::getImageDtoResponseEntity);
    }
    @GetMapping(value = "/directories/parent", params = {"directoryKey"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<DirectoryDto>> getParentDirectory(@RequestParam String directoryKey) {
        return imageService.getParentDirectory(directoryKey)
                .map(directory -> new DirectoryDto(
                        directory.getName(),
                        directory.getDirectoryKey(),
                        directory.getParentKey(),
                        directory.getImageCount(),
                        directory.getSubDirectoriesCount()
                ))
                .map(this::getDirectoryDtoResponseEntity);
    }
    @GetMapping(value = "/directories/by-parent-key", params = {"directoryKey"}, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ResponseEntity<DirectoryDto>> getDirectoriesByParentKey(@RequestParam String directoryKey) {
        return imageService.getDirectoriesByParentKey(directoryKey)
                .map(directory -> new DirectoryDto(
                        directory.getName(),
                        directory.getDirectoryKey(),
                        directory.getParentKey(),
                        directory.getImageCount(),
                        directory.getSubDirectoriesCount()
                ))
                .map(ResponseEntity::ok);
    }
    @PostMapping(value = "/directories/upload", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<String>> uploadDirectory(@RequestBody List<DirectoryDto> directories, HttpSession httpSession) {
        logger.info("DDDirectories: {}", directories);
        String sessionKey = httpSession.getId();
        Flux.fromIterable(directories)
                .flatMap(directory -> imageService.createDirectory(directory, sessionKey))
                .doOnError(e -> logger.error("Error during directory creation", e))
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
        return Mono.just(ResponseEntity.ok().body(sessionKey));
    }
    @GetMapping(value = "/directories/first-all" , produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<ResponseEntity<DirectoryDto>> getFirstDirectories() {
        return imageService.getFirstDirectories()
                .map(directory -> new DirectoryDto(
                        directory.getName(),
                        directory.getDirectoryKey(),
                        directory.getParentKey(),
                        directory.getImageCount(),
                        directory.getSubDirectoriesCount()
                ))
                .map(ResponseEntity::ok);
    }

    @GetMapping(value = "/directories/first-by-session", params = {"sessionKey"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<ResponseEntity<DirectoryDto>> getFirstDirectoriesBySessionKey(@RequestParam String sessionKey) {
        return imageService.getFirstDirectoriesBySessionKey(sessionKey)
                .map(directory -> new DirectoryDto(
                        directory.getName(),
                        directory.getDirectoryKey(),
                        directory.getParentKey(),
                        directory.getImageCount(),
                        directory.getSubDirectoriesCount()
                ))
                .map(ResponseEntity::ok);
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
                .map(this::getImageDtoResponseEntity);
    }

    @GetMapping(value = "/resized/by-image-key", params = {"imageKey", "sizeString"}, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ResponseEntity<ImageDto>> getImageByImageKey(@RequestParam String imageKey, @RequestParam String sizeString) {
        return imageService.getResizedImagesByImageKey(imageKey, ImageSize.valueOf(sizeString.toUpperCase()))
                .map(this::convertToImageDto)
                .doOnNext(image -> logger.info(image.toString()))
                .concatWith(Flux.just(new ImageDto(COMPLETE_REQUEST, COMPLETE_REQUEST, COMPLETE_REQUEST, 0, 0))
                        .delayElements(java.time.Duration.ofMillis(500)))
                .map(this::getImageDtoResponseEntity);
    }

    @PostMapping(value = "/upload",params={"directory"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<String>> uploadImages(@RequestBody List<ImageDto> images,@RequestParam String directoryKey, HttpSession httpSession) {
        String sessionKey = httpSession.getId();
        Flux.fromIterable(images)
                .flatMap(image -> imageService.resizeAndSaveOriginalImage(image, sessionKey,directoryKey))
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

    private ResponseEntity<ImageDto> getImageDtoResponseEntity(ImageDto element) {
        return element.base64().equals(ERROR) ?
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(element)
                : ResponseEntity.ok().body(element);
    }

    private ResponseEntity<DirectoryDto> getDirectoryDtoResponseEntity(DirectoryDto element) {
        return ResponseEntity.ok().body(element);
    }

}
