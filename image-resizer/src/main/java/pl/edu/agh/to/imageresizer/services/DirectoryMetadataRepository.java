package pl.edu.agh.to.imageresizer.services;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import pl.edu.agh.to.imageresizer.dto.DirectoryMetadata;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DirectoryMetadataRepository extends ReactiveCrudRepository<DirectoryMetadata, Long> {

    @Query(
            "SELECT * " +
                    "from directories d " +
                    "join directories dp " +
                    "on d.directory_id=dp.parent_directory_id " +
                    "where dp.directory_key=:dirKey"
    )
    Mono<DirectoryMetadata> findDirectoryParent(String dirKey);

    @Query(
            "SELECT * " +
                    "from directories d " +
                    "where d.parent_directory_id=(select dp.directory_id " +
                    "from directories dp " +
                    "where dp.directory_key=:parentDirectoryKey)"
    )
   Flux<DirectoryMetadata> findAllByParentDirectoryKey(String parentDirectoryKey);
    @Query(
            "SELECT * " +
                    "from directories d " +
                    "where d.directory_key=:dirKey"
    )
    Mono<DirectoryMetadata> findByDirKey(String dirKey);
}

