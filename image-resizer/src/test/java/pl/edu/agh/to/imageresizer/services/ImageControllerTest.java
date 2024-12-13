package pl.edu.agh.to.imageresizer.services;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import pl.edu.agh.to.imageresizer.controllers.ImageController;
import pl.edu.agh.to.imageresizer.model.ImageDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.Mockito.mock;


class ImageControllerTest {

    @Mock
    private ImageService imageService = Mockito.mock(ImageService.class);
    private final ImageController imageController = new ImageController(imageService);


    @Test
    void testGetImagesBySessionKey() {
        // given
        String sessionKey = "test-session";
        ImageDto image1 = new ImageDto("image1", "url1", "smallUrl1", 0, 0);
        ImageDto image2 = new ImageDto("image2", "url2", "smallUrl2", 0, 0);
        ImageDto completeRequest = new ImageDto("COMPLETE_REQUEST", "COMPLETE_REQUEST", "COMPLETE_REQUEST", 0, 0);

        Mockito.when(imageService.getResizedImagesForSessionKey(sessionKey))
                .thenReturn(Flux.just(image1, image2));

        //when and then
        StepVerifier.create(imageController.getImagesBySessionKey(sessionKey))
                .expectNext(ResponseEntity.ok(image1))
                .expectNext(ResponseEntity.ok(image2))
                .expectNext(ResponseEntity.ok(completeRequest))
                .verifyComplete();
    }

    @Test
    void testGetAllImages() {
        //given
        ImageDto image1 = new ImageDto("image1", "url1", "smallUrl1", 0, 0);
        ImageDto image2 = new ImageDto("image2", "url2", "smallUrl2", 0, 0);
        ImageDto completeRequest = new ImageDto("COMPLETE_REQUEST", "COMPLETE_REQUEST", "COMPLETE_REQUEST", 0, 0);

        Mockito.when(imageService.getAllResizedImages()).thenReturn(Flux.just(image1, image2));

        //when and then
        StepVerifier.create(imageController.getAllImages())
                .expectNext(ResponseEntity.ok(image1))
                .expectNext(ResponseEntity.ok(image2))
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

        Mockito.verify(imageService, Mockito.times(images.size()))
                .resizeAndSaveOriginalImage(Mockito.any(ImageDto.class), Mockito.eq(sessionKey));
    }
}

