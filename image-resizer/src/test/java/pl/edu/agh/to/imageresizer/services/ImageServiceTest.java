package pl.edu.agh.to.imageresizer.services;


import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import pl.edu.agh.to.imageresizer.dto.ImageDto;
import pl.edu.agh.to.imageresizer.model.ImageSize;
import pl.edu.agh.to.imageresizer.model.OriginalImage;
import pl.edu.agh.to.imageresizer.model.ResizedImage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

public class ImageServiceTest {

    private final String IMAGE_KEY = "imageKey";
    private final String SESSION_KEY = "sessionKey";
    private final String ORIGINAL_BASE64 = "src/test/resources/services/originalBase64.txt";
    private final String NAME = "name";


    @Test
    public void returnAllResizedImages() {
        // given
        List<ResizedImage> resizedImages = List.of(
                new ResizedImage("key", "name", "base64", "sessionKey", 200, 200),
                new ResizedImage("key2", "name2", "base64_2", "sessionKey_2", 200, 200),
                new ResizedImage("key3", "name3", "base64_3", "sessionKey_3", 200, 200));

        List<ResizedImage> damagedImages = List.of(
                new ResizedImage("key4", "name", "ERROR", "sessionKey", 0, 0),
                new ResizedImage("key5", "name2", "ERROR", "sessionKey_2", 0, 0));

        ResizedImageRepository resizedImageRepository = Mockito.mock(ResizedImageRepository.class);
        Mockito.when(resizedImageRepository.findResizedImagesByWidthAndHeight(200, 200))
                .thenReturn(Flux.fromIterable(resizedImages));
        Mockito.when(resizedImageRepository.findResizedImagesByWidthAndHeight(0, 0))
                .thenReturn(Flux.fromIterable(damagedImages));

        OriginalImageRepository originalImageRepository = Mockito.mock(OriginalImageRepository.class);
        ImageResizer imageResizer = Mockito.mock(ImageResizer.class);

        ImageService imageService = new ImageService(originalImageRepository, resizedImageRepository, imageResizer);

        // when and then
        StepVerifier.create(imageService.getAllResizedImages(ImageSize.MEDIUM))
                .expectNextMatches(image -> resizedImages.get(0).equals(image))
                .expectNextMatches(image -> resizedImages.get(1).equals(image))
                .expectNextMatches(image -> resizedImages.get(2).equals(image))
                .expectNextMatches(image -> damagedImages.get(0).equals(image))
                .expectNextMatches(image -> damagedImages.get(1).equals(image))
                .expectComplete()
                .verify();
    }

    @Test
    public void returnAllResizedImagesForSessionKey() {
        // given
        List<ResizedImage> resizedImages = List.of(
                new ResizedImage("key", "name", "base64", SESSION_KEY, 200, 200),
                new ResizedImage("key2", "name2", "base64_2", SESSION_KEY, 200, 200),
                new ResizedImage("key3", "name3", "base64_3", SESSION_KEY, 200, 200));
        List<ResizedImage> damagedImages = List.of(
                new ResizedImage("key4", "name", "ERROR", "sessionKey", 0, 0),
                new ResizedImage("key5", "name2", "ERROR", "sessionKey_2", 0, 0));


        ResizedImageRepository resizedImageRepository = Mockito.mock(ResizedImageRepository.class);

        Mockito.when(resizedImageRepository.findResizedImagesBySessionKeyAndWidthAndHeight(SESSION_KEY, 200, 200))
                .thenReturn(Flux.fromIterable(resizedImages));
        Mockito.when(resizedImageRepository.findResizedImagesBySessionKeyAndWidthAndHeight(SESSION_KEY, 0, 0))
                .thenReturn(Flux.fromIterable(damagedImages));

        OriginalImageRepository originalImageRepository = Mockito.mock(OriginalImageRepository.class);
        ImageResizer imageResizer = Mockito.mock(ImageResizer.class);

        ImageService imageService = new ImageService(originalImageRepository, resizedImageRepository, imageResizer);
        // when and then
        StepVerifier.create(imageService.getResizedImagesForSessionKey(SESSION_KEY, ImageSize.MEDIUM))
                .expectNextMatches(image -> resizedImages.get(0).equals(image))
                .expectNextMatches(image -> resizedImages.get(1).equals(image))
                .expectNextMatches(image -> resizedImages.get(2).equals(image))
                .expectNextMatches(image -> damagedImages.get(0).equals(image))
                .expectNextMatches(image -> damagedImages.get(1).equals(image))
                .expectComplete()
                .verify();
    }

