
FROM node:22-alpine

WORKDIR /app

# Copiar dependencias
COPY backend/package*.json ./

# Instalar dependencias
RUN npm ci --omit=dev

# Copiar codigo da API
COPY backend/ ./

# Garantir acesso aos arquivos pelo usuario node
RUN chown -R node:node /app \
    && chmod -R u+rwX,go+rX /app

# Configuracoes
ENV NODE_ENV=production
ENV PORT=3000

EXPOSE 3000

# Executar sem privilegios administrativos
USER node

CMD ["node", "server.js"]