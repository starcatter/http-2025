**Laboratorium 7: Serwer HTTP w Javie — od gniazd do frameworków**

## Informacje organizacyjne

**Data:** 14.04.2026, 8:00–9:30  
**Temat:** Podstawy protokołu HTTP, obsługa żądań GET i POST, parametry zapytania, szablonowanie HTML. Progresja od surowego gniazda TCP przez wbudowany serwer JDK do frameworka Javalin z Thymeleaf.

**Repozytorium:**
> **https://github.com/starcatter/http_2025**

Projekt startowy zawiera minimalną implementację serwera na surowym gnieździe TCP. Na zajęciach kolejno wprowadzamy wyższe poziomy abstrakcji, analizując różnice w kodzie, bezpieczeństwie i ergonomii pracy.

**Struktura zajęć:**
- **8:00–8:15** — Wejściówka (3 pytania): powtórka z Lab 2–5
- **8:15–8:45** — Część 1 (30 min): protokół HTTP, demonstracja ewolucji od gniazd do serwera HTTP
- **8:45–9:30** — Część 2 (45 min): Javalin + Thymeleaf, definiowanie ścieżek, szablony, obsługa formularza POST

**Uwagi techniczne:**
- Wymagana przeglądarka z otwartymi narzędziami deweloperskimi (zakładka **Network**).
- JDK 18+ (dla zadania opcjonalnego z JEP 408).
- Projekt Maven — po sklonowaniu wykonaj `mvn clean package` lub pozwól IntelliJ pobrać zależności.
- Port domyślny: **8088**. Tylko jeden serwer może nasłuchiwać na danym porcie — przed uruchomieniem kolejnego wariantu zatrzymaj poprzedni.

---

## Wejściówka (8:00–8:15)

> **Czas:** 15 minut  
> **Format:** 3 pytania jednokrotnego wyboru.

## Część 1 (8:15 — 8:45): Podstawy protokołu HTTP — ewolucja od gniazd do frameworków

### 1.1 Protokół HTTP

**HTTP** (*Hypertext Transfer Protocol*) jest protokołem tekstowym działającym nad TCP. Klient wysyła żądanie, serwer zwraca odpowiedź. Obie wiadomości są czytelne dla człowieka.

**Minimalne żądanie:**
```
GET /hello HTTP/1.1
Host: localhost:8088

```

**Minimalna odpowiedź:**
```
HTTP/1.1 200 OK
Content-Type: text/html

Hello HTTP!
```

**Główne elementy:**
- Linia statusu (metoda i wersja protokołu w żądaniu; kod statusu w odpowiedzi)
- Nagłówki (każdy w osobnej linii)
- Pusta linia oddzielająca nagłówki od treści
- Treść (body)

**Wybrane kody statusu:**

| Kod | Znaczenie                     | Zastosowanie                     |
|-----|-------------------------------|----------------------------------|
| 200 | OK                            | Sukces                           |
| 301/302 | Moved Permanently / Found | Przekierowanie                   |
| 404 | Not Found                     | Zasób nie istnieje               |
| 500 | Internal Server Error         | Błąd po stronie serwera          |

