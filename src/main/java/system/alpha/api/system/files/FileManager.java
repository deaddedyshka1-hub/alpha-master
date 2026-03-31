package system.alpha.api.system.files;

import lombok.Getter;
import system.alpha.api.system.configs.FriendManager;

import java.util.ArrayList;
import java.util.List;

@Getter
public class FileManager {
    @Getter
    private final static FileManager instance = new FileManager();

    private final List<AbstractFile> files = new ArrayList<>();

    public void load() {
        register(
                FriendManager.getInstance()
        );

        for (AbstractFile file : files) {
            file.load();
        }
    }

    public void save() {
        for (AbstractFile file : files) {
            file.save();
        }
    }

    public void register(AbstractFile... files) {
        this.files.addAll(List.of(files));
    }
}