    @Test
    public void returnAllResizedImagesByImageKey() {
        // given
        List<ResizedImage> resizedImages = List.of(
                new ResizedImage("key", "name", "base64", SESSION_KEY, 200, 200),
                new ResizedImage("key2", "name2", "base64_2", SESSION_KEY, 200, 200),
                new ResizedImage("key3", "name3", "base64_3", SESSION_KEY, 200, 200));
        List<ResizedImage> damagedImages = List.of(
                new ResizedImage("key4", "name", "ERROR", "sessionKey", 0, 0),
                new ResizedImage("key5", "name2", "ERROR", "sessionKey_2", 0, 0));


        ResizedImageRepository resizedImageRepository = Mockito.mock(ResizedImageRepository.class);

        Mockito.when(resizedImageRepository.findResizedImagesByImageKeyAndWidthAndHeight(SESSION_KEY, 200, 200))
                .thenReturn(Flux.fromIterable(resizedImages));
        Mockito.when(resizedImageRepository.findResizedImagesByImageKeyAndWidthAndHeight(SESSION_KEY, 0, 0))
                .thenReturn(Flux.fromIterable(damagedImages));

        OriginalImageRepository originalImageRepository = Mockito.mock(OriginalImageRepository.class);
        ImageResizer imageResizer = Mockito.mock(ImageResizer.class);

        ImageService imageService = new ImageService(originalImageRepository, resizedImageRepository, imageResizer);
        // when and then
        StepVerifier.create(imageService.getResizedImagesByImageKey(SESSION_KEY, ImageSize.MEDIUM))
                .expectNextMatches(image -> resizedImages.get(0).equals(image))
                .expectNextMatches(image -> resizedImages.get(1).equals(image))
                .expectNextMatches(image -> resizedImages.get(2).equals(image))
                .expectNextMatches(image -> damagedImages.get(0).equals(image))
                .expectNextMatches(image -> damagedImages.get(1).equals(image))
                .expectComplete()
                .verify();
    }

    @Test
    public void saveOriginalImageAndReturnTrueOnSuccess() {
        // given
        String base64 = Util.readFile(ORIGINAL_BASE64);
        ImageDto imageDto1 = new ImageDto(IMAGE_KEY, NAME, base64, 225, 225);
        OriginalImage originalImage1 = new OriginalImage(NAME, base64, SESSION_KEY, IMAGE_KEY, 255, 255);
        originalImage1.setImageId(1L);

        ResizedImage resizedImage1 = new ResizedImage(IMAGE_KEY, NAME, base64, SESSION_KEY, 200, 200);
        resizedImage1.setOriginalImageId(1L);

        ResizedImageRepository resizedImageRepository = Mockito.mock(ResizedImageRepository.class);
        OriginalImageRepository originalImageRepository = Mockito.mock(OriginalImageRepository.class);
        ImageResizer imageResizer = Mockito.mock(ImageResizer.class);

        Mockito.when(resizedImageRepository.save(Mockito.any(ResizedImage.class)))
                .thenReturn(Mono.just(resizedImage1));

        Mockito.when(originalImageRepository.save(Mockito.any(OriginalImage.class)))
                .thenReturn(Mono.just(originalImage1));

        Mockito.when(imageResizer.resize(Mockito.eq(imageDto1), Mockito.eq(SESSION_KEY)))
                .thenReturn(Flux.just(resizedImage1));

        ImageService imageService = new ImageService(originalImageRepository, resizedImageRepository, imageResizer);

        // when and then
        StepVerifier.create(imageService.resizeAndSaveOriginalImage(imageDto1, SESSION_KEY))
                .expectNext(true)
                .verifyComplete();

        Mockito.verify(originalImageRepository).save(Mockito.any(OriginalImage.class));
        Mockito.verifyNoMoreInteractions(originalImageRepository);

        Mockito.verify(resizedImageRepository).save(Mockito.any(ResizedImage.class));
        Mockito.verifyNoMoreInteractions(resizedImageRepository);
    }


