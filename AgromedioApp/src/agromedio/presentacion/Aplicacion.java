package agromedio.presentacion;

import agromedio.datos.ConexionBD;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Punto de entrada de la aplicacion de escritorio. */
public class Aplicacion {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignore) {
            // se mantiene el Look and Feel por defecto
        }
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
