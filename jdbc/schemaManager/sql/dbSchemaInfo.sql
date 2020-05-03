CREATE TABLE db_schema_type (
   id bigserial PRIMARY KEY,
   type_name text UNIQUE NOT NULL);

CREATE TABLE db_schema_version (
   id bigserial PRIMARY KEY,
   schema_type bigint REFERENCES db_schema_type(id),
   schema_sql text NOT NULL,
   alter_sql text,
   date_applied timestamp DEFAULT Now(),
   version_info text,
   build_layer_name text);

CREATE TABLE db_schema_current_version (
   schema_type bigint UNIQUE REFERENCES db_schema_type(id),
   schema_version bigint UNIQUE REFERENCES db_schema_version(id),
   last_updated timestamp DEFAULT Now());

/* 
SELECT tst.type_name, tsv.schema_sql, tsv.alter_sql, tsv.date_applied, tsv.version_info FROM db_schema_type as tst, db_schema_current_version as tscv, db_schema_version as tsv WHERE tst.id = tscv.schema_type AND tscv.schema_version = tsv.id

*/

