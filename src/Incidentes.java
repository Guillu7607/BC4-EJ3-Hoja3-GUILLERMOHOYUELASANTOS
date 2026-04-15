import java.sql.*;
import java.util.Scanner;

public class Incidentes {
    private static final String URL = "jdbc:oracle:thin:@localhost:1521:xe";
    private static final String USER = "RIBERA";
    private static final String PASS = "ribera";
    private static final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            int opcion;
            do {
                System.out.println("\n--- MENÚ GESTIÓN INCIDENCIAS ---");
                System.out.println("1. Insertar nueva incidencia");
                System.out.println("2. Mostrar todas las incidencias (JOIN)");
                System.out.println("3. Mostrar incidencias de un ciclista");
                System.out.println("4. Salir");
                System.out.print("Seleccione una opción: ");
                opcion = sc.nextInt();

                switch (opcion) {
                    case 1 -> insertarIncidencia(conn);
                    case 2 -> mostrarTodasIncidencias(conn);
                    case 3 -> mostrarPorCiclista(conn);
                    case 4 -> System.out.println("Saliendo del programa...");
                    default -> System.out.println("Opción no válida.");
                }
            } while (opcion != 4);

        } catch (SQLException e) {
            System.err.println("Error de conexión: " + e.getMessage());
        }
    }

    // A) INSERTAR UNA NUEVA INCIDENCIA
    private static void insertarIncidencia(Connection conn) {
        try {
            // Iniciamos transacción manual
            conn.setAutoCommit(false);

            System.out.print("ID del ciclista: ");
            int id = sc.nextInt();
            System.out.print("Número de etapa: ");
            int etapa = sc.nextInt();
            sc.nextLine(); // limpiar buffer
            System.out.print("Tipo de incidencia: ");
            String tipo = sc.nextLine();

            // 1. Verificar existencia
            if (!existeRegistro(conn, "SELECT 1 FROM CICLISTA WHERE ID_CICLISTA = ?", id)) {
                System.out.println("Error: El ciclista no existe.");
                return;
            }
            if (!existeRegistro(conn, "SELECT 1 FROM ETAPA WHERE NUMERO = ?", etapa)) {
                System.out.println("Error: La etapa no existe.");
                return;
            }

            // 2. Insertar
            String sql = "INSERT INTO INCIDENCIA (ID_CICLISTA, NUMERO_ETAPA, TIPO) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                ps.setInt(2, etapa);
                ps.setString(3, tipo);
                ps.executeUpdate();

                conn.commit(); // Todo correcto
                System.out.println("Incidencia registrada correctamente.");
            }

        } catch (SQLException e) {
            try {
                conn.rollback(); // Algo falló
                System.err.println("Error en la inserción. Se ha realizado ROLLBACK: " + e.getMessage());
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }
    // B) MOSTRAR TODAS LAS INCIDENCIAS (JOIN)
    private static void mostrarTodasIncidencias(Connection conn) {
        String sql = "SELECT i.ID_INCIDENCIA, c.NOMBRE, i.NUMERO_ETAPA, i.TIPO " +
                "FROM INCIDENCIA i JOIN CICLISTA c ON i.ID_CICLISTA = c.ID_CICLISTA";

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            System.out.println("\n--- LISTADO GENERAL ---");
            while (rs.next()) {
                System.out.printf("[%d] %s – Etapa %d – %s%n",
                        rs.getInt("ID_INCIDENCIA"),
                        rs.getString("NOMBRE"),
                        rs.getInt("NUMERO_ETAPA"),
                        rs.getString("TIPO"));
            }
        } catch (SQLException e) {
            System.err.println("Error al consultar: " + e.getMessage());
        }
    }

    // C) MOSTRAR INCIDENCIAS DE UN CICLISTA CONCRETO
    private static void mostrarPorCiclista(Connection conn) {
        System.out.print("Introduzca el ID del ciclista: ");
        int id = sc.nextInt();

        String sql = "SELECT NUMERO_ETAPA, TIPO FROM INCIDENCIA " +
                "WHERE ID_CICLISTA = ? ORDER BY NUMERO_ETAPA";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                System.out.println("\nIncidencias del ciclista " + id + ":");
                boolean hayDatos = false;
                while (rs.next()) {
                    hayDatos = true;
                    System.out.println("- Etapa " + rs.getInt(1) + ": " + rs.getString(2));
                }
                if (!hayDatos) System.out.println("No se encontraron incidencias.");
            }
        } catch (SQLException e) {
            System.err.println("Error al consultar: " + e.getMessage());
        }
    }

    // Método auxiliar para verificar existencia
    private static boolean existeRegistro(Connection conn, String sql, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}