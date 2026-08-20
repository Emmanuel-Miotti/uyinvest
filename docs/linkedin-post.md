# UYInvest — material para LinkedIn

## Nombre del proyecto

**UYInvest — Investment Portfolio Management Platform**

## Tecnologías (para la sección "Technologies" del post/proyecto)

Java 21 · Spring Boot 3.5 · Spring Security · JWT · JPA/Hibernate · PostgreSQL · Flyway · React 19 · TypeScript · Vite · Docker · Docker Compose · GitHub Actions · REST API · JUnit 5 · Mockito · Testcontainers · Swagger/OpenAPI

## Post sugerido (versión larga)

---

🚀 Terminé UYInvest, una plataforma de gestión de inversiones full-stack que armé de punta a punta para poner en práctica arquitectura backend, seguridad y testing de nivel profesional — no un CRUD más.

¿Qué hace?
Trackea una cartera de inversiones real: cuánto invertiste, cuánto vale hoy, ganancia/pérdida, rendimiento %, distribución por tipo de activo, evolución del capital invertido, dividendos y objetivos financieros.

Algunas decisiones técnicas de las que estoy más orgulloso:

🔹 Motor de cálculo de cartera con costo promedio ponderado, probado exhaustivamente contra números verificados a mano (no solo "pasa el test")

🔹 Autorización a nivel de service en cada endpoint: acceder al recurso de otro usuario devuelve 404, no 403 — para no confirmar ni siquiera que el recurso existe

🔹 Detecté y corregí un problema real de N+1 queries, y lo probé contando las statements SQL reales de Hibernate (no "debería andar mejor")

🔹 JWT + Spring Security, BCrypt, CORS restringido, manejo centralizado de errores con formato consistente en toda la API

🔹 114 tests de backend con JUnit 5 + Mockito + Testcontainers contra PostgreSQL real (no H2)

🔹 Stack completo dockerizado (Postgres + backend + frontend) levantando con un solo comando, y pipeline de CI en GitHub Actions corriendo build + tests + package en cada push

Stack: Java 21 · Spring Boot · Spring Security · JWT · JPA/Hibernate · PostgreSQL · React · TypeScript · Docker · REST API · JUnit · Swagger

Repo (código completo, abierto): https://github.com/Emmanuel-Miotti/uyinvest

#Java #SpringBoot #React #TypeScript #PostgreSQL #Docker #backend #fullstack

---

## Versión corta (para el "About" de un post con carrusel/imágenes, o como resumen del repo)

Plataforma full-stack de gestión de inversiones (Java 21 + Spring Boot + React + TypeScript + PostgreSQL). Motor de cálculo de cartera con costo promedio ponderado, JWT + autorización por ownership, 114 tests con Testcontainers, Docker Compose y CI en GitHub Actions. Código completo en GitHub.

## Qué demuestra este proyecto

- Desarrollo backend con Java y Spring Boot
- Diseño de APIs REST
- Arquitectura por capas (controller/service/repository/entity/dto)
- Seguridad (Spring Security, JWT, BCrypt, CORS, autorización)
- JPA/Hibernate con atención real a N+1 y fetch strategy
- Diseño de base de datos (PostgreSQL, Flyway, constraints, índices)
- Lógica de negocio no trivial (motor de cálculo financiero)
- Testing (JUnit, Mockito, Testcontainers)
- Docker y containerización multi-servicio
- CI/CD (GitHub Actions)
- Arquitectura preparada para integración con APIs externas (proveedor de precios de mercado intercambiable)

## Notas para vos antes de publicar

- Reemplazá o completá la sección de **Screenshots** del README con capturas actualizadas si seguís iterando el diseño.
- Si apuntás a reclutadores internacionales, considerá traducir el post al inglés (el código y el README ya están en inglés).
- El repo es público — repasalo una vez más vos mismo antes de compartirlo, por las dudas.
