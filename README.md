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
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "SUCESSO",
  "mensagemErro": null,
  "dataAtualizacao": "2026-10-06T09:00:04"
}
```

Resposta após falha:

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "FALHA",
  "mensagemErro": "Falha simulada no processamento",
  "dataAtualizacao": "2026-10-06T09:00:04"
}
```

## Status do pedido

O ciclo normal de processamento é:

```text
RECEBIDO
    |
    v
PROCESSANDO
    |
    v
SUCESSO
```

ou:

```text
RECEBIDO
    |
    v
PROCESSANDO
    |
    v
FALHA
```

A aplicação possui uma probabilidade simulada de **20% de falha** durante o processamento.

## Processamento assíncrono

O processamento possui um atraso aleatório entre:

```text
1 segundo e 3 segundos
```

O atraso é utilizado para simular um processamento real e demonstrar o comportamento assíncrono.

O cliente Swing não fica bloqueado durante esse processamento.

## DLQ

### Modo local

No profile `local`, a DLQ é representada por uma segunda `BlockingQueue` em memória.

```text
entrada
   |
   v
processamento
   |
   +---- falha
          |
          v
         DLQ
```

### Modo RabbitMQ

No profile `rabbit`, a mensagem é rejeitada sem requeue:

```java
channel.basicReject(deliveryTag, false);
```

O RabbitMQ utiliza a configuração de Dead Letter para encaminhar a mensagem para:

```text
pedidos.entrada.rodrigo.dlq
```

## Armazenamento de status

O projeto não utiliza banco de dados.

Os status são mantidos em memória através de:

```java
ConcurrentHashMap<UUID, StatusAtual>
```

Isso atende ao objetivo do teste, que permite a ausência de banco de dados.

Consequentemente, os status são perdidos quando a aplicação é reiniciada.

## Tratamento de interrupção

O consumidor trata interrupções de processamento.

No modo RabbitMQ, uma interrupção resulta em:

```java
channel.basicNack(deliveryTag, false, true);
```

Ou seja, a mensagem pode ser reenfileirada.

Uma falha de processamento propriamente dita utiliza:

```java
channel.basicReject(deliveryTag, false);
```

permitindo que a mensagem seja encaminhada para a DLQ.

## Validação

O endpoint de criação de pedidos utiliza Bean Validation.

São validados:

* `id` não pode ser nulo;
* `produto` não pode ser vazio;
* `quantidade` deve ser maior ou igual a 1;
* `dataCriacao` não pode ser nula.

Payloads inválidos resultam em:

```text
HTTP 400 Bad Request
```

## Testes unitários

O projeto possui testes unitários para os principais componentes e fluxos.

### PedidoPublisherTest

Verifica se o pedido é publicado corretamente na fila de entrada.

### StatusServiceTest

Verifica:

* atualização do status;
* consulta do status;
* comportamento quando o pedido não existe.

### PedidoConsumerTest

Verifica:

* processamento com sucesso;
* atualização para `SUCESSO`;
* publicação do status de sucesso;
* `basicAck`;
* falha de processamento;
* publicação do status de falha;
* `basicReject`;
* encaminhamento para DLQ;
* interrupção;
* `basicNack` com requeue.

O consumidor recebe internamente fornecedores de atraso e aleatoriedade para tornar os testes determinísticos.

Dessa forma, os testes não dependem de:

* espera real de 1–3 segundos;
* geração aleatória;
* sorte para determinar sucesso ou falha.

### PedidoControllerTest

Verifica:

* criação do pedido;
* status inicial `RECEBIDO`;
* publicação do pedido;
* resposta HTTP `202`;
* consulta de status.

### PedidoControllerValidationTest

Verifica respostas `400 Bad Request` para payloads inválidos.

### ApiExceptionHandlerTest

Verifica o tratamento de:

* erros de validação;
* exceções genéricas.

### RabbitConfigTest

Verifica:

* nomes das filas;
* configuração da entrada;
* configuração da DLQ;
* filas de sucesso e falha;
* conversor Jackson;
* criação do `RabbitTemplate`;
* declaração das filas através de `Declarables`.

## Executar os testes

Execute:

```bash
mvn clean test
```

No Windows PowerShell:

```powershell
mvn clean test
```

Os testes não dependem de RabbitMQ externo.

## Gerar o JAR

Execute:

```bash
mvn clean package
```

O JAR será gerado em:

```text
target/pedidos-assincrono-1.0.0.jar
```

Executar:

```bash
java -jar target/pedidos-assincrono-1.0.0.jar
```

Por padrão, a aplicação utilizará o profile `local`.

## Executar o JAR com RabbitMQ

Depois de iniciar o RabbitMQ:

```bash
docker compose up -d
```

Execute:

