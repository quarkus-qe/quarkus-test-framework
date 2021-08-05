package io.quarkus.test.logging;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;

import io.quarkus.test.bootstrap.Service;
import io.quarkus.test.utils.FileUtils;

public class FileServiceLoggingHandler extends ServiceLoggingHandler {

    private final File file;
    private String printedContent;

    public FileServiceLoggingHandler(Service context, File input) {
        super(context);
        this.file = input;
    }

    @Override
    protected synchronized void handle() {
        if (file.exists()) {
            String newContent = FileUtils.loadFile(file);
            onStringDifference(newContent, printedContent);
            printedContent = newContent;
        }
    }

    @Override
    public List<String> logs() {
        try {
            return Files.readAllLines(file.toPath(), Charset.defaultCharset());
        } catch (IOException e) {
            Log.warn("Exception reading file log file", e);
            // Fallback to default implementation:
            return super.logs();
        }
    }
}
