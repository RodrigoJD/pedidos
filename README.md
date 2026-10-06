# Sistema de Pedidos Desktop Assíncrono

Sistema de pedidos assíncrono desenvolvido em **Java 21 + Spring Boot**, com interface desktop em **Java Swing** e suporte a dois mecanismos de mensageria:

* **Local:** fila em memória, sem dependência de RabbitMQ.
* **RabbitMQ:** mensageria real utilizando Spring AMQP.

O projeto é uma aplicação Maven única e mantém o mecanismo de mensageria desacoplado por meio da interface `PedidoPublisher`.

## Tecnologias

* Java 21
* Spring Boot
* Spring Web
* Spring AMQP
* RabbitMQ
* Java Swing
* Jackson
* Bean Validation
* Maven
* Docker / Docker Compose
* JUnit 5
* Mockito

## Arquitetura

A aplicação possui dois modos de execução.

### Profile `local`

É o modo padrão e **não depende de RabbitMQ**.

```text
                    +---------------------+
                    |    Swing Desktop    |
                    +----------+----------+
                               |
                              HTTP
                               |
                               v
                    +---------------------+
                    |   REST Controller   |
                    +----------+----------+
                               |
                               v
                    +---------------------+
                    |   PedidoPublisher    |
                    |      local           |
                    +----------+----------+
                               |
                               v
                    +---------------------+
                    |    BlockingQueue     |
                    |      em memória      |
                    +----------+----------+
                               |
                               v
                    +---------------------+
                    | InMemoryPedido       |
                    | Consumer             |
                    +----------+----------+
                               |
                               v
                    +---------------------+
                    |    Processamento     |
                    |     assíncrono       |
                    +----------+----------+
                               |
                    +----------+----------+
                    |                     |
                    v                     v
                 SUCESSO                FALHA
                                         |
                                         v
                                  DLQ em memória
```

Fluxo:

1. Spring Boot inicia o backend HTTP.
2. O consumidor local é iniciado em uma thread separada.
3. A interface Swing é aberta.
4. O Swing envia o pedido através da API REST.
5. O backend registra o status `RECEBIDO`.
6. O pedido é colocado em uma `BlockingQueue`.
7. O consumidor processa o pedido de forma assíncrona.
8. O status é alterado para `PROCESSANDO`.
9. O processamento leva entre 1 e 3 segundos.
10. Existe uma chance simulada de 20% de falha.
11. Em caso de sucesso, o status passa para `SUCESSO`.
12. Em caso de falha, o status passa para `FALHA` e o pedido é enviado para a DLQ em memória.
13. O Swing consulta o status periodicamente através da API REST.
14. A interface atualiza a tabela quando o processamento termina.

### Profile `rabbit`

Com o profile `rabbit`, o fluxo utiliza RabbitMQ real:

```text
Swing
  |
  v
REST API
  |
  v
PedidoPublisher
  |
  v
RabbitMQ
  |
  v
pedidos.entrada.rodrigo
  |
  v
PedidoConsumer
  |
  +-------------> SUCESSO
  |
  +-------------> FALHA
                    |
                    v
             pedidos.entrada.rodrigo.dlq
```

O controller não depende diretamente de RabbitMQ.

A comunicação é desacoplada através da interface:

```java
PedidoPublisher
```

Dessa forma, a aplicação pode utilizar uma implementação local ou RabbitMQ sem alterar o controller.

## Interface gráfica

A aplicação possui uma interface desktop desenvolvida com **Java Swing**.

A interface permite:

* informar o produto;
* informar a quantidade;
* enviar um pedido;
* visualizar os pedidos enviados;
* acompanhar o processamento;
* visualizar `PROCESSANDO`;
* visualizar `SUCESSO`;
* visualizar `FALHA` e a mensagem de erro.

A interface funciona como um cliente HTTP e utiliza:

```text
POST /api/pedidos
```

para enviar pedidos e:

```text
GET /api/pedidos/status/{id}
```

para consultar os respectivos status.

O polling é executado de forma assíncrona para não bloquear a interface Swing.

## Execução local sem RabbitMQ

O profile padrão é `local`.

Na raiz do projeto execute:

```bash
mvn spring-boot:run
```

No Windows PowerShell:

```powershell
mvn spring-boot:run
```

A aplicação irá:

1. iniciar o servidor HTTP;
2. iniciar o consumidor em memória;
3. abrir a interface Swing;
4. disponibilizar a API REST.

Nenhum RabbitMQ é necessário.

## Execução com RabbitMQ

Primeiro, inicie o RabbitMQ através do Docker Compose:

```bash
docker compose up -d
```

Verifique se o container está executando:

```bash
docker ps
```

Depois execute a aplicação utilizando o profile `rabbit`.

### Linux / macOS

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=rabbit
```

### Windows PowerShell

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=rabbit"
```

A aplicação passará a utilizar RabbitMQ real.

## RabbitMQ

As seguintes filas são utilizadas por padrão:

```text
pedidos.entrada.rodrigo
pedidos.entrada.rodrigo.dlq
pedidos.status.sucesso.rodrigo
pedidos.status.falha.rodrigo
```

A fila de entrada possui configuração de Dead Letter Queue.

Quando um pedido falha durante o processamento, ele é rejeitado sem requeue:

```java
channel.basicReject(deliveryTag, false);
```

O RabbitMQ encaminha a mensagem para:

```text
pedidos.entrada.rodrigo.dlq
```

## Configuração do RabbitMQ

Por padrão:

```text
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest
RABBITMQ_VHOST=/
```

O nome das filas pode ser alterado através da variável:

```text
PEDIDOS_SUFFIX=seu-nome
```

Por exemplo:

```text
PEDIDOS_SUFFIX=rodrigo
```

produz:

```text
pedidos.entrada.rodrigo
pedidos.entrada.rodrigo.dlq
pedidos.status.sucesso.rodrigo
pedidos.status.falha.rodrigo
```

## CloudAMQP

O projeto também pode utilizar uma instância RabbitMQ hospedada, como CloudAMQP.

Configure as variáveis de ambiente fornecidas pelo serviço:

```text
RABBITMQ_HOST
RABBITMQ_PORT
RABBITMQ_USERNAME
RABBITMQ_PASSWORD
RABBITMQ_VHOST
```

Para conexões TLS, utilize a porta e as configurações fornecidas pela instância.

## API REST

### Criar pedido

```http
POST /api/pedidos
Content-Type: application/json
```

Exemplo:

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "produto": "Notebook",
  "quantidade": 2,
  "dataCriacao": "2026-10-06T09:00:00"
}
```

Resposta:

```http
HTTP 202 Accepted
```

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "RECEBIDO",
  "mensagem": "Pedido recebido para processamento"
}
```

### Consultar status

```http
GET /api/pedidos/status/{id}
```

Exemplo:

```http
GET /api/pedidos/status/550e8400-e29b-41d4-a716-446655440000
```

Resposta durante o processamento:

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PROCESSANDO",
  "mensagemErro": null,
  "dataAtualizacao": "2026-10-06T09:00:02"
}
```

Resposta após sucesso:

```json
{
  "id": "550e8400
```
