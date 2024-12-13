package pl.edu.agh.to.imageresizer.services;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import pl.edu.agh.to.imageresizer.model.OriginalImage;

public interface OriginalImageRepository extends ReactiveCrudRepository<OriginalImage, Long> {

}
