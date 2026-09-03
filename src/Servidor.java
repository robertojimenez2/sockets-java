import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Servidor central.
 * Debe correr en UNA de las máquinas de la VPN
 * Protocolo muy simple basado en texto (una línea = un mensaje):
 *   Cliente -> Servidor : "HELLO:<nombreMaquina>"   (al conectarse)
 *   Cliente -> Servidor : "HOLA"                    (cada vez que pulsa el botón)
 *   Servidor -> Cliente : "ESTADO:<total>;maq1=n1;maq2=n2;..."  (broadcast)
 */
public class Servidor {

    private static final int PUERTO = 5000;

    private static final Map<String, Integer> conteoPorMaquina = new ConcurrentHashMap<>();
    private static int totalGlobal = 0;
    private static final Object lock = new Object(); // protege totalGlobal

    private static final List<PrintWriter> clientesSalida =
            Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) throws IOException {
        ServerSocket servidor = new ServerSocket(PUERTO);
        System.out.println("Servidor escuchando en el puerto " + PUERTO + " ...");

        while (true) {
            Socket socket = servidor.accept();
            System.out.println("Nueva conexión desde: " + socket.getInetAddress());
            new Thread(() -> atenderCliente(socket)).start();
        }
    }

    private static void atenderCliente(Socket socket) {
        String nombreMaquina = null;
        PrintWriter salida = null;
        try {
            BufferedReader entrada = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            salida = new PrintWriter(socket.getOutputStream(), true);
            clientesSalida.add(salida);

            String linea;
            while ((linea = entrada.readLine()) != null) {

                if (linea.startsWith("HELLO:")) {
                    nombreMaquina = linea.substring("HELLO:".length()).trim();
                    conteoPorMaquina.putIfAbsent(nombreMaquina, 0);
                    System.out.println(nombreMaquina + " se identificó.");
                    broadcastEstado();

                } else if (linea.equals("HOLA") && nombreMaquina != null) {
                    synchronized (lock) {
                        totalGlobal++;
                        conteoPorMaquina.merge(nombreMaquina, 1, Integer::sum);
                    }
                    System.out.println("\"Hola\" recibido de " + nombreMaquina
                            + " (total global: " + totalGlobal + ")");
                    broadcastEstado();
                }
            }
        } catch (IOException e) {
            System.out.println("Se desconectó: " + nombreMaquina);
        } finally {
            if (salida != null) clientesSalida.remove(salida);
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private static void broadcastEstado() {
        StringBuilder sb = new StringBuilder("ESTADO:");
        synchronized (lock) {
            sb.append(totalGlobal);
            for (Map.Entry<String, Integer> e : conteoPorMaquina.entrySet()) {
                sb.append(";").append(e.getKey()).append("=").append(e.getValue());
            }
        }
        String mensaje = sb.toString();

        synchronized (clientesSalida) {
            for (PrintWriter pw : clientesSalida) {
                pw.println(mensaje);
            }
        }
    }
}