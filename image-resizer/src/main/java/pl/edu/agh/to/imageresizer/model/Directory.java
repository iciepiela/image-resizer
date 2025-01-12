package pl.edu.agh.to.imageresizer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "directories")
public class Directory {
    @Id
    private long directoryId;
    private String name;
    private Long parentDirectoryId;
    private String directoryKey;

    public Directory(String name, Long parentDirectoryId, String directoryKey) {
        this.name = name;
        this.parentDirectoryId = parentDirectoryId;
        this.directoryKey = directoryKey;
    }
}