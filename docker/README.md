# GDCLab Docker

Docker support is provided by:

- `backend/Dockerfile`
- `frontend/Dockerfile`
- `docker-compose.yml`
- `docker/nginx-gdclab.conf`

GDCLab reuses the existing PostgreSQL server and isolates data in the `gdclab` schema.
