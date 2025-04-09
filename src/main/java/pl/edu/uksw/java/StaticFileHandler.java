package pl.edu.uksw.java;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

class StaticFileHandler implements HttpHandler {
    private final String directory;

    public StaticFileHandler(String directory) {
        this.directory = directory;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        File file = new File(directory, path);

        // Prevent directory traversal [[6]]
        if (!file.exists() || file.isDirectory() || !file.getCanonicalPath().startsWith(new File(directory).getCanonicalPath())) {
            exchange.sendResponseHeaders(404, -1);
            return;
        }

        // Set MIME type
        String mimeType = Files.probeContentType(file.toPath());
        exchange.getResponseHeaders().set("Content-Type", mimeType != null ? mimeType : "application/octet-stream");

        // Send file
        exchange.sendResponseHeaders(200, file.length());
        try (OutputStream os = exchange.getResponseBody();
             FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        }
    }
}
