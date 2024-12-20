package pl.edu.agh.to.imageresizer.services;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import pl.edu.agh.to.imageresizer.controllers.ImageController;
import pl.edu.agh.to.imageresizer.dto.ImageDto;
import pl.edu.agh.to.imageresizer.model.OriginalImage;
import pl.edu.agh.to.imageresizer.model.ResizedImage;
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
        ResizedImage image1 = new ResizedImage("key1","image1", "base64_1", "smallUrl1", 0, 0);
        ResizedImage image2 = new ResizedImage("key2","image2", "base64_2", "base64_2", 0, 0);

        ImageDto completeRequest = new ImageDto("COMPLETE_REQUEST", "COMPLETE_REQUEST", "COMPLETE_REQUEST", 0, 0);
        ImageDto imageDto1 = new ImageDto("key1","image1", "base64_1",  0, 0);
        ImageDto imageDto2 = new ImageDto("key2","image2", "base64_2", 0, 0);

        Mockito.when(imageService.getResizedImagesForSessionKey(sessionKey))
                .thenReturn(Flux.just(image1, image2));

        //when and then
        StepVerifier.create(imageController.getImagesBySessionKey(sessionKey))
                .expectNext(ResponseEntity.ok(imageDto1))
                .expectNext(ResponseEntity.ok(imageDto2))
                .expectNext(ResponseEntity.ok(completeRequest))
                .verifyComplete();
    }

    @Test
    void testGetAllImages() {
        //given
        ResizedImage image1 = new ResizedImage("key1","image1", "base64_1", "smallUrl1", 0, 0);
        ResizedImage image2 = new ResizedImage("key2","image2", "base64_2", "base64_2", 0, 0);

        ImageDto completeRequest = new ImageDto("COMPLETE_REQUEST", "COMPLETE_REQUEST", "COMPLETE_REQUEST", 0, 0);
        ImageDto imageDto1 = new ImageDto("key1","image1", "base64_1",  0, 0);
        ImageDto imageDto2 = new ImageDto("key2","image2", "base64_2", 0, 0);

        Mockito.when(imageService.getAllResizedImages()).thenReturn(Flux.just(image1, image2));

        //when and then
        StepVerifier.create(imageController.getAllImages())
                .expectNext(ResponseEntity.ok(imageDto1))
                .expectNext(ResponseEntity.ok(imageDto2))
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

    @Test
    void testGetOriginalImage(){
        // given
        String imageKey="imageKey";
        OriginalImage originalImage=new OriginalImage("name", "base64", 20,20);
        Mockito.when(imageService.getOriginalImage(imageKey)).thenReturn(Mono.just(originalImage));

        ImageDto imageDto=new ImageDto(null,"name","base64",20,20);

        //when and then
        StepVerifier.create(imageController.getOriginalImage(imageKey))
                .expectNext(ResponseEntity.ok(imageDto))
                .verifyComplete();
    }
}

