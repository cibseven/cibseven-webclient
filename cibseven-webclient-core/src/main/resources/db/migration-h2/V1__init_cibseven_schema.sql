-- H2 baseline schema matching JPA entities in cibseven-modeler-core

CREATE TABLE IF NOT EXISTS element_templates (
    id VARCHAR(36) PRIMARY KEY,
    active BOOLEAN DEFAULT TRUE NOT NULL,
    version INTEGER DEFAULT 1,
    template_id VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    description CLOB,
    origin VARCHAR(50) NOT NULL,
    content CLOB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS processes_diagrams (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    processkey VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(150),
    created TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    active BOOLEAN DEFAULT TRUE NOT NULL,
    type VARCHAR(50) NOT NULL DEFAULT 'bpmn-c7',
    version INTEGER,
    diagram BLOB
);

CREATE TABLE IF NOT EXISTS revinfo (
    REV INT AUTO_INCREMENT PRIMARY KEY,
    REVTSTMP BIGINT
);

CREATE TABLE IF NOT EXISTS processes_diagrams_aud (
    id VARCHAR(36) NOT NULL,
    name VARCHAR(255),
    processkey VARCHAR(100) NOT NULL,
    description VARCHAR(150),
    created TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    active BOOLEAN DEFAULT TRUE NOT NULL,
    type VARCHAR(50) NOT NULL DEFAULT 'bpmn-c7',
    version INTEGER,
    diagram_mod BOOLEAN DEFAULT false NOT NULL,
    diagram BLOB,
    rev INT NOT NULL,
    revtype TINYINT,
    CONSTRAINT pk_resources_aud PRIMARY KEY (id, rev),
    CONSTRAINT fk_resources_aud_rev FOREIGN KEY (rev) REFERENCES revinfo(rev)
);

-- Envers expected revision sequence for H2
CREATE SEQUENCE IF NOT EXISTS revinfo_seq START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE IF NOT EXISTS hibernate_sequence START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS user_sessions (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    expires_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS diagram_usage (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    diagram_id VARCHAR(36) NOT NULL,
    session_id VARCHAR(36) NOT NULL,
    opened_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    closed_at TIMESTAMP,
    CONSTRAINT fk_diagram_usage_diagram FOREIGN KEY (diagram_id) REFERENCES processes_diagrams(id) ON DELETE CASCADE,
    CONSTRAINT fk_diagram_usage_session FOREIGN KEY (session_id) REFERENCES user_sessions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS forms (
    id VARCHAR(36) PRIMARY KEY,
    description VARCHAR(150),
    created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP DEFAULT '1970-01-01 00:00:00',
    active BOOLEAN DEFAULT TRUE NOT NULL,
    form_schema BLOB NOT NULL,
    formid VARCHAR(100) NOT NULL UNIQUE,
    version INTEGER DEFAULT 1
);

CREATE TABLE IF NOT EXISTS form_usage (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    form_id VARCHAR(36) NOT NULL,
    session_id VARCHAR(36) NOT NULL,
    opened_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    closed_at TIMESTAMP,
    CONSTRAINT fk_form_usage_form FOREIGN KEY (form_id) REFERENCES forms(id) ON DELETE CASCADE,
    CONSTRAINT fk_form_usage_session FOREIGN KEY (session_id) REFERENCES user_sessions(id) ON DELETE CASCADE
);
