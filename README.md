# DeskLand-Mobile

Projeto interdisciplinar do curso de Desenvolvimento de Software Multiplataforma (FATEC Mauá), combinando um aplicativo Android com práticas de integração contínua, testes de aceitação, contêineres, orquestração e entrega de artefatos.

> **Estado do projeto:** o aplicativo Android e os serviços de infraestrutura são partes distintas. O backend Node.js foi adaptado de uma API anterior de usuários/contatos/uploads. A integração das funcionalidades do catálogo, carrinho e pedidos com essa API ainda deve ser avaliada; a existência de ambos no repositório **não significa** que já estejam integrados.

## Visão geral

```text
Aplicativo Android (Kotlin + Jetpack Compose)
   └── CI: Gradle → testes → APK no GitHub Actions

API Node.js (Express)
   ├── Docker Compose (desenvolvimento)
   ├── Render.com (serviço HTTP público)
   ├── MongoDB Atlas (banco externo; conexão por MONGO_URI)
   └── Kubernetes kind (ambiente de demonstração independente do Render)
           ├── Deployment deskland-api (2 réplicas)
           └── Service deskland-api (ClusterIP)

GitHub Actions → CI Android, aceitação, backend, Docker e Kubernetes
GitHub Container Registry (GHCR) → imagens Docker publicadas pelo workflow Docker
```

O Render não é o cluster kind: são ambientes separados que podem executar a mesma API. O Kubernetes usado nos testes automatizados é temporário e não publica alterações no Render.

## Tecnologias

- **Android:** Kotlin, Jetpack Compose, Gradle e testes instrumentados.
- **Backend:** Node.js 22, Express e Mongoose.
- **Banco:** MongoDB Atlas, externo aos contêineres usados para produção/demonstração.
- **Contêineres:** Docker, Docker Compose e GitHub Container Registry.
- **Orquestração:** Kubernetes com kind, Deployment, Service e Secret.
- **Automação:** GitHub Actions.
- **Hospedagem da API:** Render.com.

## Estrutura do repositório

```text
.github/workflows/
  android-ci.yml            # Compilação, testes unitários e APK
  android-acceptance.yml    # Testes instrumentados no emulador
  backend-ci.yml            # Instalação e verificação de sintaxe do backend
  docker-ci.yml             # Build e publicação da imagem Docker no GHCR
  kubernetes-ci.yml         # Cluster temporário, MongoDB de teste e API
backend/
  app.js                    # Rotas Express, incluindo GET /health
  server.js                 # Inicialização do servidor
  controllers/ models/ routes/ middleware/
  scripts/check-mongodb.js  # Ping real ao MongoDB configurado
  .env.example              # Nomes de variáveis, sem valores reais
docker/
  Dockerfile                # Imagem Node.js da API
  Dockerfile.dockerignore   # Exclusões do contexto de build
kubernetes/
  deployment.yaml           # Duas réplicas da API
  service.yaml              # Service ClusterIP na porta 80
mobile/                     # Aplicativo Android Studio
compose.yaml                # API no Docker Compose
README.md
```

## Cinco workflows de GitHub Actions

| Workflow | Função | Resultado esperado |
|---|---|---|
| Android CI | Compilar projeto Android e executar tarefa de testes unitários | APK como artifact; a tarefa passar não prova que existam testes unitários cadastrados |
| Testes de Aceitação | Rodar testes de interface Compose em um emulador | Resultado dos testes instrumentados |
| Backend CI | Instalar dependências e verificar sintaxe de arquivos JavaScript | Aprovação das verificações estáticas (não equivale a teste de integração) |
| Docker CI/CD | Construir imagem, verificar arquivos e publicá-la no GHCR | Tags `latest` e `sha-...` na branch `main` |
| Kubernetes CI | Criar kind temporário, implantar MongoDB de teste e API, verificar `ping` e `GET /health` | Validação automatizada da implantação; não atualiza o cluster pessoal do Codespaces |

