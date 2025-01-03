package pl.edu.agh.to.imageresizer.services;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import pl.edu.agh.to.imageresizer.controllers.ImageController;
import pl.edu.agh.to.imageresizer.dto.ImageDto;
import pl.edu.agh.to.imageresizer.model.ImageSize;
import pl.edu.agh.to.imageresizer.model.OriginalImage;
import pl.edu.agh.to.imageresizer.model.ResizedImage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;


class ImageControllerTest {

    private final String ERROR = "ERROR";
    @Mock
    private ImageService imageService = Mockito.mock(ImageService.class);
    private final ImageController imageController = new ImageController(imageService);

    @Test
    void testGetImagesBySessionKey() {
        // given
        String sessionKey = "test-session";
        ResizedImage image1 = new ResizedImage("key1", "image1", "base64_1", "smallUrl1", 0, 0);
        ResizedImage image2 = new ResizedImage("key2", "image2", ERROR, "base64_2", 0, 0);
        ResizedImage image3 = new ResizedImage("key3", "image3", "base64_3", "base64_3", 0, 0);

        ImageDto completeRequest = new ImageDto("COMPLETE_REQUEST", "COMPLETE_REQUEST", "COMPLETE_REQUEST", 0, 0);
        ImageDto imageDto1 = new ImageDto("key1", "image1", "base64_1", 0, 0);
        ImageDto imageDto2 = new ImageDto("key2", "image2", ERROR, 0, 0);
        ImageDto imageDto3 = new ImageDto("key3", "image3", "base64_3", 0, 0);

        Mockito.when(imageService.getResizedImagesForSessionKey(sessionKey, ImageSize.MEDIUM))
                .thenReturn(Flux.just(image1, image2, image3));

        //when and then
        StepVerifier.create(imageController.getImagesBySessionKey(sessionKey, "medium"))
                .expectNext(ResponseEntity.ok(imageDto1))
                .expectNext(ResponseEntity.status(HttpStatus.NOT_FOUND).body(imageDto2))
                .expectNext(ResponseEntity.ok(imageDto3))
                .expectNext(ResponseEntity.ok(completeRequest))
                .verifyComplete();
    }

    @Test
    void testGetImageByImageKey() {
        // given
        String key = "test-session";
        ResizedImage image1 = new ResizedImage("key1", "image1", "base64_1", "smallUrl1", 0, 0);

        ImageDto completeRequest = new ImageDto("COMPLETE_REQUEST", "COMPLETE_REQUEST", "COMPLETE_REQUEST", 0, 0);
        ImageDto imageDto1 = new ImageDto("key1", "image1", "base64_1", 0, 0);

        Mockito.when(imageService.getResizedImagesByImageKey(key, ImageSize.MEDIUM))
                .thenReturn(Flux.just(image1));

        //when and then
        StepVerifier.create(imageController.getImageByImageKey(key, "medium"))
                .expectNext(ResponseEntity.ok(imageDto1))
                .expectNext(ResponseEntity.ok(completeRequest))
                .verifyComplete();
    }

    @Test
    void testGetImageByImageKeyDamaged() {
        // given
        String key = "test-session";
        ResizedImage image1 = new ResizedImage("key1", "image1", ERROR, "smallUrl1", 0, 0);

        ImageDto completeRequest = new ImageDto("COMPLETE_REQUEST", "COMPLETE_REQUEST", "COMPLETE_REQUEST", 0, 0);
        ImageDto imageDto1 = new ImageDto("key1", "image1", ERROR, 0, 0);

        Mockito.when(imageService.getResizedImagesByImageKey(key, ImageSize.MEDIUM))
                .thenReturn(Flux.just(image1));

        //when and then
        StepVerifier.create(imageController.getImageByImageKey(key, "medium"))
                .expectNext(ResponseEntity.status(HttpStatus.NOT_FOUND).body(imageDto1))
                .expectNext(ResponseEntity.ok(completeRequest))
                .verifyComplete();
    }

    @Test
    void testGetAllImages() {
        //given
        ResizedImage image1 = new ResizedImage("key1", "image1", "base64_1", "smallUrl1", 0, 0);
        ResizedImage image2 = new ResizedImage("key2", "image2", ERROR, "base64_2", 0, 0);
        ResizedImage image3 = new ResizedImage("key3", "image3", "base64_3", "base64_3", 0, 0);

        ImageDto completeRequest = new ImageDto("COMPLETE_REQUEST", "COMPLETE_REQUEST", "COMPLETE_REQUEST", 0, 0);
        ImageDto imageDto1 = new ImageDto("key1", "image1", "base64_1", 0, 0);
        ImageDto imageDto2 = new ImageDto("key2", "image2", ERROR, 0, 0);
        ImageDto imageDto3 = new ImageDto("key3", "image3", "base64_3", 0, 0);
        Mockito.when(imageService.getAllResizedImages(ImageSize.MEDIUM)).thenReturn(Flux.just(image1, image2, image3));

        //when and then
        StepVerifier.create(imageController.getAllImages("medium"))
                .expectNext(ResponseEntity.ok(imageDto1))
                .expectNext(ResponseEntity.status(HttpStatus.NOT_FOUND).body(imageDto2))
                .expectNext(ResponseEntity.ok(imageDto3))
                .expectNext(ResponseEntity.ok(completeRequest))
                .verifyComplete();
    }

    @Test
    void testUploadImages() {
        //given
        String sessionKey = "test-session";
        HttpSession mockSession = mock(HttpSession.class);
        Mockito.when(mockSession.getId()).thenReturn(sessionKey);

        List<ImageDto> images = List.of(
                new ImageDto("image1", "url1", "smallUrl1", 0, 0),
                new ImageDto("image2", "url2", "smallUrl2", 0, 0)
        );

        Mockito.when(imageService.resizeAndSaveOriginalImage(Mockito.any(ImageDto.class), Mockito.eq(sessionKey)))
                .thenReturn(Mono.empty());

        //when and then
        StepVerifier.create(imageController.uploadImages(images, mockSession))
                .expectNext(ResponseEntity.ok(sessionKey))
                .verifyComplete();

    }

    @Test
    void testGetOriginalImage() {
        // given
        String imageKey = "imageKey";
        String sessionKey = "session";
        OriginalImage originalImage = new OriginalImage("name", "base64", sessionKey, imageKey, 20, 20);
        Mockito.when(imageService.getOriginalImage(imageKey)).thenReturn(Mono.just(originalImage));

        ImageDto imageDto = new ImageDto(null, "name", "base64", 20, 20);

        //when and then
        StepVerifier.create(imageController.getOriginalImage(imageKey))
                .expectNext(ResponseEntity.ok(imageDto))
                .verifyComplete();
    }

    @Test
    void testGetOriginalImageDamaged() {
        // given
        String imageKey = "imageKey";
        String sessionKey = "session";
        OriginalImage originalImage = new OriginalImage("name", ERROR, sessionKey, imageKey, 0, 0);
        Mockito.when(imageService.getOriginalImage(imageKey)).thenReturn(Mono.just(originalImage));

        ImageDto imageDto = new ImageDto(null, "name", ERROR, 0, 0);

        //when and then
        StepVerifier.create(imageController.getOriginalImage(imageKey))
                .expectNext(ResponseEntity.status(HttpStatus.NOT_FOUND).body(imageDto))
                .verifyComplete();
    }
    @Test
    void healthCheckTest(){
        ResponseEntity<String> response=imageController.healthCheck();
        assertEquals(response.getStatusCode(),HttpStatus.OK);
        assertEquals(response.getBody(),"Server is running");

    }
}

