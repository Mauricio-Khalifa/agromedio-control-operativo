package agromedio.logica;

import agromedio.datos.ClienteDAO;
import agromedio.datos.ClienteDAOSqlite;
import agromedio.datos.PlantillaDAO;
import agromedio.datos.PlantillaDAOSqlite;
import agromedio.datos.ProductoDAO;
import agromedio.datos.ProductoDAOSqlite;
import agromedio.modelo.Cliente;
import agromedio.modelo.DetallePlantilla;
import agromedio.modelo.PlantillaOperativa;
import agromedio.modelo.Producto;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Modulo 0 - Carga de instrucciones administrativas (RF01 y RF02).
 * Lee archivos .xlsx con columnas:
 * Cliente | Contacto | Telefono | Mercado | Fecha (aaaa-mm-dd) | Producto | Cantidad
 * y los transforma en plantillas operativas persistentes.
 */
public class CargaExcelServicio {

    private final ClienteDAO clienteDAO;
    private final ProductoDAO productoDAO;
    private final PlantillaDAO plantillaDAO;

    public CargaExcelServicio() {
        this(new ClienteDAOSqlite(), new ProductoDAOSqlite(), new PlantillaDAOSqlite());
    }

    public CargaExcelServicio(ClienteDAO clienteDAO, ProductoDAO productoDAO, PlantillaDAO plantillaDAO) {
        this.clienteDAO = clienteDAO;
        this.productoDAO = productoDAO;
        this.plantillaDAO = plantillaDAO;
    }

