package pl.edu.agh.to.imageresizer.services;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import pl.edu.agh.to.imageresizer.model.ResizedImage;
import reactor.core.publisher.Flux;

public interface ResizedImageRepository extends ReactiveCrudRepository<ResizedImage, Long> {
    Flux<ResizedImage> findResizedImagesBySessionKey(String key);
}