**Materiały:**
- [MDN — HTTP Overview](https://developer.mozilla.org/en-US/docs/Web/HTTP/Overview)
- [MDN — HTTP Methods](https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods)
- [MDN — HTTP Status Codes](https://developer.mozilla.org/en-US/docs/Web/HTTP/Status)

### 1.2 Narzędzia deweloperskie przeglądarki

Naciśnij `F12`, przejdź do zakładki **Network**. Po odświeżeniu strony widać wszystkie wykonane żądania HTTP wraz z nagłówkami, treścią odpowiedzi i danymi przesłanymi w przypadku POST.

**Materiały:**
- [Chrome DevTools Network Reference](https://developer.chrome.com/docs/devtools/network/reference/)
- [Firefox Developer Tools](https://firefox-source-docs.mozilla.org/devtools-user/network_monitor/)

### 1.3 Serwer na surowym gnieździe TCP

Kod startowy (`App.java`):

```java
package pl.edu.uksw.java;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class App {
    private final int port;

    public App(int port) {
        this.port = port;
    }

    public static void main(String[] args) throws IOException {
        App app = new App(8088);
        app.startSocketServer();
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
            out.flush();
            out.close();
        }
    }
}
```

Klasa `ServerSocket` z pakietu `java.net` pozwala nasłuchiwać na porcie TCP i akceptować połączenia. Każde połączenie zwraca obiekt `Socket`, przez który możemy wysyłać i odbierać dane.

**Materiały:**
- [Oracle JavaDoc — ServerSocket](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/net/ServerSocket.html)
- [Oracle JavaDoc — Socket](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/net/Socket.html)

**Zadanie 1.3a (obowiązkowe)**  
Uruchom serwer. Sprawdź odpowiedź w przeglądarce (`http://localhost:8088`) i w narzędziach deweloperskich (zakładka Network). Zaobserwuj surowe nagłówki HTTP.

**Zadanie 1.3b (opcjonalne)**  
Zmodyfikuj odpowiedź tak, aby zawierała aktualny czas (`LocalDateTime.now()`). Zaobserwuj, że serwer generuje treść dynamicznie przy każdym żądaniu.

**Zadanie 1.3c (opcjonalne)**  
Rozbuduj serwer tak, aby parsował pierwszą linię żądania i obsługiwał parametr `name` w ścieżce `/greet?name=...`. Rozwiązanie znajduje się w sekcji **Rozwiązania**.

### 1.4 Wbudowany `HttpServer` z JDK

Pakiet `com.sun.net.httpserver` dostarcza gotową implementację serwera HTTP. Nie wymaga zewnętrznych zależności.

```java
public void startHTTPServer() throws IOException {
    var path = Thread.currentThread().getContextClassLoader()
        .getResource("basic").getPath();

    HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
    server.createContext("/", exchange -> {
        String response = Files.readString(Path.of(path, "index.html"));
        exchange.sendResponseHeaders(200, response.length());
        exchange.getResponseBody().write(response.getBytes());
        exchange.close();
    });
    server.start();
}
```

**Materiały:**
- [Oracle JavaDoc — HttpServer](https://docs.oracle.com/en/java/javase/17/docs/api/jdk.httpserver/com/sun/net/httpserver/HttpServer.html)

**Zadanie 1.4a (opcjonalne — demonstracja na zajęciach)**  
Uruchom `startHTTPServer()`. Zmodyfikuj plik `index.html` w katalogu `resources/basic` i sprawdź efekt.

**Zadanie 1.4b (opcjonalne — demonstracja na zajęciach)**  
Ustaw breakpoint w handlerze. Uruchom w trybie debug i przeanalizuj obiekt `exchange` (metoda, URI, nagłówki).

**Zadanie 1.4c (opcjonalne — do samodzielnej pracy)**  
Dodaj obsługę parametru `name` z query string w handlerze `HttpServer`.

### 1.5 Szablonowanie — `TemplatedHandler`

Dynamiczne generowanie HTML przez konkatenację stringów jest nieergonomiczne i niebezpieczne (XSS). Wprowadzamy prosty mechanizm szablonów oparty na wyrażeniach regularnych.

Utwórz klasę `TemplatedHandler.java`:

```java
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
        String template = Files.readString(Path.of(path, "index.tpl.html"));

        Map<String, String> replacements = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        replacements.put("time", now.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        replacements.put("date", now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        replacements.put("addr", exchange.getRemoteAddress().getAddress().getHostAddress());
        replacements.put("userAgent", exchange.getRequestHeaders().getFirst("User-Agent"));

        Pattern pattern = Pattern.compile("\\[\\[(.*?)\\]\\]");
        Matcher matcher = pattern.matcher(template);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String key = matcher.group(1);
            String value = replacements.getOrDefault(key, "[[" + key + "]]");
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(sb);

        String response = sb.toString();
        exchange.sendResponseHeaders(200, response.length());
        exchange.getResponseBody().write(response.getBytes());
        exchange.close();
    }
}
```

Dodaj metodę `startTemplatedServer()` analogicznie jak w poprzednich przykładach.

**Zadanie 1.5a (opcjonalne — demonstracja na zajęciach)**  
Uruchom `startTemplatedServer()`. Sprawdź, jak działa prosty system szablonów z placeholderami `[[nazwa]]`.

**Zadanie 1.5b (opcjonalne — do samodzielnej pracy)**  
Zaimplementuj `StaticFileHandler` (kod znajduje się w repozytorium) i dodaj `startFullServer()` serwujący pliki statyczne z katalogu `/static/`.

**Zadanie 1.5c (opcjonalne — do samodzielnej pracy, JEP 408)**  
Zastąp własny `StaticFileHandler` implementacją `SimpleFileServer.createFileHandler(...)` z JEP 408 (JDK 18+). Porównaj kod i zachowanie.

**Materiały:**
- [JEP 408: Simple Web Server](https://openjdk.org/jeps/408)

---

## Część 2 (8:45–9:30): Javalin + Thymeleaf

### 2.1 Konfiguracja Javalin — wyjaśnienie wzorca budowniczego

**Javalin** to lekki framework webowy dla Javy i Kotlina, oparty na Jetty. Używa wzorca **Builder** z wyrażeniem lambda do konfiguracji:

```java
Javalin app = Javalin.create(config -> {
    // Tutaj konfigurujemy instancję przed uruchomieniem
    config.staticFiles.add(...);           // serwowanie plików statycznych
    config.fileRenderer(...);              // rejestracja silnika szablonów
    config.bundledPlugins.enableDevLogging(); // włączenie szczegółowego logowania
});
```

Parametr `config` to obiekt konfiguracyjny. Lambda pozwala na modyfikację ustawień przed utworzeniem serwera. Jest to idiomatyczny i czytelny sposób konfiguracji w nowoczesnej Javie.

**Materiały:**
- [Javalin Documentation](https://javalin.io/documentation)
- [Javalin Configuration](https://javalin.io/documentation#configuration)

### 2.2 Podstawowa wersja Javalin

```java
public void startJavalinBasic() throws IOException {
    var path = Thread.currentThread().getContextClassLoader()
        .getResource("www").getPath();

    Javalin app = Javalin.create(config -> {
        config.staticFiles.add(staticFiles -> {
            staticFiles.directory = "www/static";
        });

        config.routes.get("/", ctx ->  ctx.html("<h1>Hello from Javalin!</h1>"));
    });

    app.start(port);
}
```

Metody `app.get()`, `app.post()` itp. definiują **routing** — mapowanie kombinacji metody HTTP i ścieżki URL na handler.

**Materiały:**
- [Javalin Handlers](https://javalin.io/documentation#handlers)
- [Javalin Context](https://javalin.io/documentation#context)

**Zadanie 2.2a (obowiązkowe)**  
Uruchom `startJavalinBasic()`. Sprawdź działanie ścieżki `/` oraz ładowanie plików statycznych.

**Zadanie 2.2b (obowiązkowe)**  
Dodaj ścieżkę `/greet` obsługującą parametr `name` z query string (`ctx.queryParam("name")`) oraz ścieżkę `/product/{id}` obsługującą parametr ścieżki (`ctx.pathParam("id")`).

### 2.3 Thymeleaf z Javalin

**Thymeleaf** to silnik szablonów HTML. Pozwala na czyste oddzielenie logiki od prezentacji. Automatycznie escapuje dane, chroniąc przed XSS.

Konfiguracja silnika:

```java
TemplateEngine templateEngine = new TemplateEngine();
ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
resolver.setPrefix("www/templates/");
templateEngine.setTemplateResolver(resolver);

Javalin app = Javalin.create(config -> {
    config.bundledPlugins.enableDevLogging();
    config.staticFiles.add(s -> s.directory = "www/static");
    config.fileRenderer(new JavalinThymeleaf(templateEngine));

    // Render Thymeleaf template
    config.routes.get("/", ctx -> {
        ctx.contentType("text/html");

        LocalDateTime now = LocalDateTime.now();

        // template data model
        var replacements = Map.of(
                "time", now.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                "date", now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                "addr", ctx.req().getRemoteAddr()
        );
        
        // render template
        ctx.render("index.html", replacements);
    });
});
```

**Materiały:**
- [Thymeleaf Documentation](https://www.thymeleaf.org/documentation.html)
- [Thymeleaf Tutorial](https://www.thymeleaf.org/doc/tutorials/3.1/usingthymeleaf.html)
- [Javalin Template Engines](https://javalin.io/documentation#template-engines)

**Zadanie 2.3a (obowiązkowe)**  
Uruchom `startJavalinTemplated()`. Przekaż model z aktualnym czasem i adresem IP. Sprawdź, jak Thymeleaf renderuje szablon z danymi.

**Zadanie 2.3b (zalecane)**  
Dodaj listę użytkowników do modelu i wyświetl ją z użyciem `th:each` oraz `th:if` do oznaczania administratorów.

### 2.4 Obsługa formularza POST

Formularz HTML (`form.html`):

```html
<form method="post" action="/submit">
    <input type="text" name="name" placeholder="Imię">
    <input type="email" name="email" placeholder="Email">
    <button type="submit">Wyślij</button>
</form>
```

Endpoint POST w Javalin:

```java
app.post("/submit", ctx -> {
    String name = ctx.formParam("name");
    String email = ctx.formParam("email");
    ctx.render("result.html", Map.of("name", name, "email", email));
});
```

**Materiały:**
- [MDN — HTML Forms](https://developer.mozilla.org/en-US/docs/Learn/Forms)
- [MDN — Form Element](https://developer.mozilla.org/en-US/docs/Web/HTML/Element/form)

**Zadanie 2.4a (obowiązkowe)**  
Utwórz formularz (`/form`) i endpoint POST (`/submit`). Przekaż dane do szablonu wyniku.

**Zadanie 2.4b (zalecane)**  
W narzędziach deweloperskich przeanalizuj żądanie POST — sprawdź metodę, nagłówek `Content-Type` oraz dane formularza w Payload.

**Zadanie 2.4c (opcjonalne)**  
Dodaj walidację pól formularza i wyświetlanie komunikatu błędu w szablonie `form.html`.

**Zadanie 2.4d (opcjonalne)**  
Dodaj arkusz stylów `style.css` i załącz go w szablonach za pomocą `th:href="@{/style.css}"`.

---

## Rozwiązania wszystkich zadań

### Rozwiązanie 1.3b (aktualny czas w surowym gnieździe)

```java
public void startSocketServer() throws IOException {
    ServerSocket server = new ServerSocket(port);
    while (true) {
        Socket client = server.accept();
        PrintWriter out = new PrintWriter(client.getOutputStream());
        
        String currentTime = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        
        out.println("HTTP/1.1 200 OK");
        out.println("Content-Type: text/html; charset=utf-8");
        out.println();
        out.println("<html><body><h1>Hello World!</h1>");
        out.println("<p>Aktualny czas: " + currentTime + "</p>");
        out.println("</body></html>");
        out.flush();
        out.close();
    }
}
```

### Rozwiązanie 1.3c (parsowanie GET w surowym gnieździe)

```java
public void startSocketServerWithRouting() throws IOException {
    ServerSocket server = new ServerSocket(port);
    while (true) {
        Socket client = server.accept();
        var reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(client.getInputStream()));
        String requestLine = reader.readLine();

        String responseBody = "<html><body><h1>Hello World!</h1></body></html>";

        if (requestLine != null && requestLine.startsWith("GET")) {
            String[] parts = requestLine.split(" ");
            if (parts.length >= 2) {
                try {
                    java.net.URI uri = java.net.URI.create(parts[1]);
                    if ("/greet".equals(uri.getPath())) {
                        String query = uri.getQuery();
                        if (query != null) {
                            for (String param : query.split("&")) {
                                String[] kv = param.split("=", 2);
                                if (kv.length == 2 && "name".equals(kv[0])) {
                                    String name = java.net.URLDecoder.decode(
                                        kv[1], java.nio.charset.StandardCharsets.UTF_8);
                                    responseBody = "<html><body><h1>Witaj, " + name + "!</h1></body></html>";
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        PrintWriter out = new PrintWriter(client.getOutputStream());
        out.println("HTTP/1.1 200 OK");
        out.println("Content-Type: text/html; charset=utf-8");
        out.println();
        out.println(responseBody);
        out.flush();
        out.close();
    }
}
```

### Rozwiązanie 1.4c (parametr w HttpServer)

W handlerze:
```java
String query = exchange.getRequestURI().getQuery();
String name = null;
if (query != null) {
    for (String p : query.split("&")) {
        String[] kv = p.split("=", 2);
        if (kv.length == 2 && "name".equals(kv[0])) {
            name = java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
        }
    }
}
String response = name == null 
    ? "<h1>Witaj, nieznajomy!</h1>" 
    : "<h1>Witaj, " + name + "!</h1>";
```

### Rozwiązanie 1.5a (TemplatedHandler z userAgent)

Kod w sekcji 1.5 już zawiera obsługę `userAgent`:
```java
replacements.put("userAgent", exchange.getRequestHeaders().getFirst("User-Agent"));
```

W szablonie:
```html
<p>Twoja przeglądarka: [[userAgent]]</p>
```

### Rozwiązanie 1.5b / 1.5c (StaticFileHandler i JEP 408)

Kody `StaticFileHandler` i `startFullServer()` znajdują się w repozytorium.  
Dla JEP 408:

```java
var fileHandler = SimpleFileServer.createFileHandler(Path.of(path, "static"));
server.createContext("/static/", fileHandler);
```

`SimpleFileServer` automatycznie obsługuje MIME types, indeksowanie katalogów i zabezpieczenia przed directory traversal.

**Materiały:**
- [JEP 408: Simple Web Server](https://openjdk.org/jeps/408)

### Rozwiązanie 2.2b (ścieżki w Javalin)

```java
app.get("/greet", ctx -> {
    String name = ctx.queryParam("name");
    ctx.html(name == null || name.isBlank() 
        ? "<h1>Witaj, nieznajomy!</h1>" 
        : "<h1>Witaj, " + name + "!</h1>");
});

app.get("/product/{id}", ctx -> {
    String id = ctx.pathParam("id");
    ctx.html("<h1>Produkt #" + id + "</h1>");
});
```

### Rozwiązanie 2.3b (pętla + warunek w Thymeleaf)

Model:
```java
var model = Map.of(
    "time", now.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
    "users", List.of(
        Map.of("name", "Anna", "admin", true),
        Map.of("name", "Jan", "admin", false),
        Map.of("name", "Piotr", "admin", true)
    )
);
```

Szablon:
```html
<ul>
    <li th:each="user : ${users}">
        <span th:text="${user.name}"></span>
        <span th:if="${user.admin}" style="color:red;"> (Administrator)</span>
    </li>
</ul>
```

### Rozwiązanie 2.4a–2.4c (formularz POST + walidacja)

Endpoint GET dla formularza:
```java
app.get("/form", ctx -> {
    ctx.render("form.html");
});
```

Endpoint POST:
```java
app.post("/submit", ctx -> {
    String name = ctx.formParam("name");
    String email = ctx.formParam("email");

    if (name == null || name.isBlank()) {
        ctx.render("form.html", Map.of("error", "Imię jest wymagane"));
        return;
    }
    if (email == null || !email.contains("@")) {
        ctx.render("form.html", Map.of("error", "Podaj poprawny adres e-mail"));
        return;
    }

    ctx.render("result.html", Map.of(
        "name", name.trim(),
        "email", email.trim()
    ));
});
```

Szablon `form.html`:
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Formularz</title>
</head>
<body>
    <h1>Wypełnij formularz</h1>
    <p th:if="${error != null}" class="error" th:text="${error}"></p>
    <form method="post" action="/submit">
        <input type="text" name="name" placeholder="Imię">
        <input type="email" name="email" placeholder="Email">
        <button type="submit">Wyślij</button>
    </form>
</body>
</html>
```

Szablon `result.html`:
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Wynik</title>
</head>
<body>
    <h1>Dziękujemy!</h1>
    <p>Imię: <span th:text="${name}"></span></p>
    <p>Email: <span th:text="${email}"></span></p>
</body>
</html>
```

### Rozwiązanie 2.4d (dodanie CSS)

Utwórz plik `www/static/style.css`:

```css
body {
    font-family: Arial, sans-serif;
    max-width: 800px;
    margin: 0 auto;
    padding: 20px;
}

.error {
    color: red;
    font-weight: bold;
}

input {
    display: block;
    margin: 10px 0;
    padding: 8px;
    width: 100%;
}

button {
    padding: 10px 20px;
    background-color: #007bff;
    color: white;
    border: none;
    cursor: pointer;
}

button:hover {
    background-color: #0056b3;
}
```

W szablonach HTML dodaj w sekcji `<head>`:

```html
<link rel="stylesheet" th:href="@{/style.css}">
```

Wyrażenie `@{/style.css}` generuje prawidłową ścieżkę do pliku statycznego. Thymeleaf automatycznie rozwiązuje ścieżkę relatywną do katalogu statycznego zdefiniowanego w konfiguracji Javalin.

---

## Podsumowanie i materiały uzupełniające

Na laboratorium poznaliśmy pełną drogę od najniższego poziomu (gniazdo TCP) do nowoczesnego podejścia opartego na lekkim frameworku. Kluczowe wnioski:

- **HTTP to po prostu tekst** przesyłany przez TCP — każde żądanie i odpowiedź składa się z nagłówków i opcjonalnej treści.
- **Ręczne pisanie serwerów** jest pouczające, ale niepraktyczne w produkcji — brakuje obsługi błędów, wielowątkowości, bezpieczeństwa.
- **Javalin znacznie upraszcza** definiowanie ścieżek, obsługę parametrów i integrację z silnikami szablonów.
- **Thymeleaf pozwala na czyste oddzielenie** warstwy prezentacji od logiki z wbudowanym zabezpieczeniem przed XSS (`th:text` automatycznie escapuje dane).

**Kluczowe różnice między poziomami abstrakcji:**

| Poziom | Zalety | Wady |
|--------|--------|------|
| Surowe gniazdo TCP | Pełna kontrola, zrozumienie protokołu | Brak wielowątkowości, obsługi błędów, bezpieczeństwa |
| `HttpServer` JDK | Wbudowany w JDK, brak zależności | Niskopoziomowe API, ograniczona funkcjonalność |
| Javalin + Thymeleaf | Ergonomia, routing, pluginy, szablony | Dodatkowe zależności |

**Materiały uzupełniające:**

- [MDN — HTTP](https://developer.mozilla.org/en-US/docs/Web/HTTP)
- [MDN — HTML Forms](https://developer.mozilla.org/en-US/docs/Learn/Forms)
- [MDN — CSS Basics](https://developer.mozilla.org/en-US/docs/Learn/Getting_started_with_the_web/CSS_basics)
- [Javalin Documentation](https://javalin.io/documentation)
- [Javalin Samples](https://github.com/javalin/javalin-samples)
- [Thymeleaf Documentation](https://www.thymeleaf.org/documentation.html)
- [JEP 408 — Simple Web Server](https://openjdk.org/jeps/408)

**Git:** Po zakończeniu zajęć wykonaj commit z komunikatem np.: *"Lab 7 complete: HTTP, Javalin, Thymeleaf"*.
```