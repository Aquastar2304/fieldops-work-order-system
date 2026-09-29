FROM golang:1.25-alpine AS build
WORKDIR /build

COPY go.mod go.sum ./
RUN go mod download

COPY . .
# CGO off keeps the binary static so it runs on a scratch-like base.
RUN CGO_ENABLED=0 GOOS=linux go build -ldflags="-s -w" -o report-service .

FROM alpine:3.20
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
COPY --from=build /build/report-service /app/report-service
USER app

EXPOSE 8081
ENTRYPOINT ["/app/report-service"]
