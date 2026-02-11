import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Servidor HTTP/1.0 multihilo en Java
 * Optimizado y alineado con los requisitos del enunciado
 */
public class Main {

    private static int PORT = 8080;
    private static final String BASE_DIR = "src/www";
    private static final String ERROR_404_PAGE = "error404.html";
    private static volatile boolean running = true;

    // Thread pool para manejar múltiples solicitudes
    private static final ExecutorService THREAD_POOL =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);

    public static void main(String[] args) {
        try {
            if (args.length > 0) {
                PORT = Integer.parseInt(args[0]);
                if (PORT <= 1024) {
                    System.err.println("El puerto debe ser mayor a 1024");
                    return;
                }
            }

            Files.createDirectories(Paths.get(BASE_DIR));
            createDefault404Page();

            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                System.out.println("======================================");
                System.out.println("Servidor HTTP iniciado");
                System.out.println("Puerto: " + PORT);
                System.out.println("Directorio base: " + BASE_DIR);
                System.out.println("======================================");

                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    running = false;
                    THREAD_POOL.shutdown();
                    System.out.println("\nServidor detenido.");
                }));

                while (running) {
                    Socket socket = serverSocket.accept();
                    THREAD_POOL.execute(new HttpHandler(socket));
                }
            }

        } catch (Exception e) {
            System.err.println("Error fatal del servidor: " + e.getMessage());
        }
    }

    /**
     * Página 404 por defecto
     */
    private static void createDefault404Page() throws IOException {
        Path errorPath = Paths.get(BASE_DIR, ERROR_404_PAGE);
        if (Files.exists(errorPath)) return;

        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>404 - No encontrado</title>
                <style>
                    body { font-family: Arial; text-align: center; padding: 50px; }
                    h1 { font-size: 72px; color: #e74c3c; }
                </style>
            </head>
            <body>
                <h1>404</h1>
                <p>Recurso no encontrado</p>
                <hr>
                <small>Servidor HTTP Java</small>
            </body>
            </html>
            """;

        Files.writeString(errorPath, html, StandardCharsets.UTF_8);
    }

    /**
     * Maneja una solicitud HTTP
     */
    private static class HttpHandler implements Runnable {

        private final Socket socket;

        HttpHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (
                    socket;
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                    OutputStream out = socket.getOutputStream();
                    PrintWriter writer = new PrintWriter(
                            new OutputStreamWriter(out, StandardCharsets.UTF_8), false)
            ) {
                socket.setSoTimeout(30000);

                String requestLine = in.readLine();
                if (requestLine == null || requestLine.isEmpty()) return;

                System.out.println("\n=== SOLICITUD ===");
                System.out.println(requestLine);

                Map<String, String> headers = new HashMap<>();
                String line;
                while (!(line = in.readLine()).isEmpty()) {
                    System.out.println(line);
                    int idx = line.indexOf(':');
                    if (idx > 0) {
                        headers.put(
                                line.substring(0, idx).trim().toLowerCase(),
                                line.substring(idx + 1).trim()
                        );
                    }
                }

                String[] parts = requestLine.split(" ");
                if (parts.length < 3 || !parts[0].equals("GET")) {
                    sendError(writer, out, 501, "Not Implemented");
                    return;
                }

                serveResource(parts[1], writer, out);

            } catch (SocketTimeoutException e) {
                System.err.println("Timeout de conexión");
            } catch (IOException e) {
                System.err.println("Error procesando solicitud: " + e.getMessage());
            }
        }

        /**
         * Sirve un archivo solicitado
         */
        private void serveResource(String resource, PrintWriter writer, OutputStream out) throws IOException {

            if (resource.equals("/")) resource = "/index.html";

            Path basePath = Paths.get(BASE_DIR).toRealPath();
            Path requestedPath = basePath.resolve(resource.substring(1)).normalize();

            if (!requestedPath.startsWith(basePath)) {
                sendError(writer, out, 403, "Forbidden");
                return;
            }

            if (!Files.exists(requestedPath) || !Files.isReadable(requestedPath)) {
                Path errorPath = basePath.resolve(ERROR_404_PAGE);
                if (Files.exists(errorPath)) {
                    sendFile(writer, out, errorPath, 404, "Not Found");
                } else {
                    sendError(writer, out, 404, "Not Found");
                }
                return;
            }

            sendFile(writer, out, requestedPath, 200, "OK");
        }

        /**
         * Envía un archivo como respuesta HTTP
         */
        private void sendFile(PrintWriter writer, OutputStream out,
                              Path file, int code, String status) throws IOException {

            String mime = getMimeType(file);
            long size = Files.size(file);

            writer.print("HTTP/1.0 " + code + " " + status + "\r\n");
            writer.print("Date: " + new Date() + "\r\n");
            writer.print("Server: JavaHTTPServer/1.0\r\n");
            writer.print("Content-Type: " + mime + "\r\n");
            writer.print("Content-Length: " + size + "\r\n");
            writer.print("Connection: close\r\n");
            writer.print("\r\n");
            writer.flush();

            try (InputStream fileIn = Files.newInputStream(file)) {
                fileIn.transferTo(out);
            }

            out.flush();
            System.out.println("Respuesta " + code + " → " + file.getFileName());
        }

        /**
         * Respuesta de error genérica
         */
        private void sendError(PrintWriter writer, OutputStream out,
                               int code, String status) throws IOException {

            String body = """
                <html>
                <body style="font-family:Arial;text-align:center">
                    <h1>Error %d - %s</h1>
                </body>
                </html>
                """.formatted(code, status);

            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

            writer.print("HTTP/1.0 " + code + " " + status + "\r\n");
            writer.print("Content-Type: text/html; charset=UTF-8\r\n");
            writer.print("Content-Length: " + bytes.length + "\r\n");
            writer.print("Connection: close\r\n");
            writer.print("\r\n");
            writer.flush();

            out.write(bytes);
            out.flush();
        }

        /**
         * Tipos MIME
         */
        private String getMimeType(Path file) {
            String name = file.getFileName().toString().toLowerCase();
            if (name.endsWith(".html") || name.endsWith(".htm")) return "text/html; charset=UTF-8";
            if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
            if (name.endsWith(".gif")) return "image/gif";
            if (name.endsWith(".png")) return "image/png";
            if (name.endsWith(".css")) return "text/css";
            if (name.endsWith(".js")) return "application/javascript";
            return "application/octet-stream";
        }
    }
}
