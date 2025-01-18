package pl.edu.agh.to.imageresizer.dto;

public record DirectoryDto(String name, String directoryKey, String parentKey, int imageCount, int subDirectoriesCount) {
}
