package agromedio.datos;

import agromedio.modelo.FilaAvance;
import agromedio.modelo.FilaConsolidado;
import agromedio.modelo.FilaFaltante;
import agromedio.modelo.FilaSaldo;

import java.util.List;

/** Consultas de control y reporte sobre las vistas de la base de datos. */
public interface ConsultaDAO {
    List<FilaSaldo> saldoPlantilla(int idPlantilla);
    List<FilaSaldo> saldoTotal();
    List<FilaFaltante> faltantesRecepcion();
    List<FilaConsolidado> consolidarDemanda(String fechaProgramada);
    List<FilaAvance> avanceDespacho();
}
