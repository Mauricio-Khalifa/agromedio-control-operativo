package agromedio.modelo;

/** Indicadores agrupados de las plantillas que mantienen abierto su ciclo operativo. */
public class ResumenProgreso {
    private int plantillasAbiertas;
    private int cargadas;
    private int enCompra;
    private int recibiendo;
    private int alistando;
    private int listas;
    private int despachando;
    private int despachosEnVerificacion;
    private int recepcionesConFaltante;

    public int getPlantillasAbiertas() { return plantillasAbiertas; }
    public void setPlantillasAbiertas(int plantillasAbiertas) { this.plantillasAbiertas = plantillasAbiertas; }
    public int getCargadas() { return cargadas; }
    public void setCargadas(int cargadas) { this.cargadas = cargadas; }
    public int getEnCompra() { return enCompra; }
    public void setEnCompra(int enCompra) { this.enCompra = enCompra; }
    public int getRecibiendo() { return recibiendo; }
    public void setRecibiendo(int recibiendo) { this.recibiendo = recibiendo; }
    public int getAlistando() { return alistando; }
    public void setAlistando(int alistando) { this.alistando = alistando; }
    public int getListas() { return listas; }
    public void setListas(int listas) { this.listas = listas; }
    public int getDespachosEnVerificacion() { return despachosEnVerificacion; }
    public void setDespachosEnVerificacion(int despachosEnVerificacion) { this.despachosEnVerificacion = despachosEnVerificacion; }
    public int getRecepcionesConFaltante() { return recepcionesConFaltante; }
    public void setRecepcionesConFaltante(int recepcionesConFaltante) { this.recepcionesConFaltante = recepcionesConFaltante; }
}
