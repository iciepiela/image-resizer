package pl.edu.agh.to.imageresizer.services;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.edu.agh.to.imageresizer.dto.ImageDto;
import reactor.test.StepVerifier;

public class ImageResizerTest {
    private static final Logger log = LoggerFactory.getLogger(ImageResizerTest.class);
    private final String ORIGINAL_BASE64 = "src/test/resources/services/originalBase64.txt";
    private final String SMALL_BASE64 = "src/test/resources/services/smallBase64.txt";
    private final String MEDIUM_BASE64 = "src/test/resources/services/mediumBase64.txt";
    private final String LARGE_BASE64 = "src/test/resources/services/largeBase64.txt";
    private final String DAMAGED_BASE64 = "src/test/resources/services/damagedBase64.txt";

    @Test
    public void resize() {
        //given
        String sessionKey = "sessionKey";
        ImageResizer imageResizer = new ImageResizer();
        String original = Util.readFile(ORIGINAL_BASE64);

        ImageDto imageDto = new ImageDto("key", "name", original, 200, 200);
        String small = Util.readFile(SMALL_BASE64);
        String medium = Util.readFile(MEDIUM_BASE64);
        String large = Util.readFile(LARGE_BASE64);
        log.info("original: {}", original);
        //when and then
        StepVerifier.create(imageResizer.resize(imageDto, sessionKey))
                .expectNextMatches(image -> image.getBase64().equals(small) && image.getWidth() == 100 && image.getHeight() == 100)
                .expectNextMatches(image -> image.getBase64().equals(medium) && image.getWidth() == 200 && image.getHeight() == 200)
                .expectNextMatches(image -> image.getBase64().equals(large) && image.getWidth() == 300 && image.getHeight() == 300)
                .expectComplete()
                .verify();

    }

    @Test
    public void resizeDamaged() {
        //given
        String sessionKey = "sessionKey";
        ImageResizer imageResizer = new ImageResizer();
        String damagedBase64 = Util.readFile(DAMAGED_BASE64);
        ImageDto imageDto = new ImageDto("key", "name", damagedBase64, 200, 200);

        //when and then
        StepVerifier.create(imageResizer.resize(imageDto, sessionKey))
                .expectNextMatches(image -> image.getBase64().equals("ERROR")
                        && image.getWidth() == 0 && image.getHeight() == 0)
                .expectComplete()
                .verify();

    }
}
