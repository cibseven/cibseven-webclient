-- Oracle baseline schema matching JPA entities in cibseven-modeler-core

CREATE TABLE element_templates (
    id VARCHAR2(36 CHAR) PRIMARY KEY,
    active NUMBER(1) DEFAULT 1 NOT NULL,
    version NUMBER(11) DEFAULT 1,
    template_id VARCHAR2(100 CHAR) NOT NULL UNIQUE,
    name VARCHAR2(200 CHAR) NOT NULL,
    description CLOB,
    origin VARCHAR2(50 CHAR) NOT NULL,
    content CLOB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by VARCHAR2(100 CHAR),
    updated_by VARCHAR2(100 CHAR)
);

CREATE TABLE processes_diagrams (
    id VARCHAR2(36) PRIMARY KEY,
    name VARCHAR2(255) NOT NULL,
    processkey VARCHAR2(100) NOT NULL UNIQUE,
    description VARCHAR2(150),
    created TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    active NUMBER(1) DEFAULT 1 NOT NULL,
    type VARCHAR2(50) NOT NULL DEFAULT 'bpmn-c7',
    version NUMBER(10,0),
    diagram BLOB
);

CREATE TABLE revinfo (
    rev NUMBER(10,0) NOT NULL PRIMARY KEY,
    revtstmp NUMBER(20,0)
);

CREATE SEQUENCE revinfo_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE processes_diagrams_aud (
    id VARCHAR2(36) NOT NULL,
    name VARCHAR2(255),
    processkey VARCHAR2(100),
    description VARCHAR2(150),
    created TIMESTAMP,
    updated TIMESTAMP,
    active NUMBER(1) DEFAULT 1,
    type VARCHAR2(50) DEFAULT 'bpmn-c7',
    version NUMBER(11),
    diagram_mod NUMBER(1) DEFAULT 0,
    diagram BLOB,
    rev NUMBER(10,0) NOT NULL,
    revtype NUMBER(6,0),
    CONSTRAINT pk_resources_aud PRIMARY KEY (id, rev),
    CONSTRAINT fk_resources_aud_rev FOREIGN KEY (rev) REFERENCES revinfo(rev)
);

CREATE SEQUENCE hibernate_sequence
    START WITH 1
    INCREMENT BY 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    CACHE 1;

CREATE TABLE user_sessions (
    id VARCHAR2(36) PRIMARY KEY,
    user_id VARCHAR2(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP
);

CREATE TABLE diagram_usage (
    id VARCHAR2(36) PRIMARY KEY,
    user_id VARCHAR2(255) NOT NULL,
    diagram_id VARCHAR2(36) NOT NULL,
    session_id VARCHAR2(36) NOT NULL,
    opened_at TIMESTAMP NOT NULL,
    closed_at TIMESTAMP,
    CONSTRAINT fk_diagram_usage_diagram FOREIGN KEY (diagram_id) REFERENCES processes_diagrams(id) ON DELETE CASCADE,
    CONSTRAINT fk_diagram_usage_session FOREIGN KEY (session_id) REFERENCES user_sessions(id) ON DELETE CASCADE
);

CREATE TABLE forms (
    id VARCHAR2(36 CHAR) PRIMARY KEY,
    description VARCHAR2(150 CHAR),
    created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP DEFAULT TO_TIMESTAMP('1970-01-01 00:00:00', 'YYYY-MM-DD HH24:MI:SS'),
    active NUMBER(1) DEFAULT 1 NOT NULL,
    form_schema BLOB NOT NULL,
    formid VARCHAR2(100 CHAR) NOT NULL UNIQUE,
    version NUMBER(11) DEFAULT 1
);

CREATE TABLE form_usage (
    id VARCHAR2(36) PRIMARY KEY,
    user_id VARCHAR2(255) NOT NULL,
    form_id VARCHAR2(36) NOT NULL,
    session_id VARCHAR2(36) NOT NULL,
    opened_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    closed_at TIMESTAMP,
    CONSTRAINT fk_form_usage_form FOREIGN KEY (form_id) REFERENCES forms(id) ON DELETE CASCADE,
    CONSTRAINT fk_form_usage_session FOREIGN KEY (session_id) REFERENCES user_sessions(id) ON DELETE CASCADE
);
