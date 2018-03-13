CREATE SEQUENCE seq_pagamenti_portale MINVALUE 1 MAXVALUE 9223372036854775807 START WITH 1 INCREMENT BY 1 CACHE 2 NOCYCLE;

CREATE TABLE pagamenti_portale
(
       cod_portale VARCHAR2(35 CHAR) NOT NULL,
       cod_canale VARCHAR2(35 CHAR),
       nome VARCHAR2(255 CHAR) NOT NULL,
       importo BINARY_DOUBLE NOT NULL,
       versante_identificativo VARCHAR2(35 CHAR),
       id_sessione VARCHAR2(35 CHAR) NOT NULL,
       id_sessione_portale VARCHAR2(35 CHAR),
       id_sessione_psp VARCHAR2(35 CHAR),
       stato VARCHAR2(35 CHAR) NOT NULL,
       codice_stato VARCHAR2(35 CHAR) NOT NULL,
       descrizione_stato VARCHAR2(1024 CHAR),
       psp_redirect_url VARCHAR2(1024 CHAR),
       psp_esito VARCHAR2(255 CHAR),
       json_request CLOB,
       wisp_id_dominio VARCHAR2(255 CHAR),
       wisp_key_pa VARCHAR2(255 CHAR),
       wisp_key_wisp VARCHAR2(255 CHAR),
       wisp_html CLOB,
       data_richiesta TIMESTAMP,
       url_ritorno VARCHAR2(1024 CHAR) NOT NULL,
       cod_psp VARCHAR2(35 CHAR),
       tipo_versamento VARCHAR2(4 CHAR),
       -- fk/pk columns
       id NUMBER NOT NULL,
       -- unique constraints
       CONSTRAINT unique_pagamenti_portale_1 UNIQUE (id_sessione),
       -- fk/pk keys constraints
       CONSTRAINT pk_pagamenti_portale PRIMARY KEY (id)
);

CREATE TRIGGER trg_pagamenti_portale
BEFORE
insert on pagamenti_portale
for each row
begin
   IF (:new.id IS NULL) THEN
      SELECT seq_pagamenti_portale.nextval INTO :new.id
                FROM DUAL;
   END IF;
end;
/



CREATE SEQUENCE seq_pag_port_versamenti MINVALUE 1 MAXVALUE 9223372036854775807 START WITH 1 INCREMENT BY 1 CACHE 2 NOCYCLE;

CREATE TABLE pag_port_versamenti
(
       -- fk/pk columns
       id NUMBER NOT NULL,
       id_pagamento_portale NUMBER NOT NULL,
       id_versamento NUMBER NOT NULL,
       -- fk/pk keys constraints
       CONSTRAINT fk_ppv_id_pagamento_portale FOREIGN KEY (id_pagamento_portale) REFERENCES pagamenti_portale(id),
       CONSTRAINT fk_ppv_id_versamento FOREIGN KEY (id_versamento) REFERENCES versamenti(id),
       CONSTRAINT pk_pag_port_versamenti PRIMARY KEY (id)
);

CREATE TRIGGER trg_pag_port_versamenti
BEFORE
insert on pag_port_versamenti
for each row
begin
   IF (:new.id IS NULL) THEN
      SELECT seq_pag_port_versamenti.nextval INTO :new.id
                FROM DUAL;
   END IF;
end;
/




ALTER TABLE rpt ADD id_pagamento_portale NUMBER;
ALTER TABLE rpt ADD CONSTRAINT fk_rpt_id_pagamento_portale FOREIGN KEY (id_pagamento_portale) REFERENCES pagamenti_portale(id);

ALTER TABLE versamenti ADD data_validita TIMESTAMP(3);
ALTER TABLE versamenti ADD nome VARCHAR(35);
--TODO not null
ALTER TABLE versamenti ADD tassonomia_avviso VARCHAR(35);
--TODO not null
ALTER TABLE versamenti ADD tassonomia VARCHAR(35);
ALTER TABLE versamenti ADD id_dominio BIGINT;
update versamenti set id_dominio = (select id_dominio from uo where id = versamenti.id_uo);
ALTER TABLE versamenti MODIFY (id_dominio NOT NULL);
ALTER TABLE versamenti MODIFY (id_uo NULL);
ALTER TABLE versamenti ADD CONSTRAINT fk_vrs_id_dominio FOREIGN KEY (id_dominio) REFERENCES domini(id);
ALTER TABLE versamenti ADD debitore_tipo VARCHAR2(1 CHAR);

ALTER TABLE acl DROP CONSTRAINT fk_acl_id_portale;
ALTER TABLE acl DROP COLUMN id_portale;

ALTER TABLE rpt DROP CONSTRAINT fk_rpt_id_portale;
ALTER TABLE rpt DROP COLUMN id_portale;

ALTER TABLE rpt ADD id_applicazione BIGINT;
ALTER TABLE rpt ADD CONSTRAINT fk_rpt_id_applicazione FOREIGN KEY (id_applicazione) REFERENCES applicazioni(id);

DROP TABLE portali;
DROP SEQUENCE portali_seq;

ALTER TABLE applicazioni ADD reg_exp VARCHAR2(1024 CHAR);

ALTER TABLE domini DROP COLUMN xml_conti_accredito;
ALTER TABLE domini DROP COLUMN xml_tabella_controparti;


