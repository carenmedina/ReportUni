CREATE DATABASE IF NOT EXISTS reportuni_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE reportuni_db;

CREATE TABLE IF NOT EXISTS usuarios (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre_completo      VARCHAR(150)        NOT NULL,
    correo_institucional VARCHAR(150)        NOT NULL UNIQUE,
    password_hash        VARCHAR(255)        NOT NULL,
    rol                   ENUM('ESTUDIANTE', 'ADMINISTRADOR') NOT NULL DEFAULT 'ESTUDIANTE',
    activo                BOOLEAN             NOT NULL DEFAULT TRUE,
    fecha_creacion        DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB;

-- Nota: no se insertan usuarios de prueba aqui a proposito, porque el
-- password_hash debe generarse con BCrypt (no un texto plano). El
-- backend crea automaticamente 3 usuarios de prueba al arrancar por
-- primera vez (ver DataInitializer.java): dos estudiantes y un
-- administrador, todos con la contraseña "ReportUni2026".
