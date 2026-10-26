package agromedio.logica;

import agromedio.datos.ConsultaDAO;
import agromedio.datos.ConsultaDAOSqlite;
import agromedio.modelo.FilaProgreso;
import agromedio.modelo.PlantillaOperativa;
import agromedio.modelo.ResumenProgreso;
import agromedio.modelo.TareaProgreso;

import java.util.ArrayList;
import java.util.List;

/** Coordina los indicadores y tareas de las plantillas con ciclo operativo abierto. */
public class ProgresoServicio {

    private final ConsultaDAO consultaDAO;

    public ProgresoServicio() {
        this(new ConsultaDAOSqlite());
    }

    public ProgresoServicio(ConsultaDAO consultaDAO) {
        if (consultaDAO == null) {
            throw new IllegalArgumentException("La consulta de progreso es obligatoria");
        }
        this.consultaDAO = consultaDAO;
    }

    public List<FilaProgreso> procesosAbiertos() {
        return consultaDAO.progresoAbierto();
    }

    public ResumenProgreso resumen() {
        return consultaDAO.resumenProgreso();
    }

    public ResumenProgreso resumen(List<FilaProgreso> filas) {
        if (filas == null) {
            throw new IllegalArgumentException("Las filas de progreso son obligatorias");
        }
        ResumenProgreso resumen = new ResumenProgreso();
        for (FilaProgreso fila : filas) {
            if (fila == null) continue;
            resumen.setPlantillasAbiertas(resumen.getPlantillasAbiertas() + 1);
            switch (fila.getEstado()) {
                case "CARGADA" -> resumen.setCargadas(resumen.getCargadas() + 1);
                case "EN_COMPRA" -> resumen.setEnCompra(resumen.getEnCompra() + 1);
                case "RECIBIENDO" -> resumen.setRecibiendo(resumen.getRecibiendo() + 1);
                case "ALISTANDO" -> resumen.setAlistando(resumen.getAlistando() + 1);
                case "LISTA" -> resumen.setListas(resumen.getListas() + 1);
                default -> { }
            }
            if ("EN_VERIFICACION".equals(fila.getEstadoDespacho())) {
                resumen.setDespachosEnVerificacion(resumen.getDespachosEnVerificacion() + 1);
            }
            if (fila.isTieneFaltante()) {
                resumen.setRecepcionesConFaltante(resumen.getRecepcionesConFaltante() + 1);
            }
        }
        return resumen;
    }

    public List<TareaProgreso> tareasPendientes() {
        List<TareaProgreso> tareas = new ArrayList<>();
        for (FilaProgreso fila : procesosAbiertos()) {
            agregarTareas(tareas, fila);
        }
        return tareas;
    }

    public List<TareaProgreso> tareasPendientes(List<FilaProgreso> filas) {
        if (filas == null) {
            throw new IllegalArgumentException("Las filas de progreso son obligatorias");
        }
        List<TareaProgreso> tareas = new ArrayList<>();
        for (FilaProgreso fila : filas) {
            if (fila != null) {
                agregarTareas(tareas, fila);
            }
        }
        return tareas;
    }

    private static void agregarTareas(List<TareaProgreso> tareas, FilaProgreso fila) {
        String estado = fila.getEstado();
        String mercado = fila.getNombreMercado();
        int id = fila.getIdPlantilla();
        if (fila.isTieneNovedad()) {
            tareas.add(new TareaProgreso(id, mercado,
                    "Revisar novedad registrada en despacho", TareaProgreso.ALTA, estado));
        }
        if (fila.isTieneFaltante()) {
            tareas.add(new TareaProgreso(id, mercado,
                    "Revisar faltante de recepción", TareaProgreso.ALTA, estado));
        }
        if (PlantillaOperativa.ESTADO_CARGADA.equals(estado)
                || PlantillaOperativa.ESTADO_EN_COMPRA.equals(estado)) {
            tareas.add(new TareaProgreso(id, mercado,
                    "Completar compra pendiente", TareaProgreso.ALTA, estado));
        } else if (PlantillaOperativa.ESTADO_RECIBIENDO.equals(estado)) {
            tareas.add(new TareaProgreso(id, mercado,
                    "Completar recepción de mercancía", TareaProgreso.ALTA, estado));
        } else if (PlantillaOperativa.ESTADO_ALISTANDO.equals(estado)) {
            tareas.add(new TareaProgreso(id, mercado,
                    "Completar alistamiento", TareaProgreso.MEDIA, estado));
        } else if (PlantillaOperativa.ESTADO_LISTA.equals(estado)) {
            tareas.add(new TareaProgreso(id, mercado,
                    "Iniciar o completar despacho", TareaProgreso.MEDIA, estado));
        }
    }
}
