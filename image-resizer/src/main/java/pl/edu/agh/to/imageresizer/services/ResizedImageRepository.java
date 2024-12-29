package pl.edu.agh.to.imageresizer.services;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import pl.edu.agh.to.imageresizer.model.ResizedImage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ResizedImageRepository extends ReactiveCrudRepository<ResizedImage, Long> {
    Flux<ResizedImage> findResizedImagesBySessionKeyAndWidthAndHeight(String key, String width, String height);

    Flux<ResizedImage> findResizedImageByImageKey(String imageKey);
}
