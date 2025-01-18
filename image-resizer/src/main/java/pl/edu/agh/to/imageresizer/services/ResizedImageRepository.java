package pl.edu.agh.to.imageresizer.services;

import org.reactivestreams.Publisher;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import pl.edu.agh.to.imageresizer.model.ResizedImage;
import reactor.core.publisher.Flux;

public interface ResizedImageRepository extends ReactiveCrudRepository<ResizedImage, Long> {
    Flux<ResizedImage> findResizedImagesBySessionKeyAndWidthAndHeight(String key, int width, int height);

    Flux<ResizedImage> findResizedImagesByImageKeyAndWidthAndHeight(String key, int width, int height);

    Flux<ResizedImage> findResizedImageByImageKey(String imageKey);

    Flux<ResizedImage> findResizedImagesByWidthAndHeight(int width, int height);

    Flux<ResizedImage> findResizedImagesByOriginalImageIdAndWidthAndHeight(Long originalImageId, Integer width, Integer height);

    Flux<ResizedImage> findResizedImagesBySessionKeyAndDirectoryKeyAndWidthAndHeight(String key, String directoryKey, int width, int height);

    Flux<ResizedImage> findResizedImagesByDirectoryKeyAndWidthAndHeight(String directoryKey, int width, int height);
}
