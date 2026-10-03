package agromedio.modelo;

public class Cliente {
    private int idCliente;
    private String nombreEntidad;
    private String contacto;
    private String telefono;

    public Cliente() {}

    public Cliente(int idCliente, String nombreEntidad, String contacto, String telefono) {
        this.idCliente = idCliente;
        this.nombreEntidad = nombreEntidad;
        this.contacto = contacto;
        this.telefono = telefono;
    }

    public int getIdCliente() { return idCliente; }
    public void setIdCliente(int idCliente) { this.idCliente = idCliente; }
    public String getNombreEntidad() { return nombreEntidad; }
    public void setNombreEntidad(String nombreEntidad) { this.nombreEntidad = nombreEntidad; }
    public String getContacto() { return contacto; }
    public void setContacto(String contacto) { this.contacto = contacto; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    @Override
    public String toString() { return nombreEntidad; }
}
