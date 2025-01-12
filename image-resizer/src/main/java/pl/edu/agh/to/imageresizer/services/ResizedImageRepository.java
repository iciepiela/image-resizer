package pl.edu.agh.to.imageresizer.services;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import pl.edu.agh.to.imageresizer.model.ResizedImage;
import reactor.core.publisher.Flux;

public interface ResizedImageRepository extends ReactiveCrudRepository<ResizedImage, Long> {
    Flux<ResizedImage> findResizedImagesBySessionKeyAndWidthAndHeight(String key, int width, int height);

    Flux<ResizedImage> findResizedImagesByImageKeyAndWidthAndHeight(String key, int width, int height);

    Flux<ResizedImage> findResizedImageByImageKey(String imageKey);

    Flux<ResizedImage> findResizedImagesByWidthAndHeight(int width, int height);

    @Query(
            "SELECT * " +
                    "FROM resized_images r " +
                    "JOIN original_images o ON o.image_id=r.original_image " +
                    "JOIN directories d ON d.directory_id=o.parent_directory_id " +
                    "WHERE d.directory_key=:key " +
                    "AND r.height=:height AND r.width=:width"
    )
    Flux<ResizedImage> findResizedImagesByDir(String key, int width, int height);

}
