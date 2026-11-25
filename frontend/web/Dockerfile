FROM node:22-alpine
RUN corepack enable && corepack prepare pnpm@10.34.9 --activate
WORKDIR /app
COPY . .
RUN pnpm install
EXPOSE 5173
CMD ["pnpm", "dev"]