```bash
java -jar target/pedidos-assincrono-1.0.0.jar --spring.profiles.active=rabbit
```

No Windows PowerShell:

```powershell
java -jar target/pedidos-assincrono-1.0.0.jar "--spring.profiles.active=rabbit"
```

## Configuração da URL da API no Desktop

A interface Swing utiliza:

```text
http://localhost:8080
```

por padrão.

É possível alterar essa URL através da variável de ambiente:

```text
PEDIDOS_API_URL
```

Exemplo:

```text
PEDIDOS_API_URL=http://localhost:8080
```

Isso permite executar o cliente desktop apontando para outro backend.

## Funcionamento do polling

Depois de enviar um pedido, o Swing mantém o UUID do pedido associado à linha correspondente da tabela.

Periodicamente, a aplicação executa:

```http
GET /api/pedidos/status/{id}
```

O status retornado pela API é refletido na tabela:

```text
ENVIADO, AGUARDANDO PROCESSO
             |
             v
        PROCESSANDO
             |
             v
          SUCESSO
```

ou:

```text
ENVIADO, AGUARDANDO PROCESSO
             |
             v
        PROCESSANDO
             |
             v
       FALHA - motivo
```

O polling é executado fora da Event Dispatch Thread do Swing para evitar congelamento da interface.

## Estrutura conceitual do projeto

```text
src
├── main
│   └── java
│       └── br.com.exemplo.pedidos
│           ├── config
│           │   └── RabbitConfig
│           │
│           ├── controller
│           │   └── PedidoController
│           │
│           ├── desktop
│           │   └── PedidosDesktop
│           │
│           ├── messaging
│           │   ├── PedidoPublisher
│           │   ├── InMemoryPedidoPublisher
│           │   └── InMemoryPedidoQueue
│           │
│           ├── model
│           │   ├── Pedido
│           │   ├── RespostaPedido
│           │   ├── StatusAtual
│           │   └── StatusPedido
│           │
│           ├── service
│           │   ├── PedidoConsumer
│           │   ├── InMemoryPedidoConsumer
│           │   ├── StatusService
│           │   └── ProcessamentoException
│           │
│           └── exception
│               └── ApiExceptionHandler
│
└── test
    └── java
        └── br.com.exemplo.pedidos
            ├── PedidoPublisherTest
            ├── StatusServiceTest
            ├── PedidoConsumerTest
            ├── PedidoControllerTest
            ├── PedidoControllerValidationTest
            ├── ApiExceptionHandlerTest
            └── RabbitConfigTest
```

## Observações importantes

### RabbitMQ não é necessário para a demonstração

O modo `local` permite que todo o fluxo assíncrono seja executado sem infraestrutura externa.

```bash
mvn spring-boot:run
```

é suficiente para executar:

```text
Spring Boot
    +
REST API
    +
Swing
    +
BlockingQueue
    +
Consumidor assíncrono
    +
DLQ em memória
```

### RabbitMQ é opcional

Para demonstrar mensageria real:

```bash
docker compose up -d
```

e execute com:

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=rabbit"
```

### Status não são persistentes

Como o projeto não utiliza banco de dados, reiniciar a aplicação elimina os status armazenados em memória.

### Processamento é propositalmente simulado

O atraso de 1–3 segundos e a probabilidade de 20% de falha existem para demonstrar:

* processamento assíncrono;
* atualização de status;
* tratamento de falhas;
* DLQ;
* polling;
* desacoplamento entre produtor e consumidor.

## Resumo da execução

### Sem RabbitMQ

```powershell
mvn clean test
mvn spring-boot:run
```

### Com RabbitMQ

```powershell
docker compose up -d
mvn spring-boot:run "-Dspring-boot.run.profiles=rabbit"
```

### Gerar JAR

```powershell
mvn clean package
```

### Executar JAR localmente

```powershell
java -jar target/pedidos-assincrono-1.0.0.jar
```

### Executar JAR com RabbitMQ

```powershell
java -jar target/pedidos-assincrono-1.0.0.jar "--spring.profiles.active=rabbit"
```

## Objetivo do projeto

O objetivo é demonstrar uma arquitetura de processamento assíncrono utilizando Java e Spring, mantendo o mecanismo de mensageria desacoplado e permitindo a execução tanto com uma infraestrutura local em memória quanto com RabbitMQ real.

O projeto demonstra conceitos importantes de sistemas distribuídos e backend, incluindo:

* APIs REST;
* processamento assíncrono;
* mensageria;
* RabbitMQ;
* filas;
* Dead Letter Queue;
* acknowledgements;
* requeue;
* profiles do Spring;
* desacoplamento por interfaces;
* Bean Validation;
* tratamento global de exceções;
* testes unitários determinísticos;
* cliente desktop Swing;
* polling assíncrono;
* concorrência e estruturas thread-safe.
