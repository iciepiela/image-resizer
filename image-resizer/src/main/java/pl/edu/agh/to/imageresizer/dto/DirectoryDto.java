package pl.edu.agh.to.imageresizer.dto;

import java.util.List;

public record DirectoryDto (String name, String dirKey,List<ImageDto> images, List<DirectoryDto> directories){}

