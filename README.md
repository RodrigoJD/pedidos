# Sistema de Pedidos Desktop Assíncrono

Projeto único Maven contendo:

- Java 21
- Spring Boot
- Spring Web
- Spring AMQP
- RabbitMQ
- Java Swing
- Jackson
- Validação Bean Validation
- DLQ
- Polling assíncrono

## Arquitetura

A aplicação é um único projeto Maven. O mecanismo de mensageria é desacoplado
por meio da interface `PedidoPublisher`.

Por padrão, a aplicação utiliza o profile `local`, que não depende de RabbitMQ:

1. Spring Boot sobe o backend HTTP.
2. O consumidor local é iniciado em uma thread separada.
3. A interface Swing é aberta.
4. O Swing envia pedidos via REST.
5. O backend publica o pedido em uma `BlockingQueue`.
6. O consumidor processa o pedido de forma assíncrona.
7. Falhas são enviadas para uma DLQ em memória.
8. O status fica armazenado em memória.
9. O Swing consulta o status periodicamente.

Com o profile `rabbit`, o mesmo fluxo utiliza RabbitMQ, filas reais e DLQ
RabbitMQ.

## RabbitMQ

Filas criadas:

- pedidos.entrada.rodrigo
- pedidos.entrada.rodrigo.dlq
- pedidos.status.sucesso.rodrigo
- pedidos.status.falha.rodrigo

A fila de entrada possui Dead Letter Queue configurada.

## Executar localmente sem RabbitMQ

Na raiz:

```bash
mvn spring-boot:run
```

A aplicação abrirá a interface Swing e o fluxo assíncrono funcionará com uma
fila em memória.

## Executar com RabbitMQ

Na raiz:

```bash
docker compose up -d
mvn spring-boot:run -Dspring-boot.run.profiles=rabbit
```

A aplicação abrirá a interface Swing usando RabbitMQ real.

## Gerar JAR

mvn clean package

Executar:

java -jar target/pedidos-assincrono-1.0.0.jar

## Configuração do RabbitMQ

Por padrão:

RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest
RABBITMQ_VHOST=/

Para CloudAMQP, configure essas variáveis de ambiente com os valores
fornecidos pelo painel da instância, incluindo a porta TLS e o vhost.

Também é possível alterar o nome das filas:

PEDIDOS_SUFFIX=seu-nome

## Backend

POST /api/pedidos

GET /api/pedidos/status/{id}

## Exemplo de POST

{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "produto": "Notebook",
  "quantidade": 2,
  "dataCriacao": "2026-10-06T09:00:00"
}

Resposta:

HTTP 202 Accepted

{
  "id": "...",
  "status": "RECEBIDO",
  "mensagem": "Pedido recebido para processamento"
}

## Observação

Os status são mantidos em memória porque o teste explicitamente permite não
utilizar banco de dados.

A aplicação simula processamento entre 1 e 3 segundos e possui 20% de chance
de gerar uma falha de processamento. Quando isso acontece, o pedido é rejeitado
sem requeue e encaminhado para a DLQ.

## Testes unitários

A aplicação possui testes unitários para os principais métodos e fluxos:

- `PedidoPublisherTest`: verifica a publicação do pedido na fila de entrada.
- `StatusServiceTest`: verifica atualização e consulta de status.
- `PedidoConsumerTest`: verifica processamento com sucesso, falha com publicação no status de falha + `basicReject` para DLQ e interrupção com `basicNack`/requeue.
- `PedidoControllerTest`: verifica criação de pedidos, publicação e consulta de status.
- `PedidoControllerValidationTest`: verifica HTTP 400 para payload inválido.
- `ApiExceptionHandlerTest`: verifica respostas para erros de validação e exceções genéricas.
- `RabbitConfigTest`: verifica nomes das filas, conversor Jackson, `RabbitTemplate` e declaração das filas/DLQ.

Para executar todos os testes:

```bash
mvn clean test
```

O `PedidoConsumer` mantém o comportamento original em produção, mas recebe internamente fornecedores de atraso e aleatoriedade para que os testes sejam determinísticos e não dependam de sorte ou esperas reais de 1–3 segundos.

## Execução sem RabbitMQ

Como o RabbitMQ originalmente fornecido para o teste pode não estar disponível,
o projeto possui um modo `local` assíncrono por padrão.

No modo local:

- o pedido é colocado em uma `BlockingQueue` em memória;
- um consumidor executa o processamento em uma thread separada;
- o processamento continua assíncrono;
- falhas são encaminhadas para uma DLQ em memória;
- a API REST e o cliente Swing continuam funcionando normalmente.

Execute simplesmente:

```bash
mvn spring-boot:run
```

Para usar RabbitMQ real, altere o profile:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=rabbit
```

Nesse modo, as implementações `RabbitPedidoPublisher`, `PedidoConsumer` e
`RabbitConfig` são ativadas e a aplicação utiliza as filas reais configuradas
pelas variáveis `RABBITMQ_*`.

A aplicação usa a interface `PedidoPublisher` para desacoplar o controller do
mecanismo de mensageria. Dessa forma, a indisponibilidade do broker externo não
impede a demonstração do fluxo assíncrono.
