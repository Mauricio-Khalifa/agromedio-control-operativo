package agromedio.presentacion;

import agromedio.datos.ConexionBD;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/** Punto de entrada de la aplicacion de escritorio. */
public class Aplicacion {

    public static void main(String[] args) {
        Tema.instalar();
        SwingUtilities.invokeLater(() -> {
            try {
                ConexionBD.inicializar();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "No se pudo inicializar la base de datos:\n" + e.getMessage(),
                        "Agromedio - Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
            FrmPrincipal principal = new FrmPrincipal();
            principal.setVisible(true);
            if (Boolean.getBoolean("agromedio.demo")) {
                principal.abrirDemostracion();
            }
        });
    }
}
