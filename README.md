# Sistema de Pedidos Desktop Assíncrono

Projeto Maven desenvolvido com **Java 21**, **Spring Boot**, **Spring AMQP**, **RabbitMQ** e **Java Swing**, demonstrando um sistema de pedidos com processamento assíncrono, polling de status, validação, tratamento de falhas e Dead Letter Queue (DLQ).

## Tecnologias

* Java 21
* Spring Boot 3.4.4
* Spring Web
* Spring AMQP
* RabbitMQ
* Java Swing
* Jackson
* Bean Validation
* JUnit 5
* Mockito
* Maven
* Docker / Docker Compose

---

# Quick Start

O projeto pode ser executado de duas formas:

* **Local:** sem RabbitMQ, utilizando filas em memória.
* **RabbitMQ:** utilizando um servidor RabbitMQ real.

## Executar sem RabbitMQ

O perfil `local` é o perfil padrão e não exige nenhuma infraestrutura externa.

```bash
mvn spring-boot:run
```

A aplicação será iniciada utilizando filas em memória.

A API ficará disponível em:

```text
http://localhost:8080
```

A interface gráfica Swing será aberta automaticamente.

---

## Executar com RabbitMQ

Para executar utilizando RabbitMQ, primeiro inicie o RabbitMQ.

### 1. Subir o RabbitMQ com Docker

```bash
docker compose up -d
```

Verifique se o container está em execução:

```bash
docker ps
```

### 2. Iniciar a aplicação com o perfil RabbitMQ

No PowerShell:

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=rabbit"
```

A aplicação será iniciada utilizando o RabbitMQ como mecanismo de mensageria.

### 3. Executar o JAR com RabbitMQ

Após gerar o projeto:

```bash
mvn clean package
```

Execute:

```powershell
java -jar target/pedidos-assincrono-1.0.0.jar "--spring.profiles.active=rabbit"
```

> **Importante:** no PowerShell, utilize as aspas no parâmetro `-Dspring-boot.run.profiles=rabbit`.

---

# Arquitetura

A aplicação possui uma arquitetura simples, porém demonstra conceitos importantes de sistemas distribuídos e processamento assíncrono.

```text
                    ┌──────────────────────┐
                    │     Java Swing       │
                    │    Desktop Client    │
                    └──────────┬───────────┘
                               │
                               │ HTTP
                               ▼
                    ┌──────────────────────┐
                    │    Spring Boot API   │
                    │                      │
                    │ PedidoController     │
                    │ StatusService        │
                    └──────────┬───────────┘
                               │
                               │ PedidoPublisher
                               ▼
                    ┌──────────────────────┐
                    │     Mensageria       │
                    │                      │
                    │ Local → Memory Queue │
                    │ Rabbit → RabbitMQ    │
                    └──────────┬───────────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │    PedidoConsumer    │
                    │                      │
                    │ Processamento Async  │
                    └──────────┬───────────┘
                               │
                    ┌──────────┴───────────┐
                    ▼                      ▼
             ┌─────────────┐       ┌─────────────┐
             │   SUCESSO   │       │    FALHA    │
             └─────────────┘       └──────┬──────┘
                                          │
                                          ▼
                                    ┌───────────┐
                                    │    DLQ    │
                                    └───────────┘
```

---

# Perfis de execução

## Perfil `local`

O perfil `local` é utilizado para desenvolvimento e testes sem necessidade de RabbitMQ.

O processamento utiliza:

```java
BlockingQueue
```

Existem duas filas em memória:

* fila de entrada;
* fila de DLQ.

O consumidor é executado de forma assíncrona utilizando um `ExecutorService`.

Para executar:

```bash
mvn spring-boot:run
```

---

## Perfil `rabbit`

O perfil `rabbit` utiliza um RabbitMQ real através do Spring AMQP.

Para executar:

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=rabbit"
```

Nesse modo:

* os pedidos são publicados no RabbitMQ;
* o consumidor utiliza `@RabbitListener`;
* mensagens processadas com sucesso recebem `basicAck`;
* mensagens com falha são rejeitadas;
* mensagens rejeitadas são encaminhadas para a DLQ;
* interrupções durante o processamento utilizam `basicNack` com requeue.

---

# RabbitMQ

