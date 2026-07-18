package repository;

import util.FileUtil;
import java.util.List;

public class FileRepository {
    private final String filePath;

    public FileRepository(String filePath) {
        this.filePath = filePath;
    }

    public List<String> readAll() {
        return FileUtil.readLines(filePath);
    }

    public void writeAll(List<String> lines) {
        FileUtil.writeLines(filePath, lines);
    }

    public void append(String line) {
        FileUtil.appendLine(filePath, line);
    }
}
