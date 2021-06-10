package io.quarkus.test.logging;

import java.io.File;

import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.utils.FileUtils;

public class FileQuarkusApplicationLoggingHandler extends LoggingHandler {

    private final File file;
    private String printedContent;

    public FileQuarkusApplicationLoggingHandler(ServiceContext context, File input) {
        super(context);
        this.file = input;
    }

    @Override
    protected void handle() {
        if (file.exists()) {
            String newContent = FileUtils.loadFile(file);
            onStringDifference(newContent, printedContent);
            printedContent = newContent;
        }
    }
}
