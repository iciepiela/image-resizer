package pl.edu.agh.to.imageresizer.services;

import org.junit.jupiter.api.Test;
import pl.edu.agh.to.imageresizer.model.ImageDto;
import reactor.test.StepVerifier;

public class ImageResizerTest {

    @Test
    public void testResize() {
        //given
        String sessionKey = "sessionKey";
        String resizedBase64 = "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";
        ImageResizer imageResizer = new ImageResizer();
        ImageDto imageDto = new ImageDto("key", "name", "data:image/jpeg;base64,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", 200, 200);

        //when and then
        StepVerifier.create(imageResizer.resize(imageDto, sessionKey))
                .expectNextMatches(image -> image.getBase64().equals(resizedBase64) && image.getWidth().equals(200) && image.getHeight().equals(200))
                .expectComplete()
                .verify();

    }
}