    @Test
    public void saveOriginalImageAndReturnFalseOnError() {
        // given
        String invalidBase64 = "invalid_base64";
        ImageDto ErrorImageDto = new ImageDto(IMAGE_KEY, NAME, invalidBase64, 225, 225);
        OriginalImage errorOriginalImage = new OriginalImage(
                NAME,
                ImageService.ERROR,
                SESSION_KEY,
                IMAGE_KEY,
                ImageService.ERROR_WIDTH_AND_HEIGHT,
                ImageService.ERROR_WIDTH_AND_HEIGHT
        );

        ResizedImage ErrorResizedImage = new ResizedImage(
                IMAGE_KEY,
                NAME,
                ImageService.ERROR,
                SESSION_KEY,
                ImageService.ERROR_WIDTH_AND_HEIGHT,
                ImageService.ERROR_WIDTH_AND_HEIGHT
        );

        ResizedImageRepository resizedImageRepository = Mockito.mock(ResizedImageRepository.class);
        OriginalImageRepository originalImageRepository = Mockito.mock(OriginalImageRepository.class);
        ImageResizer imageResizer = Mockito.mock(ImageResizer.class);


        Mockito.when(originalImageRepository.save(Mockito.any(OriginalImage.class)))
                .thenReturn(Mono.just(errorOriginalImage));
        Mockito.when(resizedImageRepository.save(Mockito.any(ResizedImage.class)))
                .thenReturn(Mono.just(ErrorResizedImage));
        Mockito.when(imageResizer.resize(Mockito.any(), Mockito.any()))
                .thenReturn(Flux.error(new RuntimeException("Error")));

        ImageService imageService = new ImageService(originalImageRepository, resizedImageRepository, imageResizer);

        // when and then
        StepVerifier.create(imageService.resizeAndSaveOriginalImage(ErrorImageDto, SESSION_KEY))
                .expectNext(false)
                .verifyComplete();

        Mockito.verify(originalImageRepository).save(Mockito.argThat(image ->
                image.getBase64().equals(ImageService.ERROR) &&
                        image.getWidth() == ImageService.ERROR_WIDTH_AND_HEIGHT &&
                        image.getHeight() == ImageService.ERROR_WIDTH_AND_HEIGHT
        ));

        Mockito.verify(resizedImageRepository).save(Mockito.argThat(image ->
                image.getBase64().equals(ImageService.ERROR) &&
                        image.getWidth() == ImageService.ERROR_WIDTH_AND_HEIGHT &&
                        image.getHeight() == ImageService.ERROR_WIDTH_AND_HEIGHT
        ));

    }

    @Test
    public void returnOriginalImageForResizedImageKey() {
        // given
        String imageKey = "key1";

        ResizedImage resizedImage = new ResizedImage(
                "kay1",
                "name1",
                "base64",
                "sessionKey",
                200,
                200
        );

        OriginalImage originalImage = new OriginalImage(
                "name1",
                "base64_original",
                "sessionKey",
                imageKey,
                1024,
                768
        );

        ResizedImageRepository resizedImageRepository = Mockito.mock(ResizedImageRepository.class);
        OriginalImageRepository originalImageRepository = Mockito.mock(OriginalImageRepository.class);

        Mockito.when(resizedImageRepository.findResizedImageByImageKey(imageKey))
                .thenReturn(Flux.just(resizedImage));
        Mockito.when(originalImageRepository.findById(originalImage.getImageId()))
                .thenReturn(Mono.just(originalImage));

        ImageService imageService = new ImageService(originalImageRepository, resizedImageRepository, null);

        // when and then
        StepVerifier.create(imageService.getOriginalImage(imageKey))
                .expectNextMatches(image ->
                        image.getName().equals(originalImage.getName()) &&
                                image.getBase64().equals(originalImage.getBase64()) &&
                                image.getImageKey().equals(originalImage.getImageKey()) &&
                                image.getWidth() == originalImage.getWidth() &&
                                image.getHeight() == originalImage.getHeight()
                )
                .expectComplete()
                .verify();

        // verify interactions
        Mockito.verify(resizedImageRepository).findResizedImageByImageKey(imageKey);
        Mockito.verify(originalImageRepository).findById(originalImage.getImageId());
    }


}

