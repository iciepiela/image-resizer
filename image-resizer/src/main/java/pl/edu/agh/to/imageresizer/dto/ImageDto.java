package pl.edu.agh.to.imageresizer.dto;

public record ImageDto (
    String imageKey,
    String name,
    String base64,
    Integer width,
    Integer height
) {}
