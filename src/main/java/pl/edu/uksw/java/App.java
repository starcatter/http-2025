package pl.edu.uksw.java;

import com.sun.net.httpserver.HttpServer;
import io.javalin.Javalin;
import io.javalin.rendering.template.JavalinThymeleaf;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.IContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Hello world!
 */
public class App {
    private final int port;

    public App(int port) {
        this.port = port;
    }

    public static void main(String[] args) throws IOException {
        App app = new App(8088);
        app.startSocketServer();
//        app.startHTTPServer();
//        app.startTemplatedServer();
//        app.startFullServer();
//        app.startJavalinBasic();
//        app.startJavalinTemplated();
    }

    public void startSocketServer() throws IOException {
        ServerSocket server = new ServerSocket(port);
        while (true) {
            Socket client = server.accept();
            PrintWriter out = new PrintWriter(client.getOutputStream());
            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: text/html");
            out.println();
            out.println("<html><body><h1>Hello World!</h1></body></html>");
            out.close();
        }
    }

    public void startHTTPServer() throws IOException {
        var path = Thread.currentThread().getContextClassLoader().getResource("basic").getPath();

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", exchange -> {
            String response = Files.readString(Path.of(path, "index.html"));

            exchange.sendResponseHeaders(200, response.length());
            exchange.getResponseBody().write(response.getBytes());
            exchange.close();
        });
        server.start();
    }

    public void startTemplatedServer() throws IOException {
        var path = Thread.currentThread().getContextClassLoader().getResource("templated").getPath();

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new TemplatedHandler(path));
        server.start();
    }


    public void startFullServer() throws IOException {
        var path = Thread.currentThread().getContextClassLoader().getResource("www").getPath();

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new TemplatedHandler(path));
        server.createContext("/static/", new StaticFileHandler(path));
        server.start();
    }

    public void startJavalinBasic() throws IOException {
        var path = Thread.currentThread().getContextClassLoader().getResource("www").getPath();

        Javalin app = Javalin.create(config -> {
            // Serve static files from "static" directory
            config.staticFiles.add(staticFiles -> {
                staticFiles.directory = "www/static";
            });
        });

        // Root route: serve HTML
        app.get("/html/", ctx -> ctx.html("<h1>Hello from Javalin!</h1>"));

        // Route to serve the HTML file directly
        String response = Files.readString(Path.of(path, "index.html"));
        app.get("/", ctx -> ctx.html(response));

        app.start(port);
    }

    public void startJavalinTemplated(){
        // Configure Thymeleaf
        TemplateEngine templateEngine = new TemplateEngine();
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("www/templates/");
        templateEngine.setTemplateResolver(resolver);

        Javalin app = Javalin.create(config -> {
            // Extra logging
            config.bundledPlugins.enableDevLogging();

            // Serve static files from "static" directory
            config.staticFiles.add(staticFiles -> {
                staticFiles.directory = "www/static";
            });

            // Enable templating
            config.fileRenderer(new JavalinThymeleaf(templateEngine));
        });

        // Render Thymeleaf template
        app.get("/", ctx -> {
            ctx.contentType("text/html");

            LocalDateTime now = LocalDateTime.now();

            var replacements = Map.of(
                    "time", now.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                    "date", now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    "addr", ctx.req().getRemoteAddr()
            );
            ctx.render("index.html", replacements);
        });

        app.start(port);
    }
}
