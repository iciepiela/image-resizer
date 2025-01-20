package pl.edu.agh.to.imageresizer.services;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import pl.edu.agh.to.imageresizer.controllers.ImageController;
import pl.edu.agh.to.imageresizer.dto.DirectoryDto;
import pl.edu.agh.to.imageresizer.dto.DirectoryMetadata;
import pl.edu.agh.to.imageresizer.dto.ImageDto;
import pl.edu.agh.to.imageresizer.model.Directory;
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
    private final DirectoryRepository directoryRepository;
    private final DirectoryMetadataRepository directoryMetadataRepository;
    private final ImageResizer imageResizer;
    private final Logger logger = LoggerFactory.getLogger(ImageController.class);


    public ImageService(OriginalImageRepository originalImageRepository, ResizedImageRepository resizedImageRepository, DirectoryRepository directoryRepository, DirectoryMetadataRepository directoryMetadataRepository, ImageResizer imageResizer) {
        this.originalImageRepository = originalImageRepository;
        this.resizedImageRepository = resizedImageRepository;
        this.directoryRepository = directoryRepository;
        this.directoryMetadataRepository = directoryMetadataRepository;
        this.imageResizer = imageResizer;
    }

    @PostConstruct
    public void reprocessAllImages() {
        directoryRepository.findByDirectoryKey("root")
                .hasElement()
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.empty();
                    } else {
                        Directory rootDirectory = new Directory();
                        rootDirectory.setDirectoryKey("root");
                        rootDirectory.setName("root");
                        rootDirectory.setParentDirectoryId(null);
                        return directoryRepository.save(rootDirectory).then();
                    }
                })
                .subscribe();
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
                .flatMap(originalImage -> resizeImage(originalImage, imageSize))
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

    public Flux<ResizedImage> getAllResizedImages(ImageSize imageSize, int page) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        return Flux.concat(
                resizedImageRepository.findResizedImagesByWidthAndHeight(imageSize.getWidth(), imageSize.getHeight(), pageable),
                resizedImageRepository.findResizedImagesByWidthAndHeight(ERROR_WIDTH_AND_HEIGHT, ERROR_WIDTH_AND_HEIGHT, pageable)
        );

    }

    public Flux<ResizedImage> getResizedImagesForSessionKey(String sessionKey, ImageSize imageSize, int page) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        return Flux.just(sessionKey)
                .flatMap(key ->
                        Flux.merge(
                                resizedImageRepository.findResizedImagesBySessionKeyAndWidthAndHeight(key,
                                        imageSize.getWidth(), imageSize.getHeight(), pageable),
                                resizedImageRepository.findResizedImagesBySessionKeyAndWidthAndHeight(key,
                                        ERROR_WIDTH_AND_HEIGHT, ERROR_WIDTH_AND_HEIGHT, pageable)));

    }

    public Mono<DirectoryMetadata> getDirectoryParent(String dirKey) {
        return Mono.just(dirKey)
                .flatMap(key -> directoryMetadataRepository.findDirectoryParent(dirKey));
    }

    public Mono<DirectoryMetadata> getRoot() {
        return directoryMetadataRepository.findByDirKey("root");
    }

    public Flux<DirectoryMetadata> getDirectories(String dirKey, int page) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        return Flux.just(dirKey)
                .flatMap(key -> directoryMetadataRepository.findAllByParentDirectoryKey(dirKey, pageable.getPageSize(), pageable.getOffset()));
    }

    public Flux<DirectoryMetadata> getDirectories(String dirKey) {
        return Flux.just(dirKey)
                .flatMap(key -> directoryMetadataRepository.findAllByParentDirectoryKey(dirKey));
    }
    public Mono<Void> deleteDirectory(String dirKey) {
        if (dirKey.equals("root")) {
            return Mono.error(new RuntimeException("Cannot delete root directory"));
        }
        return directoryRepository.findByDirectoryKey(dirKey)
                .flatMap(directory -> {
                    if (directory.getParentDirectoryId() != null) {
                        return updateParentSubDirectoryCount(directory.getParentDirectoryId(), -1)
                                .then(deleteDirectoryAndChildren(directory));
                    } else {
                        return deleteDirectoryAndChildren(directory);
                    }
                });
    }

    private Mono<Void> updateParentSubDirectoryCount(Long parentDirectoryId, int change) {
        return directoryRepository.findById(parentDirectoryId)
                .flatMap(parentDirectory -> {
                    parentDirectory.setSubDirectoriesCount(parentDirectory.getSubDirectoriesCount() + change);
                    return directoryRepository.save(parentDirectory);
                })
                .then();
    }

    private Mono<Void> deleteDirectoryAndChildren(Directory directory) {
        return directoryRepository.delete(directory).then();
    }

    public Mono<Void> deleteImage(String imageKey) {
        return originalImageRepository.findByImageKey(imageKey)
                .flatMap(this::deleteImageRecord);
    }

    private Mono<Void> updateParentImageCount(Long directoryId, int change) {
        return directoryRepository.findById(directoryId)
                .flatMap(directory -> {
                    directory.setImageCount(directory.getImageCount() + change);
                    return directoryRepository.save(directory);
                })
                .then();
    }

    private Mono<Void> deleteImageRecord(OriginalImage image) {
        return originalImageRepository.delete(image).then();
    }