Os workflows possuem acionamento manual (`workflow_dispatch`) e alguns também são acionados por `push`/Pull Request, conforme seus filtros de caminhos.

## Rodar o backend localmente

1. Configure um arquivo privado `backend/.env` com as seguintes variáveis (sem incluir os valores reais no Git):

   ```dotenv
   MONGO_URI=mongodb+srv://USUARIO:SENHA@HOST/BANCO
   JWT_SECRET=SEGREDO_ALEATORIO_GERADO_LOCALMENTE
   PORT=3000
   ```

2. Na raiz do repositório, execute:

   ```bash
   docker compose up --build -d
   docker compose ps
   curl http://localhost:3000/health
   ```

3. A rota `/health` responde com `status: ok` quando o servidor HTTP está ativo. **Ela não verifica o banco**.

### Verificar de verdade a conexão ao MongoDB Atlas

Após adicionar `backend/scripts/check-mongodb.js`, reconstrua o contêiner para incluir o script:

```bash
docker compose up --build -d
docker compose exec -T api node scripts/check-mongodb.js
```

Mensagem esperada: `Conexao MongoDB confirmada: ping OK`. Essa verificação abre uma conexão com o MongoDB configurado em `MONGO_URI` e executa `admin().ping()`; não grava documentos. A execução depende de credenciais válidas, do cluster ativo e da política de acesso de rede do MongoDB Atlas.

## Kubernetes no Codespaces

Requer Docker, kind e kubectl instalados. O cluster de demonstração é local e independente do Render.

```bash
kind create cluster --name deskland
kubectl config use-context kind-deskland
kubectl create secret generic deskland-api-secret --from-env-file=backend/.env
docker build -t deskland-api:k8s-v1 -f docker/Dockerfile .
kind load docker-image deskland-api:k8s-v1 --name deskland
kubectl apply -f kubernetes/deployment.yaml
kubectl apply -f kubernetes/service.yaml
kubectl rollout status deployment/deskland-api --timeout=180s
kubectl get pods
kubectl get services
```

**Nunca versione Secrets com credenciais reais.** Para testar a API via Service:

```bash
kubectl port-forward service/deskland-api 8080:80
```

Em outro terminal: `curl http://localhost:8080/health`.

O cluster kind temporário do quinto workflow usa um MongoDB próprio, sem credenciais do Atlas; isso torna os testes de CI independentes do banco externo. Secrets do Kubernetes não equivalem, por si só, a um cofre de produção.

## Render.com

O Web Service no Render utiliza o repositório GitHub, `docker/Dockerfile` e contexto Docker `.`. Configure as variáveis `MONGO_URI`, `JWT_SECRET` e `PORT` em **Environment**, nunca no repositório. A URL do Render deve responder a `GET /health`. Instâncias gratuitas podem hibernar por inatividade, aumentando a latência da primeira requisição.

A publicação da imagem no GHCR pelo `docker-ci.yml` **não** faz automaticamente o deploy no Render ou no cluster kind do Codespaces.

## Segurança e limitações

- `.env` e `local.properties` são arquivos locais; não publicar senhas, JWT_SECRET nem strings privadas.
- O arquivo `Dockerfile.dockerignore` deve excluir `backend/.env` do contexto Docker, além do `.gitignore`.
- Se uma credencial tiver sido exposta em uma captura ou commit, troque-a no serviço correspondente.
- As rotas originais do backend tratam usuários/contatos/uploads. Ainda é necessário integrar ou criar endpoints de produtos e pedidos se esse for um requisito do aplicativo.
- Confirme a quantidade real de testes executados nos logs de cada workflow; um job verde significa que as etapas configuradas passaram.

## Evidências para a apresentação

Registrar capturas dos cinco workflows aprovados, do artifact APK, do endpoint público `/health`, de `docker compose ps`, de `kubectl get pods` mostrando duas réplicas, do Service Kubernetes, e do resultado `ping OK` no MongoDB Atlas. Não incluir credenciais nas capturas.
