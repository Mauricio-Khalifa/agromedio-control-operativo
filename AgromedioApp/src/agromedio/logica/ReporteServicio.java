package agromedio.logica;

import agromedio.datos.ConsultaDAO;
import agromedio.datos.ConsultaDAOSqlite;
import agromedio.modelo.FilaAvance;
import agromedio.modelo.FilaConsolidado;
import agromedio.modelo.FilaFaltante;
import agromedio.modelo.FilaSaldo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Genera los reportes operativos en texto listos para imprimir. */
public class ReporteServicio {

    private final ConsultaDAO consultaDAO;

    public ReporteServicio() {
        this(new ConsultaDAOSqlite());
    }

    public ReporteServicio(ConsultaDAO consultaDAO) {
        this.consultaDAO = consultaDAO;
    }

    private static final String SEPARADOR = "====================================================================";

    private String cabecera(String titulo) {
        return SEPARADOR + "\n"
                + "ASOCIACION AGROPECUARIA Y AMBIENTAL DEL MAGDALENA MEDIO - AGROMEDIO\n"
                + titulo + "\n"
                + "Generado: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                + "\n" + SEPARADOR + "\n";
    }

    /** Reporte consolidado de faltantes de recepcion (RF06). */
    public String reporteFaltantes() {
        StringBuilder sb = new StringBuilder(cabecera("REPORTE DE FALTANTES DE RECEPCION"));
        List<FilaFaltante> filas = consultaDAO.faltantesRecepcion();
        if (filas.isEmpty()) {
            sb.append("No se han detectado faltantes de recepcion.\n");
        } else {
            double total = 0;
            for (FilaFaltante f : filas) {
                sb.append(String.format("Mercado: %s | Producto: %s%n", f.getNombreMercado(), f.getProducto()));
                sb.append(String.format("  Proveedor: %s | Comprado: %.2f %s | Recibido: %.2f | Faltante: %.2f%n",
                        f.getProveedor(), f.getCantidadComprada(), f.getUnidadCompra(),
                        f.getCantidadRecibida(), f.getCantidadFaltante()));
                total += f.getCantidadFaltante();
            }
            sb.append(SEPARADOR).append("\n");
            sb.append("Total de unidades en falta: ").append(String.format("%.2f", total)).append("\n");
        }
        return sb.toString();
    }

    /** Reporte de saldos de una plantilla. */
    public String reporteSaldo(int idPlantilla) {
        StringBuilder sb = new StringBuilder(cabecera("HOJA DE SALDO DEL MERCADO (PLANTILLA " + idPlantilla + ")"));
        List<FilaSaldo> filas = consultaDAO.saldoPlantilla(idPlantilla);
        sb.append(String.format("%-30s %10s %10s %10s %10s %10s%n",
                "PRODUCTO", "SOLICIT.", "COMPRAD.", "RECIB.", "ALIST.", "DESPACH."));
        for (FilaSaldo f : filas) {
            sb.append(String.format("%-30.30s %10.2f %10.2f %10.2f %10.2f %10.2f%n",
                    f.getProducto(), f.getSolicitado(), f.getComprado(), f.getRecibido(),
                    f.getAlistado(), f.getDespachado()));
        }
        sb.append(SEPARADOR).append("\n");
        double pendiente = filas.stream().mapToDouble(FilaSaldo::getPorDespachar).sum();
        sb.append("Por despachar: ").append(String.format("%.2f", pendiente)).append("\n");
        return sb.toString();
    }

    /** Lista de chequeo de despacho (RF09 - RF10). */
    public String reporteConsolidado(String fecha) {
        StringBuilder sb = new StringBuilder(cabecera("CONSOLIDADO DE DEMANDA - " + fecha));
        List<FilaConsolidado> filas = consultaDAO.consolidarDemanda(fecha);
        if (filas.isEmpty()) {
            sb.append("No hay plantillas activas para la fecha indicada.\n");
            return sb.toString();
        }
        sb.append(String.format("%-30s %12s %12s %10s %10s%n",
                "PRODUCTO", "DEMANDA", "EN COMPRA", "MERCADOS", "PENDIENTE"));
        for (FilaConsolidado f : filas) {
            sb.append(String.format("%-30.30s %9.2f %-3.3s %9.2f %-3.3s %5d %10.2f%n",
                    f.getProducto(), f.getTotalSolicitado(), f.getUnidadDespacho(),
                    f.getTotalComprado(), f.getUnidadDespacho(), f.getMercados(), f.getPendiente()));
        }
        return sb.toString();
    }

    /** Avance global de despachos del dia. */
    public String reporteAvance() {
        StringBuilder sb = new StringBuilder(cabecera("AVANCE DE DESPACHOS"));
        List<FilaAvance> filas = consultaDAO.avanceDespacho();
        if (filas.isEmpty()) {
            sb.append("No hay despachos registrados.\n");
        } else {
            for (FilaAvance f : filas) {
                sb.append(String.format("%s | %s | %d/%d items verificados (%.1f%%) | %s%n",
                        f.getFechaDespacho(), f.getNombreMercado(), f.getVerificados(), f.getItems(),
                        f.getPorcentaje(), f.getEstado()));
            }
        }
        return sb.toString();
    }

    public String fechaHoy() {
        return LocalDate.now().toString();
    }
}