//    public Mono<Void> deleteDirectory(String dirKey) {
//        if (dirKey.equals("root")) {
//            return Mono.error(new RuntimeException("Cannot delete root directory"));
//        }
//        return directoryRepository.findByDirectoryKey(dirKey)
//                .flatMap(directory -> {
//
//                    Long directoryId = directory.getParentDirectoryId(); // Get the directory this image belongs to
//                    if (directoryId != null) {
//                        return directoryRepository.findById(directoryId)
//                                .flatMap(parentDirectory -> {
//                                    parentDirectory.setSubDirectoriesCount(parentDirectory.getSubDirectoriesCount() - 1);
//                                    return directoryRepository.save(parentDirectory)
//                                            .then(directoryRepository.delete(directory)) // Delete the image
//                                            .then();
//                                });
//                    } else {
//                        return directoryRepository.delete(directory).then();
//                    }
//                });
//    }
//
//    public Mono<Void> deleteImage(String imageKey) {
//        return originalImageRepository.findByImageKey(imageKey)
//                .flatMap(image -> {
//                    Long directoryId = image.getParentDirectoryId();
//                    if (directoryId != null) {
//                        return directoryRepository.findById(directoryId)
//                                .flatMap(directory -> {
//                                    directory.setImageCount(directory.getImageCount() - 1);
//                                    return directoryRepository.save(directory)
//                                            .then(originalImageRepository.delete(image))
//                                            .then();
//                                });
//                    } else {
//                        return originalImageRepository.delete(image).then();
//                    }
//                });
//    }

    public Flux<ResizedImage> getResizedImagesByDirKey(String dirKey, ImageSize imageSize, int page) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        return Flux.just(dirKey)
                .flatMap(key ->
                        Flux.merge(
                                resizedImageRepository.findResizedImagesByDir(key,
                                        imageSize.getWidth(), imageSize.getHeight(), pageable.getPageSize(), pageable.getOffset()),
                                resizedImageRepository.findResizedImagesByDir(key,
                                        ERROR_WIDTH_AND_HEIGHT, ERROR_WIDTH_AND_HEIGHT, pageable.getPageSize(), pageable.getOffset())));

    }

    public Flux<ResizedImage> getResizedImagesByImageKey(String imageKey, ImageSize imageSize, int page) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        return Flux.just(imageKey)
                .flatMap(key ->
                        Flux.merge(
                                resizedImageRepository
                                        .findResizedImagesByImageKeyAndWidthAndHeight(key, imageSize.getWidth(), imageSize.getHeight(), pageable),
                                resizedImageRepository
                                        .findResizedImagesByImageKeyAndWidthAndHeight(key, ERROR_WIDTH_AND_HEIGHT, ERROR_WIDTH_AND_HEIGHT, pageable)
                        )
                );


    }


    public Mono<OriginalImage> getOriginalImage(String key) {
        return resizedImageRepository.findResizedImageByImageKey(key)
                .next()
                .flatMap(image -> originalImageRepository.findById(image.getOriginalImageId()))
                .delayElement(Duration.ofMillis(500));
    }

