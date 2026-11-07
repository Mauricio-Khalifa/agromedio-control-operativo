package agromedio.presentacion;

import javax.swing.*;
import java.awt.*;

/** Login global previo al acceso al sistema. */
public class FrmLogin extends JDialog {
    private static final String USUARIO = "admin";
    private static final String CONTRASENA = "admin123";
    private boolean autenticado;

    public FrmLogin() {
        super((Frame) null, "Acceso a AGROMEDIO", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(600, 300);
        setResizable(false);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(7, 7, 7, 7);
        g.fill = GridBagConstraints.HORIZONTAL;

        JLabel titulo = new JLabel("Ingreso al sistema");
        titulo.setFont(Tema.fuente(Font.BOLD, 20));
        titulo.setForeground(Tema.PRIMARIO_OSCURO);
        g.gridx = 0; g.gridy = 0; g.gridwidth = 2;
        panel.add(titulo, g);

        JTextField usuario = UiUtil.campo();
        JPasswordField contrasena = new JPasswordField();
        contrasena.setFont(Tema.BASE);
        JLabel mensaje = UiUtil.subtitulo("Ingrese sus credenciales para continuar.");

        g.gridwidth = 1; g.gridy = 1; g.gridx = 0;
        panel.add(new JLabel("Usuario"), g);
        g.gridx = 1; g.weightx = 1;
        panel.add(usuario, g);
        g.gridx = 0; g.gridy = 2; g.weightx = 0;
        panel.add(new JLabel("Contraseña"), g);
        g.gridx = 1; g.weightx = 1;
        panel.add(contrasena, g);
        g.gridx = 0; g.gridy = 3; g.gridwidth = 2;
        panel.add(mensaje, g);

        JButton ingresar = UiUtil.botonPrimario("Ingresar", Iconos.accion(Iconos.Accion.VERIFICAR, Color.WHITE));
        JButton salir = UiUtil.botonSecundario("Salir", Iconos.accion(Iconos.Accion.CERRAR, Tema.PRIMARIO));
        JButton demo = UiUtil.botonSecundario("Acceso demo", Iconos.accion(Iconos.Accion.VER, Tema.PRIMARIO));
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        salir.setPreferredSize(new Dimension(95, 38));
        demo.setPreferredSize(new Dimension(170, 38));
        ingresar.setPreferredSize(new Dimension(115, 38));
        botones.add(salir);
        botones.add(demo);
        botones.add(ingresar);
        g.gridy = 4;
        panel.add(botones, g);
        setContentPane(panel);

        Runnable validar = () -> {
            if (USUARIO.equals(usuario.getText().trim())
                    && CONTRASENA.equals(new String(contrasena.getPassword()))) {
                autenticado = true;
                dispose();
            } else {
                mensaje.setText("Usuario o contraseña incorrectos.");
                contrasena.setText("");
                contrasena.requestFocusInWindow();
            }
        };
        ingresar.addActionListener(e -> validar.run());
        contrasena.addActionListener(e -> validar.run());
        demo.addActionListener(e -> {
            autenticado = true;
            dispose();
        });
        salir.addActionListener(e -> dispose());
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                dispose();
            }
        });
        usuario.requestFocusInWindow();
    }

    public boolean isAutenticado() { return autenticado; }
}
