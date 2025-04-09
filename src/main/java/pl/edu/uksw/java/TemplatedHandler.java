package pl.edu.uksw.java;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class TemplatedHandler implements HttpHandler {
    private final String path;

    public TemplatedHandler(String path) {
        this.path = path;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String templateContent = Files.readString(Path.of(path, "index.tpl.html"));

        // Create dynamic data map
        Map<String, String> replacements = new HashMap<>();

        // Populate with dynamic values
        LocalDateTime now = LocalDateTime.now();
        replacements.put("time", now.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        replacements.put("date", now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));

        InetAddress clientAddr = exchange.getRemoteAddress().getAddress();
        replacements.put("addr", clientAddr.getHostAddress());

        // Perform template substitution
        Pattern pattern = Pattern.compile("\\[\\[(.*?)\\]\\]");
        Matcher matcher = pattern.matcher(templateContent);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String tag = matcher.group(1);
            String value = replacements.getOrDefault(tag, "[[" + tag + "]]");
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(sb);

        // Send response
        String responseContent = sb.toString();
        exchange.sendResponseHeaders(200, responseContent.length());
        exchange.getResponseBody().write(responseContent.getBytes());

        exchange.getResponseBody().flush();
        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        exchange.close();
    }
}
