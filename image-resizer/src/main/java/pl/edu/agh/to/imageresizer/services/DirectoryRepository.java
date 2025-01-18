package pl.edu.agh.to.imageresizer.services;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import pl.edu.agh.to.imageresizer.model.Directory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DirectoryRepository extends ReactiveCrudRepository<Directory, Long> {


    Flux<Directory> findDirectoriesByParentKey(String directoryKey);


    Mono<Directory> findByDirectoryKey(String directoryKey);

    Flux<Directory> findDirectoriesByParentKeyAndSessionKey(String s, String sessionKey);
}