DROP TABLE acl;
CREATE TABLE acl
(
	ruolo VARCHAR2(255 CHAR),
	principal VARCHAR2(255 CHAR),
	servizio VARCHAR2(255 CHAR) NOT NULL,
	diritti VARCHAR2(255 CHAR) NOT NULL,
	-- fk/pk columns
	id NUMBER NOT NULL,
	-- fk/pk keys constraints
	CONSTRAINT pk_acl PRIMARY KEY (id)
);

DROP TABLE ruoli;
DROP SEQUENCE seq_ruoli;
DROP TRIGGER trg_ruoli;

CREATE SEQUENCE seq_utenze MINVALUE 1 MAXVALUE 9223372036854775807 START WITH 1 INCREMENT BY 1 CACHE 2 NOCYCLE;

CREATE TABLE utenze
(
	principal VARCHAR2(255 CHAR) NOT NULL,
	abilitato NUMBER NOT NULL,
	-- fk/pk columns
	id NUMBER NOT NULL,
	-- unique constraints
	CONSTRAINT unique_utenze_1 UNIQUE (principal),
	-- fk/pk keys constraints
	CONSTRAINT pk_utenze PRIMARY KEY (id)
);

ALTER TABLE utenze MODIFY abilitato DEFAULT 1;

CREATE TRIGGER trg_utenze
BEFORE
insert on utenze
for each row
begin
   IF (:new.id IS NULL) THEN
      SELECT seq_utenze.nextval INTO :new.id
                FROM DUAL;
   END IF;
end;
/



CREATE SEQUENCE seq_utenze_domini MINVALUE 1 MAXVALUE 9223372036854775807 START WITH 1 INCREMENT BY 1 CACHE 2 NOCYCLE;

CREATE TABLE utenze_domini
(
	-- fk/pk columns
	id NUMBER NOT NULL,
	id_utenza NUMBER NOT NULL,
	id_dominio NUMBER NOT NULL,
	-- fk/pk keys constraints
	CONSTRAINT fk_nzd_id_utenza FOREIGN KEY (id_utenza) REFERENCES utenze(id),
	CONSTRAINT fk_nzd_id_dominio FOREIGN KEY (id_dominio) REFERENCES domini(id),
	CONSTRAINT pk_utenze_domini PRIMARY KEY (id)
);

CREATE TRIGGER trg_utenze_domini
BEFORE
insert on utenze_domini
for each row
begin
   IF (:new.id IS NULL) THEN
      SELECT seq_utenze_domini.nextval INTO :new.id
                FROM DUAL;
   END IF;
end;
/



CREATE SEQUENCE seq_utenze_tributi MINVALUE 1 MAXVALUE 9223372036854775807 START WITH 1 INCREMENT BY 1 CACHE 2 NOCYCLE;

CREATE TABLE utenze_tributi
(
	-- fk/pk columns
	id NUMBER NOT NULL,
	id_utenza NUMBER NOT NULL,
	id_tributo NUMBER NOT NULL,
	-- fk/pk keys constraints
	CONSTRAINT fk_nzt_id_utenza FOREIGN KEY (id_utenza) REFERENCES utenze(id),
	CONSTRAINT fk_nzt_id_tributo FOREIGN KEY (id_tributo) REFERENCES tributi(id),
	CONSTRAINT pk_utenze_tributi PRIMARY KEY (id)
);

CREATE TRIGGER trg_utenze_tributi
BEFORE
insert on utenze_tributi
for each row
begin
   IF (:new.id IS NULL) THEN
      SELECT seq_utenze_tributi.nextval INTO :new.id
                FROM DUAL;
   END IF;
end;
/



insert into utenze (principal) select distinct principal from ((select principal from operatori) union (select principal from applicazioni) )as s;


ALTER TABLE applicazioni ADD id_utenza NUMBER;
UPDATE applicazioni set id_utenza = (select id from utenze where principal = applicazioni.principal);
ALTER TABLE applicazioni MODIFY (id_utenza NOT NULL);
ALTER TABLE applicazioni DROP COLUMN principal;
ALTER TABLE applicazioni DROP COLUMN abilitato;

ALTER TABLE operatori ADD id_utenza NUMBER;
UPDATE operatori set id_utenza = (select id from utenze where principal = operatori.principal);
ALTER TABLE operatori MODIFY (id_utenza NOT NULL);
ALTER TABLE operatori DROP COLUMN principal;
ALTER TABLE operatori DROP COLUMN profilo;
ALTER TABLE operatori DROP COLUMN abilitato;

ALTER TABLE applicazioni ADD auto_iuv BOOLEAN;
UPDATE applicazioni SET auto_iuv = true;
ALTER TABLE applicazioni MODIFY (auto_iuv NOT NULL);

ALTER TABLE domini DROP COLUMN custom_iuv;
ALTER TABLE domini DROP COLUMN iuv_prefix_strict;
ALTER TABLE domini DROP COLUMN riuso_iuv;

ALTER TABLE iban_accredito DROP COLUMN id_seller_bank;
ALTER TABLE iban_accredito DROP COLUMN id_negozio;
ALTER TABLE applicazioni DROP COLUMN versione;

ALTER TABLE versamenti add dati_allegati CLOB;
ALTER TABLE singoli_versamenti add dati_allegati CLOB;
ALTER TABLE singoli_versamenti add descrizione VARCHAR2(256 CHAR);
ALTER TABLE singoli_versamenti drop column note;
