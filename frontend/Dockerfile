FROM node:24-alpine AS build
WORKDIR /workspace
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build -- --configuration production --base-href /gdclab/

FROM nginx:1.27-alpine
COPY --from=build /workspace/dist/gdclab-ui/browser /usr/share/nginx/html/gdclab
