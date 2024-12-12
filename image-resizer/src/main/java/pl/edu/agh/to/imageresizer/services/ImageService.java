package pl.edu.agh.to.imageresizer.services;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import pl.edu.agh.to.imageresizer.model.ImageDto;
import pl.edu.agh.to.imageresizer.model.OriginalImage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@AllArgsConstructor
public class ImageService {
    private final OriginalImageRepository originalImageRepository;
    private final ResizedImageRepository resizedImageRepository;
    private final ImageResizer imageResizer;

    public Flux<ImageDto> getAllResizedImages() {
        return resizedImageRepository.findAll()
                .map(el -> new ImageDto(el.getImageKey(), el.getName(), el.getBase64()))
                .delayElements(Duration.ofSeconds(1));
    }

    public Flux<ImageDto> getResizedImagesForSessionKey(String sessionKey) {
        return Flux.just(sessionKey)
                .flatMap(resizedImageRepository::findResizedImagesBySessionKey)
                .map(el -> new ImageDto(el.getImageKey(), el.getName(), el.getBase64()))
                .delayElements(Duration.ofSeconds(1));
    }

    public Mono<Boolean> resizeAndSaveOriginalImage(ImageDto imageDto, String sessionKey) {
        return imageResizer.resize(imageDto, sessionKey)
                .flatMap(resizedImage ->
                        originalImageRepository.save(new OriginalImage(imageDto.getName(), imageDto.getBase64()))
                                .flatMap(savedOriginalImage -> {
                                    resizedImage.setOriginalImageId(savedOriginalImage.getImageId());
                                    return resizedImageRepository.save(resizedImage)
                                            .then(Mono.just(true));
                                })
                )
                .onErrorResume(e -> {
                    e.printStackTrace();
                    return Mono.just(false);
                });
    }

}
