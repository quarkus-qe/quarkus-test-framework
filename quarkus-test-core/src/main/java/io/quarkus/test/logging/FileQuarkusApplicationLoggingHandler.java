package io.quarkus.test.logging;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

import io.quarkus.test.ServiceContext;

public class FileQuarkusApplicationLoggingHandler extends LoggingHandler {

    private final Path file;
    private final InputStream from;

    public FileQuarkusApplicationLoggingHandler(ServiceContext context, String output, InputStream from) {
        super(context);
        this.file = context.getServiceFolder().resolve(output);
        this.from = from;
    }

    @Override
    protected void handle() {
        try (OutputStream outStream = new FileOutputStream(file.toFile())) {
            byte[] buffer = new byte[8 * 1024];
            int bytesRead;
            while ((bytesRead = from.read(buffer)) != -1) {
                String line = new String(buffer, 0, bytesRead);
                onLines(line);
                outStream.write(line.getBytes());
            }
        } catch (IOException ignored) {
        }
    }

}
