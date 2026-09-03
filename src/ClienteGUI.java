import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.net.*;

/**
 * Cliente con interfaz visual (Swing).
 * Se ejecuta en CADA máquina que participa (incluida, si se quiere, la del servidor).
 */
public class ClienteGUI extends JFrame {

    private static final String IP_SERVIDOR = "100.81.20.65"; // IP de Tailscale del servidor
    private static final int PUERTO = 5000;                // debe coincidir con Servidor.java
    private static final String NOMBRE_MAQUINA = "PC-G";   // nombre único para esta máquina

    private PrintWriter salida;
    private BufferedReader entrada;
    private JLabel totalLabel;
    private DefaultTableModel modeloTabla;

    public ClienteGUI(String ipServidor, int puerto, String nombreMaquina) {
        super("Cliente Hola - " + nombreMaquina);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(420, 320);
        setLayout(new BorderLayout(10, 10));

        JButton botonHola = new JButton("Decir \u00a1Hola!");
        botonHola.setFont(new Font("Arial", Font.BOLD, 20));

        totalLabel = new JLabel("Total global: 0", SwingConstants.CENTER);
        totalLabel.setFont(new Font("Arial", Font.BOLD, 16));

        modeloTabla = new DefaultTableModel(new Object[]{"Máquina", "Veces dicho"}, 0);
        JTable tabla = new JTable(modeloTabla);

        add(totalLabel, BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(botonHola, BorderLayout.SOUTH);

        // Al pulsar el botón, se envía "HOLA" al servidor. El servidor
        // se encarga de sumar y reenviar el nuevo total a TODOS los clientes.
        botonHola.addActionListener(e -> {
            if (salida != null) salida.println("HOLA");
        });

        conectar(ipServidor, puerto, nombreMaquina);

        setVisible(true);
    }

    private void conectar(String ipServidor, int puerto, String nombreMaquina) {
        try {
            Socket socket = new Socket(ipServidor, puerto);
            salida = new PrintWriter(socket.getOutputStream(), true);
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            salida.println("HELLO:" + nombreMaquina);

            // Hilo separado que escucha continuamente al servidor,
            // sin bloquear la interfaz gráfica (así se actualiza en paralelo
            // con lo que hacen las demás máquinas).
            new Thread(this::escucharServidor).start();

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo conectar al servidor " + ipServidor + ":" + puerto
                            + "\n¿Está el servidor corriendo? ¿Hace ping la VPN?");
            System.exit(1);
        }
    }

    private void escucharServidor() {
        try {
            String linea;
            while ((linea = entrada.readLine()) != null) {
                if (linea.startsWith("ESTADO:")) {
                    procesarEstado(linea.substring("ESTADO:".length()));
                }
            }
        } catch (IOException e) {
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this, "Se perdió la conexión con el servidor."));
        }
    }

    private void procesarEstado(String datos) {
        String[] partes = datos.split(";");
        int total = Integer.parseInt(partes[0]);

        SwingUtilities.invokeLater(() -> {
            totalLabel.setText("Total global: " + total);
            modeloTabla.setRowCount(0);
            for (int i = 1; i < partes.length; i++) {
                String[] par = partes[i].split("=");
                modeloTabla.addRow(new Object[]{par[0], par[1]});
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ClienteGUI(IP_SERVIDOR, PUERTO, NOMBRE_MAQUINA));
    }
}