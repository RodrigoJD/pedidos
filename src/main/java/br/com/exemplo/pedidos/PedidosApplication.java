package br.com.exemplo.pedidos;

import br.com.exemplo.pedidos.desktop.PedidosDesktop;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.swing.SwingUtilities;

@SpringBootApplication
public class PedidosApplication {

    public static void main(String[] args) {
        SpringApplication application =
                new SpringApplication(PedidosApplication.class);

        application.setHeadless(false);

        application.run(args);

        SwingUtilities.invokeLater(() ->
                new PedidosDesktop().setVisible(true)
        );
    }
}
