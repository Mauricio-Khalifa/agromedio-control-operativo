package agromedio.logica;

import agromedio.datos.*;

/** Fábrica centralizada para obtener servicios, evitando la instanciación directa de DAOs en la capa de UI. */
public final class ServiceFactory {

    private ServiceFactory() {}

    public static CompraServicio getCompraServicio() {
        return new CompraServicio(new CompraDAOSqlite(), new PlantillaDAOSqlite(), new ConsultaDAOSqlite());
    }

    public static AlistamientoServicio getAlistamientoServicio() {
        return new AlistamientoServicio(new AlistamientoDAOSqlite(), new PlantillaDAOSqlite(), new ConsultaDAOSqlite(), new ProductoDAOSqlite());
    }
    
    public static CargaExcelServicio getCargaExcelServicio() {
        return new CargaExcelServicio(new ClienteDAOSqlite(), new ProductoDAOSqlite(), new PlantillaDAOSqlite(), new CargaPlantillaDAOSqlite());
    }

    public static DespachoServicio getDespachoServicio() {
        return new DespachoServicio(new DespachoDAOSqlite(), new PlantillaDAOSqlite());
    }

    public static PlantillaServicio getPlantillaServicio() {
        return new PlantillaServicio(new PlantillaDAOSqlite());
    }

    public static ProgresoServicio getProgresoServicio() {
        return new ProgresoServicio(new ConsultaDAOSqlite());
    }

    public static RecepcionServicio getRecepcionServicio() {
        return new RecepcionServicio(new RecepcionDAOSqlite(), new CompraDAOSqlite(), new PlantillaDAOSqlite());
    }

    public static ReporteServicio getReporteServicio() {
        return new ReporteServicio(new ConsultaDAOSqlite());
    }
}
