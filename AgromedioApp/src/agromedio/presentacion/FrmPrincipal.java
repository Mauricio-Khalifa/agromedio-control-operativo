package agromedio.presentacion;

import agromedio.datos.ConexionBD;

import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionListener;

/** Ventana principal del Sistema de Control Operativo Agromedio. */
public class FrmPrincipal extends JFrame {

    private final JDesktopPane escritorio = new JDesktopPane();
    private int abiertas = 0;

    public FrmPrincipal() {
        super("Sistema de Control Operativo AGROMEDIO");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1150, 720));
        setLayout(new BorderLayout());

        setJMenuBar(crearMenu());
        add(escritorio, BorderLayout.CENTER);

        JPanel barra = new JPanel(new BorderLayout());
        barra.setBorder(new EmptyBorder(4, 10, 4, 10));
        JLabel lblBase = new JLabel("Base de datos: " + ConexionBD.rutaBD()
                + "   |   Asociacion Agropecuaria y Ambiental del Magdalena Medio");
        barra.add(lblBase, BorderLayout.WEST);
        add(barra, BorderLayout.SOUTH);

        setLocationRelativeTo(null);
    }

    private JMenuBar crearMenu() {
        JMenuBar barra = new JMenuBar();

        JMenu mCatalogos = new JMenu("Catalogos");
        mCatalogos.add(item("Clientes", e -> abrir(new FrmCatalogos(new CatalogoClientes()))));
        mCatalogos.add(item("Categorias", e -> abrir(new FrmCatalogos(new CatalogoCategorias()))));
        mCatalogos.add(item("Unidades de medida", e -> abrir(new FrmCatalogos(new CatalogoUnidades()))));
        mCatalogos.add(item("Proveedores", e -> abrir(new FrmCatalogos(new CatalogoProveedores()))));
        mCatalogos.add(item("Productos", e -> abrir(new FrmProductos())));

        JMenu mOperacion = new JMenu("Operacion");
        mOperacion.add(item("Plantillas operativas", e -> abrir(new FrmPlantillas())));
        mOperacion.add(item("Carga desde Excel (Modulo 0)", e -> abrir(new FrmCargaExcel())));
        mOperacion.add(item("Compras (Modulo 1)", e -> abrir(new FrmCompras())));
        mOperacion.add(item("Recepcion (Modulo 2)", e -> abrir(new FrmRecepcion())));
        mOperacion.add(item("Alistamiento / Produccion (Modulo 3)", e -> abrir(new FrmAlistamiento())));
        mOperacion.add(item("Despacho (Modulo 4)", e -> abrir(new FrmDespacho())));

        JMenu mConsultas = new JMenu("Consultas y reportes");
        mConsultas.add(item("Reportes operativos", e -> abrir(new FrmReportes())));

        JMenu mAyuda = new JMenu("Ayuda");
        mAyuda.add(item("Acerca del sistema", e -> UiUtil.mensaje(this,
                "Sistema de Control Operativo Agromedio\n"
                        + "Proyecto Integrador - UT Santander 2026\n"
                        + "Materias: Planeacion de Sistemas Informaticos, "
                        + "Motores de Bases de Datos y Programacion Orientada a Objetos.\n"
                        + "Base de datos: SQLite (" + ConexionBD.rutaBD() + ")")));

        barra.add(mCatalogos);
        barra.add(mOperacion);
        barra.add(mConsultas);
        barra.add(mAyuda);
        return barra;
    }

    private JMenuItem item(String texto, ActionListener accion) {
        JMenuItem i = new JMenuItem(texto);
        i.setFont(UiUtil.FUENTE);
        i.addActionListener(accion);
        return i;
    }

    private void abrir(JInternalFrame frame) {
        escritorio.add(frame);
        abiertas++;
        Dimension d = escritorio.getSize();
        int desplaz = 28 * ((abiertas - 1) % 8);
        if (d.width > 0 && d.height > 0) {
            if (frame.getWidth() > d.width - 8) {
                frame.setSize(d.width - 8, frame.getHeight());
            }
            if (frame.getHeight() > d.height - 8) {
                frame.setSize(frame.getWidth(), d.height - 8);
            }
            desplaz = Math.min(desplaz, Math.max(0, d.width - frame.getWidth() - 8));
            desplaz = Math.min(desplaz, Math.max(0, d.height - frame.getHeight() - 8));
        }
        frame.setLocation(desplaz, desplaz);
        frame.setVisible(true);
        try {
            frame.setSelected(true);
        } catch (java.beans.PropertyVetoException ignore) {
            // la ventana igual queda visible
        }
    }

    /** Abre ventanas de ejemplo (uso: capturas de pantalla y demostracion). */
    public void abrirDemostracion() {
        SwingUtilities.invokeLater(() -> {
            abrir(new FrmPlantillas());
            abrir(new FrmCompras());
            abrir(new FrmReportes());
        });
    }
}
