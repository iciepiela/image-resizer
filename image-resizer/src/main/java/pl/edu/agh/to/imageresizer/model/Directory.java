package pl.edu.agh.to.imageresizer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "directories")
public class Directory {
    @Id
    private long directoryId;
    @Column("name")
    private String name;
    @Column("directory_key")
    private String directoryKey;
    @Column("parent_key")
    private String parentKey;
    @Column("session_key")
    private String sessionKey;
    @Column("image_count")
    private int imageCount;
    @Column("sub_directories_count")
    private int subDirectoriesCount;

    public Directory(String name, String directoryKey, String parentKey, String sessionKey, int imageCount, int subDirectoriesCount) {
        this.name = name;
        this.directoryKey = directoryKey;
        this.parentKey = parentKey;
        this.sessionKey = sessionKey;
        this.imageCount = imageCount;
        this.subDirectoriesCount = subDirectoriesCount;
    }
}
