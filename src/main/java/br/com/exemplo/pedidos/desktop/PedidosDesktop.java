package br.com.exemplo.pedidos.desktop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

import static br.com.exemplo.pedidos.enums.Status.*;

public class PedidosDesktop extends JFrame {

    private final JTextField produtoField = new JTextField(20);
    private final JTextField quantidadeField = new JTextField(6);

    private final DefaultTableModel tableModel =
            new DefaultTableModel(
                    new Object[]{
                            "ID",
                            "Produto",
                            "Quantidade",
                            "Status"
                    },
                    0
            ) {
                @Override
                public boolean isCellEditable(
                        int row,
                        int column) {
                    return false;
                }
            };

    private final JTable tabela = new JTable(tableModel);

    private final ObjectMapper objectMapper =
            new ObjectMapper()
                    .registerModule(new JavaTimeModule());

    private final HttpClient httpClient =
            HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .build();

    private final String backendUrl =
            System.getenv()
                    .getOrDefault(
                            "PEDIDOS_API_URL",
                            "http://localhost:8080"
                    );

    private final Map<UUID, Integer> linhas =
            new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    public PedidosDesktop() {

        super("Sistema de Pedidos Assíncrono");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 500);
        setLocationRelativeTo(null);

        JPanel formulario =
                new JPanel(new FlowLayout(FlowLayout.LEFT));

        formulario.add(new JLabel("Produto:"));
        formulario.add(produtoField);

        formulario.add(new JLabel("Quantidade:"));
        formulario.add(quantidadeField);

        JButton enviarButton =
                new JButton("Enviar Pedido");

        formulario.add(enviarButton);

        add(
                formulario,
                BorderLayout.NORTH
        );

        tabela.setFillsViewportHeight(true);
        tabela.setAutoCreateRowSorter(true);

        add(
                new JScrollPane(tabela),
                BorderLayout.CENTER
        );

        enviarButton.addActionListener(
                event -> enviarPedido(enviarButton)
        );

        scheduler.scheduleWithFixedDelay(
                this::consultarStatus,
                3,
                4,
                TimeUnit.SECONDS
        );

        addWindowListener(
                new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosing(
                            java.awt.event.WindowEvent event) {

                        scheduler.shutdownNow();
                    }
                }
        );
    }

    private void enviarPedido(JButton button) {

        String produto =
                produtoField.getText().trim();

        int quantidade;

        try {

            quantidade =
                    Integer.parseInt(
                            quantidadeField
                                    .getText()
                                    .trim()
                    );

            if (produto.isBlank() || quantidade <= 0) {
                throw new IllegalArgumentException();
            }

        } catch (Exception exception) {

            JOptionPane.showMessageDialog(
                    this,
                    "Informe um produto e uma quantidade maior que zero.",
                    "Dados inválidos",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        UUID id = UUID.randomUUID();

        Map<String, Object> payload =
                new LinkedHashMap<>();

        payload.put("id", id);
        payload.put("produto", produto);
        payload.put("quantidade", quantidade);
        payload.put(
                "dataCriacao",
                LocalDateTime.now()
        );

        button.setEnabled(false);

        new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground()
                    throws Exception {

                String json =
                        objectMapper.writeValueAsString(
                                payload
                        );

                HttpRequest request =
                        HttpRequest.newBuilder()
                                .uri(
                                        URI.create(
                                                backendUrl +
                                                        "/api/pedidos"
                                        )
                                )
                                .header(
                                        "Content-Type",
                                        "application/json"
                                )
                                .timeout(
                                        Duration.ofSeconds(8)
                                )
                                .POST(
                                        HttpRequest.BodyPublishers
                                                .ofString(json)
                                )
                                .build();

                HttpResponse<String> response =
                        httpClient.send(
                                request,
                                HttpResponse.BodyHandlers.ofString()
                        );

                if (response.statusCode() != 202) {

                    throw new IllegalStateException(
                            "HTTP " +
                                    response.statusCode() +
                                    ": " +
                                    response.body()
                    );
                }

                return null;
            }

            @Override
            protected void done() {

                button.setEnabled(true);

                try {

                    get();

                    int row =
                            tableModel.getRowCount();

                    tableModel.addRow(
                            new Object[]{
                                    id.toString(),
                                    produto,
                                    quantidade,
                                    "ENVIADO, AGUARDANDO PROCESSO"
                            }
                    );

                    linhas.put(id, row);

                    produtoField.setText("");
                    quantidadeField.setText("");

                } catch (Exception exception) {

                    JOptionPane.showMessageDialog(
                            PedidosDesktop.this,
                            "Não foi possível enviar o pedido. " +
                                    "Verifique se o backend está disponível.",
                            "Erro",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }

        }.execute();
    }

    private void consultarStatus() {

        for (UUID id : new ArrayList<>(linhas.keySet())) {

            try {

                HttpRequest request =
                        HttpRequest.newBuilder()
                                .uri(
                                        URI.create(
                                                backendUrl +
                                                        "/api/pedidos/status/" +
                                                        id
                                        )
                                )
                                .timeout(Duration.ofSeconds(5))
                                .GET()
                                .build();

                HttpResponse<String> response =
                        httpClient.send(
                                request,
                                HttpResponse.BodyHandlers.ofString()
                        );

                if (response.statusCode() != 200) {
                    continue;
                }

                JsonNode json =
                        objectMapper.readTree(response.body());

                String status =
                        json.path("status")
                                .asText(DESCONHECIDO.name());

                String erro =
                        json.path("mensagemErro")
                                .asText("");

                if (status.equals(SUCESSO.name())) {

                    atualizarTabela(
                            id,
                            SUCESSO.name()
                    );

                } else if (status.equals(FALHA.name())) {

                    atualizarTabela(
                            id,
                            status +
                                    (
                                            erro.isBlank()
                                                    ? ""
                                                    : " - " + erro
                                    )
                    );

                } else if (status.equals(PROCESSANDO.name())) {

                    atualizarTabela(
                            id,
                            PROCESSANDO.name()
                    );
                }

            } catch (Exception ignored) {
                /*
                 * Erros transitórios são ignorados.
                 * O próximo ciclo do polling tentará novamente.
                 */
            }
        }
    }

    private static boolean isStatusEqualsProcessando(String status) {
        return status.equals(PROCESSANDO.name());
    }

    private void atualizarTabela(
            UUID id,
            String status) {

        SwingUtilities.invokeLater(() -> {

            Integer row = linhas.get(id);

            System.out.println(
                    "ATUALIZAR TABELA: id=" + id +
                            ", status=" + status +
                            ", row=" + row +
                            ", totalRows=" + tableModel.getRowCount()
            );

            if (row != null &&
                    row < tableModel.getRowCount()) {

                tableModel.setValueAt(
                        status,
                        row,
                        3
                );

                if (status.startsWith(SUCESSO.name()) ||
                        status.startsWith(FALHA.name())) {

                    linhas.remove(id);
                }
            }
        });
    }
}
