# Kifeito Tasks

O **Kifeito Tasks** é responsável pelo gerenciamento das tarefas do usuário.

O serviço permite criar, consultar, atualizar, concluir, reabrir, cancelar, reativar e excluir tarefas.

O serviço também é responsável por manter a data e o horário de agendamento da tarefa e comunicar eventos relacionados ao seu ciclo de vida ao **Kifeito Notification**.

---

<a id="indice"></a>

## 📋 Índice

1. [🎯 Responsabilidade](#-responsabilidade)
2. [📝 Criar tarefa](#-criar-tarefa)
3. [🔎 Consultar tarefas](#-consultar-tarefas)
4. [✏️ Atualizar tarefa](#️-atualizar-tarefa)
5. [✅ Concluir tarefa](#-concluir-tarefa)
6. [🔄 Reabrir tarefa](#-reabrir-tarefa)
7. [🔓 Reativar tarefa cancelada](#-reativar-tarefa-cancelada)
8. [❌ Cancelar tarefa](#-cancelar-tarefa)
9. [🗑️ Excluir tarefa](#️-excluir-tarefa)
10. [📨 Integração com Notification](#-integração-com-notification)
11. [💾 Persistência](#-persistência)
12. [🔗 Comunicação](#-comunicação)
13. [🛠️ Tecnologias](#️-tecnologias)
14. [📁 Estrutura](#-estrutura)
15. [⚙️ Configuração](#️-configuração)
16. [🐳 Docker](#-docker)
17. [🧪 Testes](#-testes)
18. [🚫 Fora do escopo da versão 1](#-fora-do-escopo-da-versão-1)
19. [🚀 Versão 2](#-versao-2)
20. [📄 Licença](#-licença)

---

<a id="responsabilidade"></a>

# 🎯 Responsabilidade

O serviço possui uma responsabilidade específica:

> **Gerenciar as tarefas do usuário e seu ciclo de vida, incluindo seu agendamento.**

O **Kifeito Tasks** é o responsável pela tarefa e pela sua `scheduledAt`.

O **Kifeito Notification** é responsável pelos lembretes associados às tarefas.

| Responsabilidade | Serviço |
|---|---|
| Tarefa | Tasks |
| Data e horário do agendamento | Tasks |
| Status da tarefa | Tasks |
| Ciclo de vida da tarefa | Tasks |
| Lembrete | Notification |
| Horário de envio do lembrete | Notification |
| Status do lembrete | Notification |
| Envio de e-mail | Notification |

O `userId` identifica o proprietário da tarefa e é obtido a partir da identidade autenticada.

O `userId` não deve ser recebido pelo cliente para determinar a propriedade da tarefa.

⬆️ [Voltar ao índice](#indice)

---

<a id="criar-tarefa"></a>

# 📝 Criar tarefa

O usuário autenticado pode criar uma nova tarefa.

**POST /tasks**

### Fluxo

```text
JWT
  ↓
identifica userId
  ↓
valida dados
  ↓
cria Task
  ↓
persiste
  ↓
publica TaskCreated
```

### Regras

- `title` é obrigatório;
- `description` é opcional;
- `scheduledAt` é opcional na versão 1;
- `userId` não vem do corpo da requisição;
- `userId` é obtido da identidade autenticada;
- `id` é gerado pelo sistema;
- O status inicial é `PENDING`;
- `createdAt` é gerado pelo sistema;
- `updatedAt` é gerado/controlado pelo sistema;
- Quando a tarefa possuir `scheduledAt`, o Tasks publica um evento para o Notification;
- O Tasks não cria nem controla diretamente o lembrete.

### Evento

Quando uma tarefa com agendamento é criada:

```text
TaskCreated
```

O evento contém as informações necessárias para que o Notification crie o lembrete.

⬆️ [Voltar ao índice](#indice)

---

<a id="consultar-tarefas"></a>

# 🔎 Consultar tarefas

O usuário pode consultar **somente as próprias tarefas**.

A identidade do usuário é obtida através do JWT.

---

## 3.1. Listar tarefas

Retorna todas as tarefas pertencentes ao usuário autenticado.

**GET /tasks**

### Fluxo

```text
JWT
  ↓
identifica userId
  ↓
busca tarefas do usuário
  ↓
retorna lista de tarefas
```

### Regras

- A consulta deve considerar apenas tarefas pertencentes ao `userId` autenticado.
- O cliente não informa o `userId` como critério de propriedade.

---

## 3.2. Consultar uma tarefa específica

Retorna uma tarefa específica pertencente ao usuário autenticado.

**GET /tasks/{id}**

### Fluxo

```text
JWT
  ↓
identifica userId
  ↓
recebe id da tarefa
  ↓
verifica se a tarefa pertence ao usuário
  ↓
retorna tarefa
```

### Regras

- A tarefa deve ser identificada pelo `id`;
- Somente o proprietário pode consultar a tarefa;
- O `userId` é obtido através do JWT;
- Uma tarefa pertencente a outro usuário não deve ser exposta.

⬆️ [Voltar ao índice](#indice)

---

<a id="atualizar-tarefa"></a>

# ✏️ Atualizar tarefa

O usuário pode alterar sua própria tarefa, inclusive reagendando-a.

**PUT /tasks/{id}**

### Campos que podem ser alterados

```text
title
description
scheduledAt
```

### Fluxo

```text
JWT
  ↓
identifica userId
  ↓
localiza tarefa
  ↓
verifica propriedade
  ↓
valida dados
  ↓
atualiza tarefa
  ↓
persiste
```

### Regras

- Apenas o usuário proprietário pode atualizar a tarefa;
- O `userId` é obtido através do JWT;
- O cliente não pode alterar o `userId`;
- O `id` da tarefa identifica o recurso que será atualizado;
- `updatedAt` é atualizado pelo sistema;
- O status da tarefa não é alterado através dessa operação;
- Quando `scheduledAt` for alterado, o Tasks deve comunicar a alteração ao Notification;
- O Notification será responsável por ajustar o lembrete associado.

### Evento

Quando o agendamento for alterado:

```text
TaskScheduledDateChanged
```

O Notification utiliza o novo `scheduledAt` para recalcular o horário do lembrete.

⬆️ [Voltar ao índice](#indice)

---

<a id="concluir-tarefa"></a>

# ✅ Concluir tarefa

Uma tarefa com status `PENDING` pode ser concluída pelo próprio usuário.

A conclusão representa uma **ação de domínio**, portanto será exposta através de uma operação específica.

**PATCH /tasks/{id}/complete**

### Fluxo

```text
JWT
  ↓
identifica userId
  ↓
localiza tarefa
  ↓
verifica propriedade
  ↓
valida status
  ↓
PENDING → COMPLETED
  ↓
persiste
  ↓
publica TaskCompleted
```

### Regras

- Apenas o usuário proprietário da tarefa pode concluí-la;
- Apenas uma tarefa `PENDING` pode ser concluída;
- O status é alterado de `PENDING` para `COMPLETED`;
- O cliente não informa o status no corpo da requisição;
- A identidade do proprietário é obtida através do JWT;
- A conclusão não ocorre automaticamente quando `scheduledAt` é atingido;
- Após a conclusão, o Tasks comunica o evento ao Notification;
- Caso exista um lembrete pendente, o Notification deverá cancelá-lo;
- Caso o lembrete já tenha sido enviado, ele permanece como enviado.

### Evento

```text
TaskCompleted
```

⬆️ [Voltar ao índice](#indice)

---

<a id="reabrir-tarefa"></a>

# 🔄 Reabrir tarefa

Uma tarefa `COMPLETED` pode ser reaberta pelo próprio usuário, retornando ao status `PENDING`.

A reabertura representa uma **ação de domínio**, portanto será exposta através de uma operação específica.

**PATCH /tasks/{id}/reopen**

### Fluxo

```text
JWT
  ↓
identifica userId
  ↓
localiza tarefa
  ↓
verifica propriedade
  ↓
valida status
  ↓
COMPLETED → PENDING
  ↓
persiste
  ↓
publica TaskReopened
```

### Regras

- Apenas o usuário proprietário da tarefa pode reabri-la;
- Apenas uma tarefa `COMPLETED` pode ser reaberta;
- O status é alterado de `COMPLETED` para `PENDING`;
- A `scheduledAt` existente é preservada;
- A reabertura não altera automaticamente o agendamento;
- O cliente não informa o status no corpo da requisição;
- A identidade do proprietário é obtida através do JWT;
- Se o agendamento ainda estiver no futuro, o Notification poderá criar um novo lembrete;
- Se o agendamento já tiver passado, nenhum novo lembrete será criado automaticamente;
- Nesse caso, o usuário deverá reagendar a tarefa caso queira receber um novo lembrete.

### Evento

```text
TaskReopened
```

⬆️ [Voltar ao índice](#indice)

---

<a id="reativar-tarefa-cancelada"></a>

# 🔓 Reativar tarefa cancelada

Uma tarefa com status `CANCELLED` pode ser reativada pelo próprio usuário, retornando ao status `PENDING`.

A reativação representa uma **ação de domínio**, portanto será exposta através de uma operação específica.

**PATCH /tasks/{id}/reactivate**

### Fluxo

```text
JWT
  ↓
identifica userId
  ↓
localiza tarefa
  ↓
verifica propriedade
  ↓
valida status
  ↓
CANCELLED → PENDING
  ↓
persiste
  ↓
publica TaskReactivated
```

### Regras

- Apenas o usuário proprietário pode reativar a tarefa;
- Apenas uma tarefa `CANCELLED` pode ser reativada;
- O status é alterado de `CANCELLED` para `PENDING`;
- O cliente não informa o status no corpo da requisição;
- A identidade do proprietário é obtida através do JWT;
- A `scheduledAt` existente é preservada;
- A data de agendamento não é alterada automaticamente;
- Se `scheduledAt` estiver no futuro, o Notification poderá criar um novo lembrete;
- Se `scheduledAt` já tiver passado, nenhum novo lembrete será criado automaticamente.

### Evento

```text
TaskReactivated
```

⬆️ [Voltar ao índice](#indice)

---

<a id="cancelar-tarefa"></a>

# ❌ Cancelar tarefa

Uma tarefa com status `PENDING` pode ser cancelada explicitamente pelo próprio usuário.

O cancelamento representa uma **ação de domínio**, portanto será exposto através de uma operação específica.

**PATCH /tasks/{id}/cancel**

### Fluxo

```text
JWT
  ↓
identifica userId
  ↓
localiza tarefa
  ↓
verifica propriedade
  ↓
valida status
  ↓
PENDING → CANCELLED
  ↓
persiste
  ↓
publica TaskCancelled
```

### Regras

- Apenas o usuário proprietário pode cancelar a tarefa;
- Apenas uma tarefa `PENDING` pode ser cancelada;
- O status é alterado de `PENDING` para `CANCELLED`;
- O cliente não informa o status no corpo da requisição;
- A identidade do proprietário é obtida através do JWT;
- A `scheduledAt` existente é preservada;
- Uma tarefa cancelada não deve gerar ou manter um lembrete ativo;
- O Tasks não controla nem remove diretamente o lembrete;
- O cancelamento da tarefa deve ser comunicado ao Notification.

### Evento

```text
TaskCancelled
```

⬆️ [Voltar ao índice](#indice)

---

<a id="excluir-tarefa"></a>

# 🗑️ Excluir tarefa

O usuário pode excluir uma tarefa de sua propriedade.

A exclusão é **definitiva na versão 1**. Após a exclusão, a tarefa não poderá mais ser consultada ou recuperada.

**DELETE /tasks/{id}**

### Fluxo

```text
JWT
  ↓
identifica userId
  ↓
localiza tarefa
  ↓
verifica propriedade
  ↓
exclui tarefa
  ↓
publica TaskDeleted
```

### Regras

- Apenas o usuário proprietário pode excluir a tarefa;
- A tarefa deve ser identificada pelo `id`;
- A identidade do proprietário é obtida através do JWT;
- O cliente não informa `userId` para determinar a propriedade;
- Os dados da tarefa são removidos do serviço Tasks;
- Caso exista um lembrete associado à tarefa, o Tasks deve comunicar a exclusão ao Notification;
- O Tasks não controla nem remove diretamente o lembrete;
- A exclusão não permite recuperação da tarefa na versão 1.

### Evento

```text
TaskDeleted
```

⬆️ [Voltar ao índice](#indice)

---

<a id="integracao-com-notification"></a>

# 📨 Integração com Notification

Tarefas que possuem `scheduledAt` podem possuir um lembrete associado, gerenciado pelo **Kifeito Notification**.

O Tasks é responsável pelo agendamento da tarefa.

O Notification é responsável pelo lembrete e pelo horário em que a notificação será enviada.

### Responsabilidades

| Informação | Responsável |
|---|---|
| Tarefa | Tasks |
| `scheduledAt` | Tasks |
| Status da tarefa | Tasks |
| Lembrete | Notification |
| `scheduledFor` | Notification |
| Status do lembrete | Notification |
| `sentAt` | Notification |

### Eventos publicados

O Tasks publica eventos relacionados ao ciclo de vida da tarefa:

```text
TaskCreated
TaskScheduledDateChanged
TaskCompleted
TaskReopened
TaskCancelled
TaskReactivated
TaskDeleted
```

### Regras

- O Tasks não armazena o estado do lembrete;
- O Tasks não controla o envio do e-mail;
- O Tasks não acessa o banco de dados do Notification;
- O Tasks não remove diretamente lembretes;
- O Tasks comunica alterações através de eventos;
- O Notification processa os eventos e atualiza seu próprio domínio;
- Quando `scheduledAt` for alterado, o Notification recalcula `scheduledFor`;
- Quando uma tarefa for concluída antes do envio do lembrete, o Notification cancela o lembrete pendente;
- Quando uma tarefa for cancelada, o Notification cancela o lembrete;
- Quando uma tarefa for excluída, o Notification remove ou invalida o lembrete associado;
- Quando uma tarefa for reaberta ou reativada com agendamento futuro, o Notification poderá criar um novo lembrete.

### Exemplo

```text
Tasks
  │
  │ TaskCreated
  │ TaskScheduledDateChanged
  │ TaskCompleted
  │ TaskReopened
  │ TaskCancelled
  │ TaskReactivated
  │ TaskDeleted
  ▼
RabbitMQ
  │
  ▼
Notification
```

O `eventId` identifica unicamente cada evento e permite que o Notification controle o processamento duplicado.

⬆️ [Voltar ao índice](#indice)

---

<a id="persistencia"></a>

# 💾 Persistência

O Kifeito Tasks possui **banco de dados próprio**.

A persistência é responsável exclusivamente pelos dados pertencentes ao domínio de tarefas.

### Entidade `Task`

| Campo | Descrição |
|---|---|
| `id` | Identificador único da tarefa |
| `userId` | Usuário proprietário |
| `title` | Título da tarefa |
| `description` | Descrição da tarefa |
| `scheduledAt` | Data e horário planejados para a tarefa |
| `status` | `PENDING`, `COMPLETED` ou `CANCELLED` |
| `createdAt` | Data e hora de criação |
| `updatedAt` | Data e hora da última alteração |

O `scheduledAt` possui uma única fonte de verdade dentro do domínio: **Kifeito Tasks**.

O Notification não deve manter uma cópia do `scheduledAt` original da tarefa.

⬆️ [Voltar ao índice](#indice)

---

<a id="comunicacao"></a>

# 🔗 Comunicação

A comunicação entre Tasks e Notification é assíncrona.

```text
┌──────────────┐
│     Tasks    │
└──────┬───────┘
       │
       │ Eventos
       ▼
┌──────────────┐
│   RabbitMQ   │
└──────┬───────┘
       │
       ▼
┌──────────────┐
│ Notification │
└──────────────┘
```

### RabbitMQ

Responsável pelo transporte assíncrono dos eventos relacionados ao ciclo de vida das tarefas.

O Tasks publica os eventos e o Notification os consome.

O Tasks não precisa aguardar o processamento do lembrete para concluir uma operação de tarefa.

⬆️ [Voltar ao índice](#indice)

---

<a id="tecnologias"></a>

# 🛠️ Tecnologias

| Tecnologia | Utilização |
|---|---|
| Java 17 | Linguagem |
| Spring Boot | Framework |
| Spring Web | Desenvolvimento da API REST |
| Spring Data JPA | Persistência |
| PostgreSQL | Banco de dados |
| Spring Security | Autenticação e autorização |
| JWT | Identificação do usuário autenticado |
| Spring AMQP | Integração com RabbitMQ |
| RabbitMQ | Mensageria assíncrona |
| Gradle | Build |
| Docker | Containerização |
| GitHub Actions | CI |

⬆️ [Voltar ao índice](#indice)

---

<a id="estrutura"></a>

# 📁 Estrutura

Estrutura inicial:

```text
kifeito-tasks
│
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com.jefferson.tasks
│   │   │       │
│   │   │       ├── controller
│   │   │       │   └── TaskController.java
│   │   │       │
│   │   │       ├── dto
│   │   │       │   ├── TaskRequestDTO.java
│   │   │       │   └── TaskResponseDTO.java
│   │   │       │
│   │   │       ├── entity
│   │   │       │   └── Task.java
│   │   │       │
│   │   │       ├── enums
│   │   │       │   └── TaskStatus.java
│   │   │       │
│   │   │       ├── exception
│   │   │       │   └── TaskException.java
│   │   │       │
│   │   │       ├── repository
│   │   │       │   └── TaskRepository.java
│   │   │       │
│   │   │       ├── service
│   │   │       │   └── TaskService.java
│   │   │       │
│   │   │       ├── messaging
│   │   │       │   └── TaskEventPublisher.java
│   │   │       │
│   │   │       ├── security
│   │   │       │   └── SecurityConfig.java
│   │   │       │
│   │   │       └── TasksApplication.java
│   │   │
│   │   └── resources
│   │       └── application.yaml
│
├── .github
│   └── workflows
│       └── pull-request.yml
│
├── .gitignore
├── Dockerfile
├── build.gradle
├── gradlew
├── gradlew.bat
└── README.md
```

⬆️ [Voltar ao índice](#indice)

---

<a id="configuracao"></a>

# ⚙️ Configuração

As configurações do banco de dados e do RabbitMQ são fornecidas por variáveis de ambiente.

```text
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD

RABBITMQ_HOST
RABBITMQ_PORT
RABBITMQ_USERNAME
RABBITMQ_PASSWORD

JWT_SECRET
```

As informações sensíveis não devem ser armazenadas diretamente no código-fonte.

> O arquivo `.env` não deve ser versionado.

⬆️ [Voltar ao índice](#indice)

---

<a id="docker"></a>

# 🐳 Docker

O Kifeito Tasks possui seu próprio `Dockerfile` e pode ser executado junto aos demais serviços através do Docker Compose.

O Docker garante um ambiente de execução padronizado, facilita a configuração e permite executar os serviços de forma isolada e reproduzível.

⬆️ [Voltar ao índice](#indice)

---

<a id="testes"></a>

# 🧪 Testes

O serviço terá testes unitários para as regras de negócio e testes de integração para suas principais integrações.

- **Testes unitários:** regras de negócio e ciclo de vida das tarefas.
- **Testes de integração:** PostgreSQL, RabbitMQ e autenticação.

⬆️ [Voltar ao índice](#indice)

---

<a id="fora-do-escopo-da-versao-1"></a>

# 🚫 Fora do escopo da versão 1

Para manter a complexidade proporcional à necessidade do sistema, a versão 1 não possui:

- Tarefas recorrentes;
- Regras avançadas de recorrência;
- Múltiplos lembretes;
- Prioridade;
- Categorias;
- Tags;
- Busca avançada;
- Filtros avançados;
- Paginação;
- Ordenação configurável;
- Compartilhamento de tarefas;
- Colaboração entre usuários;
- Projetos compartilhados;
- Equipes;
- Tarefas de outros usuários;
- Recuperação de tarefas excluídas.

Esses recursos poderão ser avaliados em versões futuras conforme a necessidade do produto.

⬆️ [Voltar ao índice](#indice)

---

<a id="versao-2"></a>

# 🚀 Versão 2

A versão 2 passa a enriquecer o gerenciamento das tarefas.

Os detalhes dessas funcionalidades deverão ser definidos quando a versão 2 for planejada e implementada.

---

## 1. Tarefas recorrentes

Permitir que uma tarefa possua uma configuração de recorrência.

Exemplos:

```text
Academia
Toda terça-feira às 19:00

Trabalhar
Segunda a sexta às 08:00

Pagar conta
Todo dia 10
```

A tarefa deixa de possuir somente:

```text
scheduledAt
```

e passa a poder possuir uma configuração de recorrência.

Conceitualmente:

```text
Task
├── scheduledAt
└── recurrence
    ├── frequency
    ├── interval
    ├── daysOfWeek
    └── endDate
```

Os detalhes do modelo ficam para quando a funcionalidade for implementada.

---

## 2. Regras de recorrência

A versão 2 deverá definir o comportamento de:

- Concluir tarefa recorrente;
- Cancelar tarefa recorrente;
- Reabrir tarefa recorrente;
- Alterar uma ocorrência;
- Alterar toda a série.

Essa complexidade não faz parte da versão 1.

---

## 3. Notificações mais avançadas

Na versão 1:

```text
1 lembrete
└── 1 hora antes
```

Na versão 2, poderá ser possível configurar:

```text
15 minutos antes
30 minutos antes
1 hora antes
1 dia antes
```

Também poderá existir mais de um lembrete para a mesma tarefa.

Exemplo:

```text
Task
 │
 ├── 1 dia antes
 ├── 1 hora antes
 └── 15 minutos antes
```

O gerenciamento desses lembretes continuará sendo responsabilidade do domínio de Notification.

---

## 4. Prioridade

Adicionar prioridade às tarefas:

```text
LOW
MEDIUM
HIGH
URGENT
```

Isso permitirá organização e filtragem mais avançadas.

---

## 5. Categorias

Permitir organizar tarefas por categorias.

Exemplos:

```text
Trabalho
Estudos
Pessoal
Saúde
Finanças
```

A funcionalidade exigirá evolução da modelagem do domínio.

---

## 6. Tags

Permitir adicionar múltiplas tags às tarefas.

Exemplos:

```text
#java
#faculdade
#backend
#urgente
```

Isso permitirá filtros mais flexíveis.

---

## 7. Busca e filtros avançados

A V2 poderá permitir consultas por:

- Status;
- Prioridade;
- Período;
- Categoria;
- Tag;
- Título.

Exemplo:

```text
GET /tasks?status=PENDING&priority=HIGH
```

---

## 8. Ordenação e paginação

Para uma aplicação com grande quantidade de tarefas, poderão ser adicionadas:

- Paginação;
- Ordenação;
- Filtros combinados.

Exemplo:

```text
GET /tasks?page=0&size=20&sort=scheduledAt,asc
```

Esses recursos serão avaliados conforme a necessidade real do produto.

⬆️ [Voltar ao índice](#indice)

---

<a id="licenca"></a>

# 📄 Licença

O Kifeito está sendo desenvolvido inicialmente para uso próprio e para um grupo limitado de usuários.

Apesar do uso inicial restrito, o projeto está sendo desenvolvido com arquitetura, práticas e estrutura voltadas para um produto comercial, podendo futuramente ser disponibilizado de forma mais ampla.

O código-fonte, a aplicação, a identidade visual, a documentação e demais componentes do projeto são de propriedade do próprio autor.

A utilização, cópia, modificação, distribuição ou comercialização de qualquer parte do projeto depende de autorização expressa do detentor dos direitos.

⬆️ [Voltar ao índice](#indice)
