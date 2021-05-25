import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class FileListReader {

    ArrayList<File> fileList = new ArrayList<>();

    FileListReader(String dataDir){
        Path dir = Paths.get(dataDir);
        try {
            Files.walk(dir).forEach(path -> createList(path.toFile()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createList(File file) {
        if (!file.isDirectory()) {
            this.fileList.add(file);
        }
    }

    public ArrayList<File> getFiles(){
        return fileList;
    }
}
