package agromedio.modelo;

public class Producto {
    private int idProducto;
    private String nombre;
    private int idCategoria;
    private int idUnidadCompra;
    private int idUnidadDespacho;
    private double factorConversion;

    // Campos de presentacion (no persistidos): se cargan con JOIN para la UI
    private String nombreCategoria;
    private String nombreUnidadCompra;
    private String abreviaturaCompra;
    private String nombreUnidadDespacho;
    private String abreviaturaDespacho;

    public Producto() {}

    public Producto(int idProducto, String nombre, int idCategoria, int idUnidadCompra,
                    int idUnidadDespacho, double factorConversion) {
        this.idProducto = idProducto;
        this.nombre = nombre;
        this.idCategoria = idCategoria;
        this.idUnidadCompra = idUnidadCompra;
        this.idUnidadDespacho = idUnidadDespacho;
        this.factorConversion = factorConversion;
    }

    public int getIdProducto() { return idProducto; }
    public void setIdProducto(int idProducto) { this.idProducto = idProducto; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public int getIdCategoria() { return idCategoria; }
    public void setIdCategoria(int idCategoria) { this.idCategoria = idCategoria; }
    public int getIdUnidadCompra() { return idUnidadCompra; }
    public void setIdUnidadCompra(int idUnidadCompra) { this.idUnidadCompra = idUnidadCompra; }
    public int getIdUnidadDespacho() { return idUnidadDespacho; }
    public void setIdUnidadDespacho(int idUnidadDespacho) { this.idUnidadDespacho = idUnidadDespacho; }
    public double getFactorConversion() { return factorConversion; }
    public void setFactorConversion(double factorConversion) { this.factorConversion = factorConversion; }

    public String getNombreCategoria() { return nombreCategoria; }
    public void setNombreCategoria(String nombreCategoria) { this.nombreCategoria = nombreCategoria; }
    public String getNombreUnidadCompra() { return nombreUnidadCompra; }
    public void setNombreUnidadCompra(String nombreUnidadCompra) { this.nombreUnidadCompra = nombreUnidadCompra; }
    public String getAbreviaturaCompra() { return abreviaturaCompra; }
    public void setAbreviaturaCompra(String abreviaturaCompra) { this.abreviaturaCompra = abreviaturaCompra; }
    public String getNombreUnidadDespacho() { return nombreUnidadDespacho; }
    public void setNombreUnidadDespacho(String nombreUnidadDespacho) { this.nombreUnidadDespacho = nombreUnidadDespacho; }
    public String getAbreviaturaDespacho() { return abreviaturaDespacho; }
    public void setAbreviaturaDespacho(String abreviaturaDespacho) { this.abreviaturaDespacho = abreviaturaDespacho; }

    /** Convierte una cantidad de la unidad de compra a la de despacho. */
    public double aUnidadDespacho(double cantidadCompra) {
        return Math.round(cantidadCompra * factorConversion * 100.0) / 100.0;
    }

    /** Convierte una cantidad de la unidad de despacho a la de compra. */
    public double aUnidadCompra(double cantidadDespacho) {
        return Math.round((cantidadDespacho / factorConversion) * 100.0) / 100.0;
    }

    @Override
    public String toString() { return nombre; }
}
