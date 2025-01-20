package pl.edu.agh.to.imageresizer.services;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import pl.edu.agh.to.imageresizer.model.OriginalImage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OriginalImageRepository extends ReactiveCrudRepository<OriginalImage, Long> {

    @Query("SELECT o.* FROM original_images o " +
            "LEFT JOIN resized_images r " +
            "ON o.image_id = r.original_image " +
            "AND r.width = :width " +
            "AND r.height = :height " +
            "WHERE r.image_id IS NULL")
    Flux<OriginalImage> findOriginalImagesWithoutResizedImageOfSize(int width, int height, Pageable pageable);
    Mono<OriginalImage> findByImageKey(String imageKey);

    Mono<Void> deleteAllByParentDirectoryId(Long parentDirectoryId);
}
