# autou-email-ai

Projeto Spring Boot para classificar e-mails (PRODUTIVO/IMPRODUTIVO) e sugerir resposta.

## Como rodar localmente

1) Build
```
mvn -q -DskipTests package
```

2) Subir app
```
mvn spring-boot:run
```

App: `http://localhost:8080`

## Variaveis de ambiente (Render)

- `OPENAI_API_KEY` (obrigatoria para usar IA)
- `OPENAI_MODEL` (opcional, default: `gpt-4o-mini`)

## Configuracao de OpenAI

`src/main/resources/application.properties`:
```
openai.base-url=https://api.openai.com/v1
openai.api-key=${OPENAI_API_KEY:}
openai.model=${OPENAI_MODEL:gpt-4o-mini}
```

## Testes manuais rapidos

1) Sem `OPENAI_API_KEY`:
   - Colar texto e enviar -> mostra erro amigavel de chave nao configurada.
2) Com `OPENAI_API_KEY`:
   - Colar texto simples -> retorna categoria, confianca, motivo e resposta sugerida.
3) Upload PDF:
   - Enviar PDF com texto -> retorna analise e resposta sugerida.

## Custos e limites

O uso da API da OpenAI tem custo por token. Use com moderacao e limite
volume em ambientes de teste.
