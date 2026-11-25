FROM node:22-alpine
RUN apk add --no-cache vips-dev && corepack enable pnpm && corepack prepare pnpm@10.34.9 --activate
WORKDIR /app
COPY package.json pnpm-lock.yaml ./
RUN pnpm install --prod
COPY . .
RUN pnpm build
USER node
EXPOSE 1337
CMD ["pnpm", "start"]
