package agromedio.logica;

import agromedio.datos.CargaPlantillaDAO;
import agromedio.datos.CargaPlantillaDAOSqlite;
import agromedio.datos.ClienteDAO;
import agromedio.datos.ClienteDAOSqlite;
import agromedio.datos.ConexionBD;
import agromedio.datos.PlantillaDAO;
import agromedio.datos.PlantillaDAOSqlite;
import agromedio.datos.ProductoDAO;
import agromedio.datos.ProductoDAOSqlite;
import agromedio.modelo.CargaPlantilla;
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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Modulo 0 - Carga de instrucciones administrativas (RF01 y RF02).
 * Lee archivos .xlsx/.xls y archivos planos .csv/.txt con columnas:
 * Cliente | Contacto | Telefono | Mercado | Fecha (aaaa-mm-dd) | Producto | Cantidad
 * y los transforma en plantillas operativas persistentes.
 */
public class CargaExcelServicio {

    private static final DateTimeFormatter[] FORMATOS_FECHA = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ISO_LOCAL_DATE,
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("d-MMM-yyyy").toFormatter(new Locale("es")),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("d-MMM-yyyy").toFormatter(Locale.ENGLISH)
    };

    /** Acepta cantidades numericas o descriptivas: "9 unidades", "1 de 300 gr", "400 gramos". */
    private static final Pattern PATRON_CANTIDAD =
            Pattern.compile("^([+-]?\\d+(?:[.,]\\d+)?)\\s*(.*)$");

    private final ClienteDAO clienteDAO;
    private final ProductoDAO productoDAO;
    private final PlantillaDAO plantillaDAO;
    private final CargaPlantillaDAO cargaDAO;

    public CargaExcelServicio() {
        this(new ClienteDAOSqlite(), new ProductoDAOSqlite(), new PlantillaDAOSqlite(),
                new CargaPlantillaDAOSqlite());
    }

    public CargaExcelServicio(ClienteDAO clienteDAO, ProductoDAO productoDAO, PlantillaDAO plantillaDAO) {
        this(clienteDAO, productoDAO, plantillaDAO, new CargaPlantillaDAOSqlite());
    }

    public CargaExcelServicio(ClienteDAO clienteDAO, ProductoDAO productoDAO,
                              PlantillaDAO plantillaDAO, CargaPlantillaDAO cargaDAO) {
        this.clienteDAO = clienteDAO;
        this.productoDAO = productoDAO;
        this.plantillaDAO = plantillaDAO;
        this.cargaDAO = cargaDAO;
    }

    public ResultadoCarga cargar(File archivo) throws Exception {
        ResultadoCarga res = new ResultadoCarga();
        if (archivo == null || !archivo.exists()) {
            res.getErrores().add("El archivo no existe.");
            return res;
        }

        String hash = hashSha256(archivo);
        CargaPlantilla previa = cargaDAO.buscarPorHash(hash);
        if (previa != null) {
            final int idCargaPrevia = previa.getIdCarga();
            // Si todas las plantillas de esa carga fueron eliminadas, el registro
            // es huerfano: se limpia y se permite recargar el archivo.
            boolean quedanPlantillas = plantillaDAO.listar().stream()
                    .anyMatch(p -> p.getIdCarga() == idCargaPrevia);
            if (!quedanPlantillas) {
                cargaDAO.eliminar(idCargaPrevia);
                previa = null;
            }
        }
        if (previa != null) {
            res.setDuplicada(true);
            res.getErrores().add("Carga ya realizada: el archivo '" + archivo.getName()
                    + "' ya fue cargado el " + previa.getFechaCarga()
                    + " (carga #" + previa.getIdCarga() + ").");
            return res;
        }

        Map<String, Producto> productos = new HashMap<>();
        for (Producto p : productoDAO.listar()) {
            productos.put(claveProducto(p.getNombre()), p);
        }
        Map<String, Cliente> clientes = new HashMap<>();
        for (Cliente cl : clienteDAO.listar()) {
            clientes.put(clave(cl.getNombreEntidad()), cl);
        }

        // Agrupa las filas por (cliente, mercado, fecha) preservando el orden del archivo
        Map<String, PlantillaOperativa> plantillas = new LinkedHashMap<>();
        Map<String, List<DetallePlantilla>> detalles = new LinkedHashMap<>();

        if (esArchivoPlano(archivo)) {
            cargarPlano(archivo, res, productos, clientes, plantillas, detalles);
        } else {
            cargarLibroExcel(archivo, res, productos, clientes, plantillas, detalles);
        }
        persistir(archivo, hash, res, plantillas, detalles);
        return res;
    }

    /** Lee un libro Excel y transforma sus filas al modelo comun. */
    private void cargarLibroExcel(File archivo, ResultadoCarga res,
                                  Map<String, Producto> productos, Map<String, Cliente> clientes,
                                  Map<String, PlantillaOperativa> plantillas,
                                  Map<String, List<DetallePlantilla>> detalles) throws Exception {
        // Se lee por flujo para que Apache POI no reescriba el archivo y cambie su hash.
        try (InputStream entrada = Files.newInputStream(archivo.toPath());
             Workbook wb = WorkbookFactory.create(entrada)) {
            Sheet hoja = wb.getSheetAt(0);
            if (hoja == null || hoja.getLastRowNum() < 1) {
                res.getErrores().add("El archivo no tiene datos.");
                return;
            }
            String encabezado = textoCelda(hoja.getRow(0).getCell(0)).toLowerCase(Locale.ROOT);
            String encabezadoProducto = hoja.getRow(0).getCell(6) == null ? ""
                    : textoCelda(hoja.getRow(0).getCell(6)).toLowerCase(Locale.ROOT);
            if (!encabezado.contains("cliente") || !encabezadoProducto.contains("producto")) {
                res.getErrores().add(mensajeFormato());
                return;
            }

            for (int i = 1; i <= hoja.getLastRowNum(); i++) {
                Row fila = hoja.getRow(i);
                if (fila == null) {
                    continue;
                }
                res.setLineasLeidas(res.getLineasLeidas() + 1);
                procesarFila(res, i + 1,
                        textoCelda(fila.getCell(0)).trim(),
                        textoCelda(fila.getCell(1)).trim(),
                        textoCelda(fila.getCell(2)).trim(),
                        textoCelda(fila.getCell(3)).trim(),
                        textoCelda(fila.getCell(4)).trim(),
                        textoCelda(fila.getCell(5)).trim(),
                        textoCelda(fila.getCell(6)).trim(),
                         textoCelda(fila.getCell(7)).trim(),
                         textoCelda(fila.getCell(8)).trim(),
                        productos, clientes, plantillas, detalles);
            }
        }
    }

    /** Lee CSV/TXT delimitado por coma, punto y coma o tabulador. */
    private void cargarPlano(File archivo, ResultadoCarga res,
                             Map<String, Producto> productos, Map<String, Cliente> clientes,
                             Map<String, PlantillaOperativa> plantillas,
                             Map<String, List<DetallePlantilla>> detalles) throws Exception {
        List<String> lineas = Files.readAllLines(archivo.toPath(), StandardCharsets.UTF_8);
        if (lineas.isEmpty()) {
            res.getErrores().add("El archivo no tiene datos.");
            return;
        }
        lineas.set(0, quitarBom(lineas.get(0)));
        char separador = detectarSeparador(lineas.get(0));
        List<String[]> filas = new ArrayList<>();
        for (String linea : lineas) {
            if (linea != null && !linea.trim().isEmpty()) {
                filas.add(parsearLineaPlana(linea, separador));
            }
        }
        if (filas.size() < 2) {
            res.getErrores().add("El archivo no tiene datos.");
            return;
        }
        String[] encabezado = filas.get(0);
        if (encabezado.length < 7
                || !valor(encabezado, 0).toLowerCase(Locale.ROOT).contains("cliente")
                || !valor(encabezado, 6).toLowerCase(Locale.ROOT).contains("producto")) {
            res.getErrores().add(mensajeFormato());
            return;
        }

        for (int i = 1; i < filas.size(); i++) {
            String[] fila = filas.get(i);
            res.setLineasLeidas(res.getLineasLeidas() + 1);
            procesarFila(res, i + 1,
                    valor(fila, 0), valor(fila, 1), valor(fila, 2), valor(fila, 3),
                    valor(fila, 4), valor(fila, 5), valor(fila, 6), valor(fila, 7), valor(fila, 8),
                    productos, clientes, plantillas, detalles);
        }
    }

    /** Valida una fila y la agrega al agrupamiento por mercado/fecha. */
    private void procesarFila(ResultadoCarga res, int linea,
                              String nombreCliente, String contacto, String telefono,
                              String mercado, String textoMercados, String textoFecha, String nombreProducto,
                              String textoCantidad, String unidadSolicitada, Map<String, Producto> productos,
                              Map<String, Cliente> clientes,
                              Map<String, PlantillaOperativa> plantillas,
                              Map<String, List<DetallePlantilla>> detalles) {
        nombreCliente = nombreCliente.trim();
        contacto = contacto.trim();
        telefono = telefono.trim();
        mercado = mercado.trim();
        textoMercados = textoMercados.trim();
        textoFecha = textoFecha.trim();
        nombreProducto = nombreProducto.trim();
        unidadSolicitada = unidadSolicitada == null ? "" : unidadSolicitada.trim();

        if (nombreCliente.isEmpty() || mercado.isEmpty() || nombreProducto.isEmpty() || textoFecha.isEmpty()) {
            res.getErrores().add("Linea " + linea + ": faltan datos obligatorios (cliente, mercado, fecha o producto).");
            return;
        }

        int cantidadMercados;
        try {
            cantidadMercados = Integer.parseInt(textoMercados);
        } catch (NumberFormatException e) {
            res.getErrores().add("Linea " + linea + ": Cant. Mercados debe ser un entero mayor que cero.");
            return;
        }
        if (cantidadMercados <= 0) {
            res.getErrores().add("Linea " + linea + ": Cant. Mercados debe ser mayor que cero.");
            return;
        }
        CantidadArchivo cantidad = interpretarCantidadNueva(textoCantidad, unidadSolicitada, nombreProducto);
        if (cantidad == null) {
            res.getErrores().add("Linea " + linea + ": cantidad invalida '" + textoCantidad.trim()
                    + "' (use un numero o un texto como '9 unidades', '1 de 300 gr' o '400 gramos').");
            return;
        }
        if (cantidad.valor <= 0) {
            res.getErrores().add("Linea " + linea + ": la cantidad debe ser mayor que cero.");
            return;
        }
        nombreProducto = cantidad.nombreProducto;

        LocalDate fecha = parsearFecha(textoFecha, FORMATOS_FECHA);
        if (fecha == null) {
            res.getErrores().add("Linea " + linea + ": fecha invalida '" + textoFecha
                    + "' (use aaaa-mm-dd o dd/mm/aaaa).");
            return;
        }
        Producto producto = productos.get(claveProducto(nombreProducto));
        if (producto == null) {
            producto = new Producto(0, nombreProducto, categoriaPara(nombreProducto),
                    cantidad.idUnidad, cantidad.idUnidad, 1.0);
            productoDAO.guardar(producto);
            productos.put(claveProducto(nombreProducto), producto);
            res.setProductosCreados(res.getProductosCreados() + 1);
        }

        String clavePlantilla = clave(nombreCliente) + "|" + clave(mercado) + "|" + fecha;
        PlantillaOperativa plantilla = plantillas.get(clavePlantilla);
        if (plantilla == null) {
            plantilla = new PlantillaOperativa();
            plantilla.setNombreMercado(mercado);
            plantilla.setFechaProgramada(fecha.toString());
            plantilla.setEstado(PlantillaOperativa.ESTADO_CARGADA);
            plantilla.setCantidadMercados(cantidadMercados);
            plantillas.put(clavePlantilla, plantilla);
            detalles.put(clavePlantilla, new ArrayList<>());

            Cliente cliente = clientes.get(clave(nombreCliente));
            if (cliente == null) {
                cliente = new Cliente(0, nombreCliente, contacto, telefono);
                clienteDAO.guardar(cliente);
                clientes.put(clave(nombreCliente), cliente);
            }
            plantilla.setIdCliente(cliente.getIdCliente());
        }

        // C-2: si el producto ya esta en la plantilla del archivo, se acumula la
        // cantidad en una sola linea (evita detalle duplicado que infla el saldo).
        List<DetallePlantilla> listaDetalle = detalles.get(clavePlantilla);
        for (DetallePlantilla existente : listaDetalle) {
            if (existente.getIdProducto() == producto.getIdProducto()) {
                // Una fila repetida no se suma: el archivo debe contener una sola
                // solicitud por producto y presentación.
                return;
            }
        }
        DetallePlantilla det = new DetallePlantilla();
        det.setIdProducto(producto.getIdProducto());
        det.setCantidadSolicitada(Math.round(cantidad.valor * cantidadMercados * 1000.0) / 1000.0);
        listaDetalle.add(det);
    }

    /** Persiste la carga y sus plantillas (RF02 / G05). */
    private void persistir(File archivo, String hash, ResultadoCarga res,
                           Map<String, PlantillaOperativa> plantillas,
                           Map<String, List<DetallePlantilla>> detalles) {
        // C-3: el registro de auditoria (G05), las plantillas y su detalle se
        // guardan en una sola transaccion; si algo falla no queda carga parcial.
        ConexionBD.enTransaccion(() -> {
            if (!plantillas.isEmpty()) {
                int totalProductos = detalles.values().stream().mapToInt(List::size).sum();
                CargaPlantilla carga = new CargaPlantilla();
                carga.setNombreArchivo(archivo.getName());
                carga.setHashArchivo(hash);
                carga.setUsuario(System.getProperty("user.name", "OPERADOR"));
                carga.setPlantillasCreadas(plantillas.size());
                carga.setProductosRegistrados(totalProductos);
                int idCarga = cargaDAO.guardar(carga);
                res.setIdCarga(idCarga);
                for (PlantillaOperativa p : plantillas.values()) {
                    p.setIdCarga(idCarga);
                }
            }

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
        });
    }

    private static boolean esArchivoPlano(File archivo) {
        String nombre = archivo.getName().toLowerCase(Locale.ROOT);
        return nombre.endsWith(".csv") || nombre.endsWith(".txt");
    }

    private static String mensajeFormato() {
        return "Formato no reconocido. Se esperaba: Cliente | Contacto | Telefono | Mercado | Fecha | Producto | Cantidad";
    }

    private static String valor(String[] fila, int indice) {
        return indice < fila.length && fila[indice] != null ? fila[indice].trim() : "";
    }

    /**
     * Interpreta la columna Cantidad. Acepta numeros y expresiones habituales
     * de la plantilla administrativa: "9 unidades", "1 de 300 gr",
     * "2 de 1000 ml", "1 libra", "0.25 libra" o "400 gramos".
     */
    private static CantidadArchivo interpretarCantidadNueva(String texto, String unidad, String nombreProducto) {
        String u = normalizar(unidad);
        String canon = u.equals("libras") || u.equals("lb") ? "libra" :
                u.equals("gramos") || u.equals("gramo") || u.equals("gr") ? "gr" :
                u.equals("mililitros") || u.equals("mililitro") || u.equals("ml") ? "ml" :
                u.equals("unidades") || u.equals("unidad") || u.equals("und") ? "unidad" : u;
        if (canon.isEmpty()) return interpretarCantidad(texto, nombreProducto);
        double valor;
        try { valor = Double.parseDouble(texto.trim().replace(',', '.')); }
        catch (NumberFormatException e) { return null; }
        String presentacion = nombreProducto + " (" + texto.trim() + " " + canon + ")";
        int idUnidad = canon.equals("gr") ? 9 : canon.equals("ml") ? 10 :
                canon.equals("libra") ? 3 : canon.equals("unidad") ? 6 : 6;
        return new CantidadArchivo(valor, idUnidad, presentacion);
    }

    private static CantidadArchivo interpretarCantidad(String texto, String nombreProducto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.trim().replace(',', '.');
        if (limpio.isEmpty()) {
            return null;
        }
        Matcher m = PATRON_CANTIDAD.matcher(limpio);
        if (!m.matches()) {
            return null;
        }
        double valor;
        try {
            valor = Double.parseDouble(m.group(1));
        } catch (NumberFormatException e) {
            return null;
        }

        String resto = m.group(2).trim();
        String restoNormalizado = normalizar(resto);
        String producto = nombreProducto.trim();
        int unidad;

        if (restoNormalizado.startsWith("de ")) {
            // "1 de 300 gr": una presentacion de 300 gr, no 300 unidades.
            producto = producto + " (" + resto.substring(3).trim() + ")";
            unidad = 6; // Unidad
        } else if (restoNormalizado.contains("gramo") || restoNormalizado.matches(".*\\bgr\\b.*")) {
            valor = valor / 1000.0;
            unidad = 2; // Kilogramo
        } else if (restoNormalizado.contains("mililitro") || restoNormalizado.matches(".*\\bml\\b.*")) {
            valor = valor / 1000.0;
            unidad = 1; // Litro
        } else if (restoNormalizado.contains("litro") || restoNormalizado.matches(".*\\bl\\b.*")) {
            unidad = 1; // Litro
        } else if (restoNormalizado.contains("libra") || restoNormalizado.matches(".*\\blb\\b.*")) {
            unidad = 3; // Libra
        } else if (restoNormalizado.contains("kilo") || restoNormalizado.matches(".*\\bkg\\b.*")) {
            unidad = 2; // Kilogramo
        } else if (restoNormalizado.contains("quintal")) {
            unidad = 7;
        } else if (restoNormalizado.contains("bulto")) {
            unidad = 4;
        } else if (restoNormalizado.contains("caja")) {
            unidad = 5;
        } else if (restoNormalizado.contains("galon")) {
            unidad = 8;
        } else if (restoNormalizado.contains("docena")) {
            valor = valor * 12.0;
            unidad = 6;
        } else {
            unidad = 6; // Unidad: "9 unidades", "1 rama", "1 unidad"
        }

        valor = Math.round(valor * 1000.0) / 1000.0;
        return new CantidadArchivo(valor, unidad, producto);
    }

    private static int categoriaPara(String nombreProducto) {
        String n = normalizar(nombreProducto);
        if (n.contains("leche") || n.contains("queso") || n.contains("yogurt")) {
            return 1; // Lacteos
        }
        if (n.contains("carne") || n.contains("pollo") || n.contains("pechuga")
                || n.contains("higado") || n.contains("filete") || n.contains("basa")) {
            return 3; // Carnes y proteinas
        }
        if (n.contains("huevo")) {
            return 7; // Huevos
        }
        if (n.contains("arroz") || n.contains("avena") || n.contains("granola") || n.contains("harina")
                || n.contains("lenteja") || n.contains("macarron") || n.contains("spaguetti")
                || n.contains("spaghetti") || n.contains("panela")) {
            return 4; // Legumbres y cereales
        }
        if (n.contains("papa") || n.contains("yuca") || n.contains("platano") || n.contains("tomate")
                || n.contains("cebolla") || n.contains("zanahoria") || n.contains("lechuga")
                || n.contains("pepino") || n.contains("pimenton") || n.contains("remolacha")
                || n.contains("habichuela") || n.contains("cilantro") || n.contains("perejil")
                || n.contains("apio") || n.contains("aguacate") || n.contains("calabazin")
                || n.contains("limon") || n.contains("lulo") || n.contains("guayaba")
                || n.contains("mango") || n.contains("papaya") || n.contains("patilla")
                || n.contains("banano") || n.contains("durazno") || n.contains("fresa")
                || n.contains("mora") || n.contains("manzana") || n.contains("pera")
                || n.contains("mandarina")) {
            return 6; // Frutas y verduras
        }
        return 5; // Abarrotes
    }

    private static String clave(String texto) {
        return normalizar(texto);
    }

    private static String claveProducto(String texto) {
        return normalizar(texto);
    }

    private static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String n = Normalizer.normalize(texto, Normalizer.Form.NFD);
        n = n.replaceAll("\\p{M}", "");
        return n.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }

    private static final class CantidadArchivo {
        final double valor;
        final int idUnidad;
        final String nombreProducto;

        CantidadArchivo(double valor, int idUnidad, String nombreProducto) {
            this.valor = valor;
            this.idUnidad = idUnidad;
            this.nombreProducto = nombreProducto;
        }
    }

    private static String quitarBom(String texto) {
        return texto != null && texto.startsWith("\uFEFF") ? texto.substring(1) : texto;
    }

    private static char detectarSeparador(String encabezado) {
        char[] candidatos = {';', ',', '\t'};
        char elegido = ';';
        int maximo = -1;
        for (char c : candidatos) {
            int cuenta = contarFueraComillas(encabezado, c);
            if (cuenta > maximo) {
                maximo = cuenta;
                elegido = c;
            }
        }
        return elegido;
    }

    private static int contarFueraComillas(String linea, char separador) {
        boolean comillas = false;
        int cuenta = 0;
        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);
            if (c == '"') {
                comillas = !comillas;
            } else if (c == separador && !comillas) {
                cuenta++;
            }
        }
        return cuenta;
    }

    private static String[] parsearLineaPlana(String linea, char separador) {
        List<String> campos = new ArrayList<>();
        StringBuilder campo = new StringBuilder();
        boolean comillas = false;
        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);
            if (c == '"') {
                if (comillas && i + 1 < linea.length() && linea.charAt(i + 1) == '"') {
                    campo.append('"');
                    i++;
                } else {
                    comillas = !comillas;
                }
            } else if (c == separador && !comillas) {
                campos.add(campo.toString());
                campo.setLength(0);
            } else {
                campo.append(c);
            }
        }
        campos.add(campo.toString());
        return campos.toArray(new String[0]);
    }

    private static String hashSha256(File archivo) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(Files.readAllBytes(archivo.toPath()));
        StringBuilder hex = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
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

}