    public ResultadoCarga cargar(File archivo) throws Exception {
        ResultadoCarga res = new ResultadoCarga();
        if (archivo == null || !archivo.exists()) {
            res.getErrores().add("El archivo no existe.");
            return res;
        }

        Map<String, Producto> productos = new HashMap<>();
        for (Producto p : productoDAO.listar()) {
            productos.put(p.getNombre().trim().toLowerCase(Locale.ROOT), p);
        }
        Map<String, Cliente> clientes = new HashMap<>();
        for (Cliente cl : clienteDAO.listar()) {
            clientes.put(cl.getNombreEntidad().trim().toLowerCase(Locale.ROOT), cl);
        }

        // Agrupa las filas por (cliente, mercado, fecha) preservando el orden del archivo
        Map<String, PlantillaOperativa> plantillas = new LinkedHashMap<>();
        Map<String, List<DetallePlantilla>> detalles = new LinkedHashMap<>();

        try (Workbook wb = WorkbookFactory.create(archivo)) {
            Sheet hoja = wb.getSheetAt(0);
            if (hoja == null || hoja.getLastRowNum() < 1) {
                res.getErrores().add("El archivo no tiene datos.");
                return res;
            }
            String encabezado = textoCelda(hoja.getRow(0).getCell(0)).toLowerCase(Locale.ROOT);
            String encabezadoProducto = hoja.getRow(0).getCell(5) == null ? ""
                    : textoCelda(hoja.getRow(0).getCell(5)).toLowerCase(Locale.ROOT);
            if (!encabezado.contains("cliente") || !encabezadoProducto.contains("producto")) {
                res.getErrores().add("Formato no reconocido. Se esperaba: Cliente | Contacto | Telefono | Mercado | Fecha | Producto | Cantidad");
                return res;
            }

            DateTimeFormatter[] formatos = {
                    DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                    DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                    DateTimeFormatter.ISO_LOCAL_DATE
            };

            for (int i = 1; i <= hoja.getLastRowNum(); i++) {
                Row fila = hoja.getRow(i);
                if (fila == null) {
                    continue;
                }
                res.setLineasLeidas(res.getLineasLeidas() + 1);
                int linea = i + 1;

                String nombreCliente = textoCelda(fila.getCell(0)).trim();
                String contacto = textoCelda(fila.getCell(1)).trim();
                String telefono = textoCelda(fila.getCell(2)).trim();
                String mercado = textoCelda(fila.getCell(3)).trim();
                String textoFecha = textoCelda(fila.getCell(4)).trim();
                String nombreProducto = textoCelda(fila.getCell(5)).trim();
                double cantidad = numeroCelda(fila.getCell(6));

                if (nombreCliente.isEmpty() || mercado.isEmpty() || nombreProducto.isEmpty() || textoFecha.isEmpty()) {
                    res.getErrores().add("Linea " + linea + ": faltan datos obligatorios (cliente, mercado, fecha o producto).");
                    continue;
                }
                if (cantidad <= 0) {
                    res.getErrores().add("Linea " + linea + ": la cantidad debe ser mayor que cero.");
                    continue;
                }
                LocalDate fecha = parsearFecha(textoFecha, formatos);
                if (fecha == null) {
                    res.getErrores().add("Linea " + linea + ": fecha invalida '" + textoFecha + "' (use aaaa-mm-dd).");
                    continue;
                }
                Producto producto = productos.get(nombreProducto.toLowerCase(Locale.ROOT));
                if (producto == null) {
                    res.getErrores().add("Linea " + linea + ": el producto '" + nombreProducto + "' no existe en el catalogo.");
                    continue;
                }

                String clave = nombreCliente.toLowerCase(Locale.ROOT) + "|" + mercado.toLowerCase(Locale.ROOT)
                        + "|" + fecha;
                PlantillaOperativa plantilla = plantillas.get(clave);
                if (plantilla == null) {
                    plantilla = new PlantillaOperativa();
                    plantilla.setNombreMercado(mercado);
                    plantilla.setFechaProgramada(fecha.toString());
                    plantilla.setEstado(PlantillaOperativa.ESTADO_CARGADA);
                    plantillas.put(clave, plantilla);
                    detalles.put(clave, new ArrayList<>());

                    Cliente cliente = clientes.get(nombreCliente.toLowerCase(Locale.ROOT));
                    if (cliente == null) {
                        cliente = new Cliente(0, nombreCliente, contacto, telefono);
                        clienteDAO.guardar(cliente);
                        clientes.put(nombreCliente.toLowerCase(Locale.ROOT), cliente);
                    }
                    plantilla.setIdCliente(cliente.getIdCliente());
                }

                DetallePlantilla det = new DetallePlantilla();
                det.setIdProducto(producto.getIdProducto());
                det.setCantidadSolicitada(cantidad);
                detalles.get(clave).add(det);
            }
        }

        // Persistencia (RF02): transforma las filas en registros estructurados
        for (Map.Entry<String, PlantillaOperativa> e : plantillas.entrySet()) {
            PlantillaOperativa p = e.getValue();
            plantillaDAO.guardar(p);
            plantillasCreadas(res);
            for (DetallePlantilla d : detalles.get(e.getKey())) {
                d.setIdPlantilla(p.getIdPlantilla());
                plantillaDAO.agregarDetalle(d);
                res.setProductosRegistrados(res.getProductosRegistrados() + 1);
            }
        }
        return res;
    }

    private void plantillasCreadas(ResultadoCarga res) {
        res.setPlantillasCreadas(res.getPlantillasCreadas() + 1);
    }

    private static LocalDate parsearFecha(String texto, DateTimeFormatter[] formatos) {
        for (DateTimeFormatter f : formatos) {
            try {
                return LocalDate.parse(texto, f);
            } catch (DateTimeParseException ignore) {
                // intenta con el siguiente formato
            }
        }
        return null;
    }

    private static String textoCelda(Cell celda) {
        if (celda == null) {
            return "";
        }
        if (celda.getCellType() == CellType.NUMERIC) {
            if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(celda)) {
                return celda.getLocalDateTimeCellValue().toLocalDate().toString();
            }
            double d = celda.getNumericCellValue();
            if (d == Math.rint(d)) {
                return String.valueOf((long) d);
            }
            return String.valueOf(d);
        }
        return celda.toString();
    }

    private static double numeroCelda(Cell celda) {
        if (celda == null) {
            return 0;
        }
        if (celda.getCellType() == CellType.NUMERIC) {
            return celda.getNumericCellValue();
        }
        try {
            String t = celda.toString().trim().replace(',', '.');
            return t.isEmpty() ? 0 : Double.parseDouble(t);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
