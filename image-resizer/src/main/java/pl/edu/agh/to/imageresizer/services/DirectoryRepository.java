package pl.edu.agh.to.imageresizer.services;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import pl.edu.agh.to.imageresizer.model.Directory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DirectoryRepository extends ReactiveCrudRepository<Directory, Long> {
    @Query(
            "SELECT * " +
                    "from directories d " +
                    "join directories dp " +
                    "on d.directory_id=dp.parent_directory_id " +
                    "where dp.directory_key=:dirKey"
    )
    Mono<Directory> findDirectoryParent(String dirKey);
}
