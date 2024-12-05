package pl.edu.agh.to.imageresizer.services;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import pl.edu.agh.to.imageresizer.model.ResizedImage;

public interface ResizedImageRepository extends ReactiveCrudRepository<ResizedImage, Long> {

}
