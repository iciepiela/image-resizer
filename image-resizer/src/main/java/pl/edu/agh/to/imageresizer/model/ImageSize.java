package pl.edu.agh.to.imageresizer.model;

import lombok.Getter;

@Getter
public enum ImageSize {
    SMALL(50, 50),
    MEDIUM(200, 200),
    LARGE(300, 300);


    private final int width;
    private final int height;

    ImageSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

}

