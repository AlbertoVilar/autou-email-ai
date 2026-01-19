# AutoU Email AI

Aplicação web em Spring Boot para classificar e-mails como **PRODUTIVO** ou
**IMPRODUTIVO** e sugerir uma resposta em português usando a API da OpenAI.

Interface simples em HTML/Thymeleaf, preparada para deploy em plataformas como
Render.

## Visão geral da solução

- Entrada via UI web:
  - Colar texto de e-mail.
  - Fazer upload de arquivo `.txt` ou `.pdf`.
- Serviço de aplicação:
  - Valida entrada.
  - Extrai texto (txt/pdf).
  - Normaliza unicode e remove caracteres de controle.
  - Chama a OpenAI e converte o resultado para o domínio.
- Saída:
  - Categoria (PRODUTIVO / IMPRODUTIVO).
  - Confiança (0–1, exibida em %).
  - Motivo curto.
  - Resposta sugerida pronta para copiar.

## Arquitetura

O projeto segue o estilo **Ports & Adapters (Hexagonal)**, organizado em
pacotes:

- `application`
  - Contém o caso de uso principal (`EmailAnalysisService`) que implementa
    `EmailAnalysisUseCase`.
  - Orquestra validações, extração de texto, normalização e chamada à IA.
- `domain`
  - Modelos de domínio puros:
    - `EmailAnalysisResult`
    - `EmailCategory` (PRODUTIVO / IMPRODUTIVO)
- `infrastructure`
  - `ai`: implementação de `AiClient` via `OpenAiClient`, que chama a API
    da OpenAI usando `RestClient` e `ObjectMapper`.
  - `file`: adaptadores `PdfTextExtractor` e `TxtTextExtractor`, que
    implementam `FileTextExtractor`.
  - `config`: `OpenAiConfig` expõe o `RestClient` configurado com base URL e
    header de autorização.
- `web`
  - `EmailUiController` expõe os endpoints da UI (GET `/`, POST `/analyze-text`,
    POST `/analyze-file`).
  - `UiExceptionHandler` trata erros conhecidos e exibe mensagens amigáveis
    na tela.
  - DTOs para transportar dados entre camada web e aplicação.
- `support`
  - Utilitários de apoio (ex.: pré-processamento de texto).

Benefícios:

- Baixo acoplamento entre domínio, aplicação e infraestrutura.
- Possibilidade de trocar o provedor de IA ou extratores de arquivo sem
  impactar o domínio.
- Facilita testes unitários e de integração.

## Tecnologias empregadas

- **Linguagem:** Java 21
- **Framework:** Spring Boot 3.4.2 (Spring Web MVC, Bean Validation, RestClient)
- **Template engine:** Thymeleaf
- **Build:** Maven
- **Serialização JSON:** Jackson
- **PDF:** Apache PDFBox
- **Utilitários:** Lombok (opcional, apenas em tempo de compilação)
- **IA:** OpenAI API (Responses API, com JSON Schema)
- **Front-end:** HTML + JavaScript vanilla (cópia de resposta via clipboard)
- **Deploy:** preparado para plataformas PaaS (ex.: Render), com `server.port`
  configurado via variável `PORT`.

## Como rodar localmente

### Pré-requisitos

- JDK 21+
- Maven 3.9+ (ou compatível)
- Conta na OpenAI com chave de API (para testar com IA real)

### Passos

1. Clonar o repositório

2. Configurar as variáveis de ambiente (opcional para subir a aplicação,
   obrigatório para usar IA real):

   - `OPENAI_API_KEY` – chave da OpenAI.
   - `OPENAI_MODEL` – opcional, default: `gpt-4o-mini`.

3. Subir a aplicação em modo de desenvolvimento:

   ```bash
   mvn spring-boot:run
   ```

   A aplicação ficará disponível em:

   - `http://localhost:8080`

4. (Opcional) Gerar o artefato JAR:

   ```bash
   mvn -q -DskipTests package
   java -jar target/autou-email-ai-0.0.1-SNAPSHOT.jar
   ```

## Configuração de OpenAI

Arquivo [`src/main/resources/application.properties`](src/main/resources/application.properties):

```properties
spring.application.name=autou-email-ai
openai.base-url=https://api.openai.com/v1
openai.api-key=${OPENAI_API_KEY:}
openai.model=${OPENAI_MODEL:gpt-4o-mini}
server.port=${PORT:8080}
```

Pontos importantes:

- Sem `OPENAI_API_KEY`, a aplicação sobe, mas qualquer chamada à IA resulta
  em erro amigável de chave não configurada.
- `OPENAI_MODEL` é configurável por ambiente.
- `server.port` lê a variável `PORT`, facilitando deploy em Render.

## Fluxo da aplicação

1. Usuário acessa `GET /` e vê o formulário em `index.html`.
2. O usuário escolhe:
   - Colar texto e enviar para `POST /analyze-text`, ou
   - Enviar arquivo `.txt`/`.pdf` para `POST /analyze-file`.
3. O `EmailUiController` chama o caso de uso `EmailAnalysisUseCase`.
4. `EmailAnalysisService`:
   - Valida entrada.
   - Se arquivo, escolhe o `FileTextExtractor` adequado (txt/pdf) e extrai o
     texto.
   - Normaliza unicode e remove caracteres de controle.
   - Chama `AiClient` (implementado por `OpenAiClient`).
5. `OpenAiClient`:
   - Monta payload com JSON Schema para a Responses API.
   - Envia a requisição para a OpenAI.
   - Extrai o texto de saída do JSON e mapeia para `AiAnalysisResponse`.
6. O serviço converte `AiAnalysisResponse` em `EmailAnalysisResult`.
7. A camada web converte o resultado em `AnalyzeResultViewModel` e exibe
   categoria, confiança, motivo e resposta sugerida na tela.

Em caso de erro conhecido (`AiNotConfiguredException`, `AiQuotaException`,
`AiRequestFailedException`, `InvalidAiResponseException`,
`InvalidFileException`, `IllegalArgumentException`), o `UiExceptionHandler`
mostra uma mensagem amigável e limpa o resultado na tela.

## Cenários de teste manual

1. Sem `OPENAI_API_KEY`:
   - Acessar a página, colar um texto e enviar.
   - Esperado: mensagem de erro informando chave de IA não configurada.

2. Com `OPENAI_API_KEY` válida:
   - Colar um texto simples (por exemplo, pedido de reunião).
   - Esperado: categoria, confiança, motivo e resposta sugerida em PT-BR.

3. Upload de PDF:
   - Enviar um PDF com texto selecionável.
   - Esperado: o texto é extraído, analisado e exibido com os mesmos campos.

4. Upload de arquivo inválido:
   - Enviar arquivo vazio ou tipo não suportado.
   - Esperado: mensagem de erro orientando a usar `.txt` ou `.pdf`.

## Custos e limites

O uso da API da OpenAI gera custo por token. Recomendações:

- Utilizar chaves de teste e monitorar o consumo.
- Aplicar limites de uso em ambientes compartilhados.
- Evitar enviar textos muito longos em ambientes de demonstração.

---

Este projeto foi desenvolvido como estudo/prática de integração com IA e
arquitetura em camadas com Ports & Adapters.
