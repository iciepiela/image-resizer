package pl.edu.agh.to.imageresizer.services;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import pl.edu.agh.to.imageresizer.model.Directory;
import reactor.core.publisher.Mono;

public interface DirectoryRepository extends ReactiveCrudRepository<Directory, Long> {
    Mono<Directory> findByDirectoryKey(String key);
}
