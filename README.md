# poject1_uploader
## Вариант 1: запуск всего стека через Docker Compose (рекомендуется)

**Требования:** установленный Docker и Docker Compose.

```bash
docker compose -f infra/compose.yml up --build
```

После запуска сервисы будут доступны по портам:

- **FileUploader API:** http://localhost:8080
- **Worker-service:** http://localhost:8081
- **PostgreSQL:** localhost:55433
- **Kafka (EXTERNAL):** localhost:19092
- **MinIO API:** http://localhost:9000
- **MinIO Console:** http://localhost:9001 (логин/пароль: `minio` / `minio12345`)

Остановить и удалить контейнеры:

```bash
docker compose -f infra/compose.yml down

## Как правильно отправлять запросы в сервис

Сервис принимает загрузку файла только через `multipart/form-data` и требует два заголовка:

- `X-Client-Id` — идентификатор клиента.
- `Idempotency-Key` — ключ идемпотентности (повтор с тем же ключом вернет уже созданную запись).

### Пример с `curl`

```bash
curl -X POST \
  -H "X-Client-Id: demo-client" \
  -H "Idempotency-Key: upload-0001" \
  -F "file=@/path/to/file.txt" \
  http://localhost:8080/uploads
```

### Пример ответа

```json
{
  "uploadId": "8b6a8d10-1e55-4b5e-9c9a-6f6a9b5d4f8a",
  "status": "RECEIVED"
}
```

Если воркер успел обработать файл, статус может быть `STORED` или `FAILED`. В таком случае API вернет `200 OK`, иначе — `202 Accepted`.
