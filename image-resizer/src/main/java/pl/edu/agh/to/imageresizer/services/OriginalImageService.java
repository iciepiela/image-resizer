package pl.edu.agh.to.imageresizer.services;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import pl.edu.agh.to.imageresizer.model.OriginalImage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

@Service
@AllArgsConstructor
public class OriginalImageService {
    private final OriginalImageRepository originalImageRepository;

    public Flux<byte[]> getAllImages() {
        return originalImageRepository.findAll()
                .map(OriginalImage::getPath)
                .flatMap(path -> {
                    String fullPath = path.replaceFirst("^~", System.getProperty("user.home"));
                    Path filePath = Paths.get(fullPath);
                    return Mono.fromCallable(() -> Files.readAllBytes(filePath))
                            .onErrorResume(e -> {
                                System.err.println("Error reading file: " + filePath + ", " + e.getMessage());
                                return Mono.empty();
                            })
                            .repeat(3);
                })
                .delayElements(Duration.ofSeconds(1)) ;
    }
}