O projeto utiliza quatro filas principais.

| Fila                             | Função                 |
| -------------------------------- | ---------------------- |
| `pedidos.entrada.rodrigo`        | Entrada dos pedidos    |
| `pedidos.entrada.rodrigo.dlq`    | Dead Letter Queue      |
| `pedidos.status.sucesso.rodrigo` | Notificação de sucesso |
| `pedidos.status.falha.rodrigo`   | Notificação de falha   |

O sufixo das filas é configurável através da propriedade:

```properties
app.pedidos.suffix
```

Por padrão:

```text
rodrigo
```

---

# Dead Letter Queue

Quando um pedido apresenta uma falha durante o processamento, o consumidor RabbitMQ rejeita a mensagem:

```java
channel.basicReject(deliveryTag, false);
```

A fila de entrada possui uma configuração de Dead Letter:

```text
pedidos.entrada.rodrigo
        │
        │ processamento
        │
        ├── sucesso ──> ACK
        │
        └── falha ────> REJECT
                         │
                         ▼
                pedidos.entrada.rodrigo.dlq
```

No modo `local`, o comportamento equivalente é implementado utilizando uma segunda `BlockingQueue` em memória.

---

# Processamento assíncrono

O processamento do pedido não é realizado de forma síncrona dentro da requisição HTTP.

Ao criar um pedido:

```http
POST /api/pedidos
```

a API:

1. valida a requisição;
2. cria o pedido;
3. registra o status `RECEBIDO`;
4. publica o pedido na fila;
5. retorna imediatamente `HTTP 202 Accepted`.

O processamento ocorre posteriormente pelo consumidor.

Fluxo:

```text
POST /api/pedidos
        │
        ▼
    RECEBIDO
        │
        ▼
     FILA
        │
        ▼
   PROCESSANDO
        │
        ├───────────────┐
        ▼               ▼
     SUCESSO           FALHA
                         │
                         ▼
                        DLQ
```

---

# Status do pedido

Os status possíveis são:

```text
RECEBIDO
PROCESSANDO
SUCESSO
FALHA
```

Os status são armazenados em memória utilizando:

```java
ConcurrentHashMap<UUID, StatusAtual>
```

Essa abordagem foi adotada porque o projeto não exige banco de dados.

---

# API REST

## Criar pedido

```http
POST /api/pedidos
Content-Type: application/json
```

Exemplo:

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "produto": "Notebook",
  "quantidade": 1,
  "dataCriacao": "2026-10-06T12:00:00"
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

---

## Consultar status

```http
GET /api/pedidos/status/{id}
```

Exemplo:

```bash
curl http://localhost:8080/api/pedidos/status/550e8400-e29b-41d4-a716-446655440000
```

Resposta:

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "SUCESSO",
  "mensagemErro": null,
  "atualizadoEm": "2026-10-06T12:00:03"
}
```

---

# Java Swing

A aplicação também possui uma interface gráfica desenvolvida com Java Swing.

A interface:

* permite informar produto e quantidade;
* cria novos pedidos;
* exibe os pedidos enviados;
* exibe o status atual;
* realiza polling periódico da API;
* atualiza automaticamente os pedidos concluídos;
* apresenta mensagens de erro quando ocorre uma falha.

O polling é realizado aproximadamente a cada:

```text
4 segundos
```

Após o processamento:

```text
PROCESSANDO
      │
      ▼
 SUCESSO / FALHA