//    public Mono<Boolean> saveDirectory(DirectoryDto directoryDto, String sessionKey, String directoryKey) {
//        return directoryRepository.findByDirectoryKey(directoryKey)
//                .flatMap(directory -> saveDirectory(directoryDto, sessionKey, directory.getDirectoryId()));
//    }
//
//    public Mono<Boolean> saveDirectory(DirectoryDto directoryDto, String sessionKey, Long parentDirectoryId) {
//        Directory directory = new Directory(
//                directoryDto.name(),
//                parentDirectoryId,
//                directoryDto.dirKey(),
//                directoryDto.subDirectoriesCount(),
//                directoryDto.imageCount()
//        );
//
//        return directoryRepository.save(directory)
//                .flatMap(savedDirectory ->
//                        directoryRepository.findById(parentDirectoryId)
//                                .flatMap(parentDirectory -> {
//                                    parentDirectory.setSubDirectoriesCount(parentDirectory.getSubDirectoriesCount() + 1);
//                                    return directoryRepository.save(parentDirectory);
//                                })
//                                .then(Flux.concat(
//                                                        Flux.fromIterable(directoryDto.directories())
//                                                                .flatMap(dir -> saveDirectory(dir, sessionKey, savedDirectory.getDirectoryId())),
//                                                        Flux.fromIterable(directoryDto.images())
//                                                                .flatMap(imageDto -> resizeAndSaveOriginalImage(imageDto, sessionKey, savedDirectory.getDirectoryId()))
//                                                )
//                                                .then(Mono.just(true))
//                                )
//                );
//
//    }

    public Mono<Boolean> saveOrUpdateDirectory(DirectoryDto directoryDto, String sessionKey, String directoryKey) {
        return directoryRepository.findByDirectoryKey(directoryKey)
                .flatMap(existingDirectory -> updateDirectory(existingDirectory, directoryDto, sessionKey))
                .switchIfEmpty(createAndSaveDirectory(directoryDto, sessionKey, null));
    }

    private Mono<Boolean> updateDirectory(Directory existingDirectory, DirectoryDto directoryDto, String sessionKey) {
        existingDirectory.setName(directoryDto.name());
        existingDirectory.setSubDirectoriesCount(directoryDto.subDirectoriesCount());
        existingDirectory.setImageCount(directoryDto.imageCount());

        return directoryRepository.save(existingDirectory)
                .then(clearAndRecreateChildren(existingDirectory.getDirectoryId(), directoryDto, sessionKey));
    }

    private Mono<Boolean> createAndSaveDirectory(DirectoryDto directoryDto, String sessionKey, Long parentDirectoryId) {
        Directory directory = new Directory(
                directoryDto.name(),
                parentDirectoryId,
                directoryDto.dirKey(),
                directoryDto.subDirectoriesCount(),
                directoryDto.imageCount()
        );

        return directoryRepository.save(directory)
                .flatMap(savedDirectory -> {
                    if (parentDirectoryId != null) {
                        return updateParentSubDirectoryCount(parentDirectoryId)
                                .then(saveChildrenDirectoriesAndImages(directoryDto, sessionKey, savedDirectory.getDirectoryId()));
                    } else {
                        return saveChildrenDirectoriesAndImages(directoryDto, sessionKey, savedDirectory.getDirectoryId());
                    }
                });
    }

    private Mono<Void> updateParentSubDirectoryCount(Long parentDirectoryId) {
        return directoryRepository.findById(parentDirectoryId)
                .flatMap(parentDirectory -> {
                    parentDirectory.setSubDirectoriesCount(parentDirectory.getSubDirectoriesCount() + 1);
                    return directoryRepository.save(parentDirectory);
                })
                .then();
    }

    private Mono<Boolean> saveChildrenDirectoriesAndImages(DirectoryDto directoryDto, String sessionKey, Long parentDirectoryId) {
        return Flux.concat(
                        Flux.fromIterable(directoryDto.directories())
                                .flatMap(childDirectory -> createAndSaveDirectory(childDirectory, sessionKey, parentDirectoryId)),
                        Flux.fromIterable(directoryDto.images())
                                .flatMap(imageDto -> resizeAndSaveOriginalImage(imageDto, sessionKey, parentDirectoryId))
                )
                .then(Mono.just(true));
    }

    private Mono<Boolean> clearAndRecreateChildren(Long parentDirectoryId, DirectoryDto directoryDto, String sessionKey) {
        return directoryRepository.deleteAllByParentDirectoryId(parentDirectoryId)
                .then(saveChildrenDirectoriesAndImages(directoryDto, sessionKey, parentDirectoryId));
    }


    public Mono<Boolean> resizeAndSaveOriginalImage(ImageDto imageDto, String sessionKey, Long parentDirectoryId) {
        return getImageDimensions(imageDto.base64())
                .flatMap(dimensions -> saveOriginalImage(imageDto, dimensions, sessionKey, parentDirectoryId))
                .flatMap(savedOriginalImage -> resizeImage(imageDto, sessionKey, savedOriginalImage)
                        .all(result -> result))
                .onErrorResume(e -> saveErrorOriginalImage(imageDto, sessionKey, parentDirectoryId)
                        .flatMap(savedImage -> saveErrorResizedImage(imageDto, sessionKey, savedImage))
                        .then(Mono.just(false)))
                .flatMap(savedImage -> directoryRepository.findById(parentDirectoryId)
                        .flatMap(directory -> {
                            directory.setImageCount(directory.getImageCount() + 1);
                            return directoryRepository.save(directory);
                        }).then(Mono.just(true)));
    }

    private Mono<OriginalImage> saveOriginalImage(ImageDto imageDto, Pair<Integer, Integer> dimensions, String sessionKey, Long parentDirectoryId) {
        OriginalImage originalImage = new OriginalImage(imageDto.name(),
                imageDto.base64(),
                sessionKey,
                imageDto.imageKey(),
                dimensions.getFirst(),
                dimensions.getSecond(),
                parentDirectoryId);

        return originalImageRepository.save(originalImage)
                .onErrorResume(e -> {
                    OriginalImage errorImage = new OriginalImage(imageDto.name(), ERROR,
                            sessionKey,
                            imageDto.imageKey(),
                            ERROR_WIDTH_AND_HEIGHT, ERROR_WIDTH_AND_HEIGHT,
                            parentDirectoryId);
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

    private Mono<OriginalImage> saveErrorOriginalImage(ImageDto imageDto, String sessionKey, Long parentDirectoryId) {
        OriginalImage originalImage = new OriginalImage(
                imageDto.name(),
                ERROR,
                sessionKey,
                imageDto.imageKey(),
                ERROR_WIDTH_AND_HEIGHT,
                ERROR_WIDTH_AND_HEIGHT,
                parentDirectoryId
        );
        return originalImageRepository.save(originalImage);
    }
}
