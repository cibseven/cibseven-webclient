-- The two tables the diagram history is written to, as a real installation has them: copied from
-- the shipped create script (activiti.h2.create.modeler.sql in the cibseven repository).
--
-- Pinned here on purpose. Letting Hibernate generate the schema from the entities would make a
-- wrong column name pass the test, since it would create whatever the mapping declares.

CREATE TABLE IF NOT EXISTS MOD_REVINFO (
    REV BIGINT AUTO_INCREMENT PRIMARY KEY,
    REVTSTMP BIGINT
);

CREATE TABLE IF NOT EXISTS MOD_PROCESSES_DIAGRAMS_AUD (
    ID VARCHAR(36) NOT NULL,
    NAME VARCHAR(255),
    PROCESSKEY VARCHAR(100),
    DESCRIPTION VARCHAR(150),
    CREATED TIMESTAMP,
    UPDATED TIMESTAMP,
    ACTIVE BOOLEAN DEFAULT TRUE,
    TYPE VARCHAR(50),
    VERSION INTEGER DEFAULT 1,
    DIAGRAM_MOD BOOLEAN DEFAULT false,
    DIAGRAM BLOB,
    UPDATED_BY VARCHAR(100),
    REV BIGINT NOT NULL,
    REVTYPE TINYINT,
    CONSTRAINT MOD_PK_RESOURCES_AUD PRIMARY KEY (ID, REV),
    CONSTRAINT MOD_FK_RESOURCES_AUD_REV FOREIGN KEY (REV) REFERENCES MOD_REVINFO(REV)
);