```

o pedido deixa de ser acompanhado pelo polling.

A URL da API pode ser configurada através da variável de ambiente:

```text
PEDIDOS_API_URL
```

Por padrão:

```text
http://localhost:8080
```

Exemplo:

```powershell
$env:PEDIDOS_API_URL="http://localhost:8080"
```

---

# Simulação de processamento

Para demonstrar o comportamento assíncrono, o processamento possui uma simulação de:

```text
1 a 3 segundos
```

Também existe uma probabilidade de:

```text
20%
```

de falha simulada.

Isso permite testar facilmente:

* processamento assíncrono;
* sucesso;
* falhas;
* DLQ;
* atualização de status;
* polling do cliente.

---

# Tratamento de interrupção

Durante o processamento RabbitMQ, uma interrupção utiliza:

```java
channel.basicNack(deliveryTag, false, true);
```

O terceiro parâmetro `true` faz com que a mensagem seja recolocada na fila.

Isso permite diferenciar:

* **falha de negócio/processamento:** mensagem vai para DLQ;
* **interrupção temporária:** mensagem pode ser processada novamente.

---

# Validação

A API utiliza Bean Validation para validar os pedidos recebidos.

Exemplo:

```java
@NotNull UUID id
@NotBlank String produto
@Min(1) int quantidade
@NotNull LocalDateTime dataCriacao
```

Requisições inválidas retornam:

```http
HTTP 400 Bad Request
```

---

# Testes

O projeto possui testes unitários para os principais componentes.

Entre eles:

* `PedidoPublisherTest`
* `StatusServiceTest`
* `PedidoConsumerTest`
* `InMemoryPedidoConsumerTest`
* `PedidoControllerTest`
* `PedidoControllerValidationTest`
* `ApiExceptionHandlerTest`
* `RabbitConfigTest`

Os testes cobrem, entre outros cenários:

* publicação de pedidos;
* atualização e consulta de status;
* processamento com sucesso;
* falha durante processamento;
* envio para DLQ;
* `ACK`;
* `NACK` com requeue;
* validação da API;
* configuração das filas RabbitMQ;
* configuração do `RabbitTemplate`;
* conversão JSON.

Executar:

```bash
mvn test
```

---

# Configuração RabbitMQ

As configurações do RabbitMQ podem ser definidas através das propriedades da aplicação.

Exemplo:

```properties
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest
app.pedidos.suffix=rodrigo
```

Para ambientes externos, como CloudAMQP, essas propriedades podem ser sobrescritas por variáveis de ambiente.

Exemplo:

```text
SPRING_RABBITMQ_HOST
SPRING_RABBITMQ_PORT
SPRING_RABBITMQ_USERNAME
SPRING_RABBITMQ_PASSWORD
```

---

# Docker

Para iniciar a infraestrutura:

```bash
docker compose up -d
```

Verificar containers:

```bash
docker ps
```

Parar a infraestrutura:

```bash
docker compose down
```

---

# Executar o projeto completo

## Local

```bash
mvn clean package
mvn spring-boot:run
```

## RabbitMQ

```bash
docker compose up -d
mvn clean package
mvn spring-boot:run "-Dspring-boot.run.profiles=rabbit"
```

Ou utilizando o JAR:

```powershell
java -jar target/pedidos-assincrono-1.0.0.jar "--spring.profiles.active=rabbit"
```

---

# Estrutura do projeto

```text
src
├── main
│   ├── java
│   │   └── br.com.exemplo.pedidos
│   │       ├── config
│   │       ├── controller
│   │       ├── exception
│   │       ├── messaging
│   │       ├── model
│   │       ├── service
│   │       └── PedidosApplication.java
│   │
│   └── resources
│       └── application.properties
│
└── test
    └── java
        └── br.com.exemplo.pedidos
```

---

# Principais conceitos demonstrados

Este projeto foi desenvolvido para demonstrar na prática:

* Java 21;
* Spring Boot;
* arquitetura assíncrona;
* mensageria;
* RabbitMQ;
* Spring AMQP;
* `@RabbitListener`;
* ACK/NACK;
* Dead Letter Queue;
* requeue;
* processamento concorrente;
* polling;
* Java Swing;
* REST API;
* Bean Validation;
* tratamento global de exceções;
* testes unitários;
* Mockito;
* Docker;
* profiles do Spring;
* separação entre infraestrutura e regra de negócio.

---

# Objetivo

O objetivo do projeto é demonstrar uma implementação completa de um sistema de pedidos assíncrono, permitindo executar a mesma aplicação tanto em um ambiente local, sem infraestrutura externa, quanto em um ambiente utilizando RabbitMQ.

A arquitetura permite demonstrar conceitos comuns em aplicações distribuídas, como:

```text
API
 │
 ▼
Mensageria
 │
 ▼
Processamento Assíncrono
 │
 ├── Sucesso
 │
 └── Falha
      │
      ▼
     DLQ
```

O projeto também demonstra como manter o código desacoplado do mecanismo de mensageria, permitindo alternar entre uma implementação em memória e RabbitMQ através dos profiles do Spring.
