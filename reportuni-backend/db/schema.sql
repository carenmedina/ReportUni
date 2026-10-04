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

-- Semana 4: creacion de reportes (formulario, fotografia y ubicacion)
-- Estas tablas tambien las crea Hibernate automaticamente
-- (spring.jpa.hibernate.ddl-auto=update); el script sirve para tener
-- el esquema documentado y poder crearlo a mano si se prefiere.
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS reportes (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id            BIGINT        NOT NULL,
    tipo_dano             VARCHAR(30)   NOT NULL,
    prioridad_estimada    VARCHAR(10)   NOT NULL,
    bloque                VARCHAR(30)   NOT NULL,
    espacio_especifico    VARCHAR(120)  NOT NULL,
    latitud               DOUBLE        NULL,
    longitud              DOUBLE        NULL,
    descripcion           VARCHAR(1000) NOT NULL,
    notificar_por_correo  BOOLEAN       NOT NULL DEFAULT TRUE,
    estado                VARCHAR(20)   NOT NULL DEFAULT 'PENDIENTE',
    fecha_creacion        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reportes_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS reporte_fotos (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    reporte_id       BIGINT       NOT NULL,
    nombre_original  VARCHAR(255) NOT NULL,
    nombre_archivo   VARCHAR(80)  NOT NULL,
    tipo_contenido   VARCHAR(30)  NOT NULL,
    tamano_bytes     BIGINT       NOT NULL,
    orden            INT          NOT NULL,
    CONSTRAINT fk_fotos_reporte FOREIGN KEY (reporte_id) REFERENCES reportes (id) ON DELETE CASCADE
) ENGINE = InnoDB;