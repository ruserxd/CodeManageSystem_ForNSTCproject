FROM node:24.22.2-alpine AS builder
WORKDIR /app
# 先複製「依賴描述檔」，讓 yarn install 這層可以被快取
COPY package.json yarn.lock .yarnrc.yml ./
COPY .yarn/releases ./.yarn/releases
RUN yarn install --immutable
# 複製程式碼
COPY . .
# 接受建置參數
ARG VITE_API_URL
RUN yarn build

FROM nginx:1.31.8-alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80