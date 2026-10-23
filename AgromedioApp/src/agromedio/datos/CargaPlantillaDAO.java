package agromedio.datos;

import agromedio.modelo.CargaPlantilla;

public interface CargaPlantillaDAO {
    CargaPlantilla buscarPorHash(String hashArchivo);
    CargaPlantilla obtener(int idCarga);
    int guardar(CargaPlantilla carga);
    void actualizarResumen(int idCarga, int plantillasCreadas, int productosRegistrados);
}
