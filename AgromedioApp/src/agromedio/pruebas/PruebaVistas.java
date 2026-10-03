package agromedio.pruebas;

import agromedio.datos.ConexionBD;
import agromedio.presentacion.FrmAlistamiento;
import agromedio.presentacion.FrmCargaExcel;
import agromedio.presentacion.FrmCatalogos;
import agromedio.presentacion.FrmCompras;
import agromedio.presentacion.FrmDespacho;
import agromedio.presentacion.FrmDetallePlantilla;
import agromedio.presentacion.FrmProductos;
import agromedio.presentacion.FrmPlantillas;
import agromedio.presentacion.FrmRecepcion;
import agromedio.presentacion.FrmReportes;
import agromedio.presentacion.CatalogoCategorias;
import agromedio.presentacion.CatalogoClientes;
import agromedio.presentacion.CatalogoProveedores;
import agromedio.presentacion.CatalogoUnidades;

import javax.swing.JInternalFrame;
import javax.swing.SwingUtilities;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Construye todas las ventanas del sistema para verificar que cargan sin error.
 * Ejecutar con entorno grafico disponible.
 */
public class PruebaVistas {

    private static int total = 0;
    private static int fallos = 0;

    public static void main(String[] args) throws Exception {
        Path bd = Paths.get("build", "prueba_vistas.db");
        Files.createDirectories(bd.toAbsolutePath().getParent());
        Files.deleteIfExists(bd);
        ConexionBD.fijarRuta(bd.toString());
        ConexionBD.inicializar();

        List<java.util.function.Supplier<JInternalFrame>> fabricas = List.of(
                () -> new FrmCatalogos(new CatalogoClientes()),
                () -> new FrmCatalogos(new CatalogoCategorias()),
                () -> new FrmCatalogos(new CatalogoUnidades()),
                () -> new FrmCatalogos(new CatalogoProveedores()),
                FrmProductos::new,
                FrmPlantillas::new,
                () -> new FrmDetallePlantilla(1),
                FrmCargaExcel::new,
                FrmCompras::new,
                FrmRecepcion::new,
                FrmAlistamiento::new,
                FrmDespacho::new,
                FrmReportes::new
        );

        List<String> nombres = List.of("Catalogo clientes", "Catalogo categorias", "Catalogo unidades",
                "Catalogo proveedores", "Productos", "Plantillas", "Detalle plantilla", "Carga Excel",
                "Compras", "Recepcion", "Alistamiento", "Despacho", "Reportes");

        for (int i = 0; i < fabricas.size(); i++) {
            final int idx = i;
            AtomicReference<Throwable> error = new AtomicReference<>();
            SwingUtilities.invokeAndWait(() -> {
                JInternalFrame f = null;
                try {
                    f = fabricas.get(idx).get();
                    f.setVisible(true);
                } catch (Throwable t) {
                    error.set(t);
                } finally {
                    if (f != null) {
                        f.dispose();
                    }
                }
            });
            verificar("Ventana " + nombres.get(idx), () -> error.get() == null);
        }

        System.out.println();
        System.out.println("RESULTADO: " + (total - fallos) + "/" + total + " ventanas construidas sin error");
        if (fallos > 0) {
            System.exit(1);
        }
    }

    private interface Prueba { boolean ok() throws Exception; }

    private static void verificar(String nombre, Prueba p) {
        total++;
        try {
            if (p.ok()) {
                System.out.println("[OK]   " + nombre);
            } else {
                fallos++;
                System.out.println("[FAIL] " + nombre);
            }
        } catch (Exception e) {
            fallos++;
            System.out.println("[FAIL] " + nombre + " -> " + e);
        }
    }
}
