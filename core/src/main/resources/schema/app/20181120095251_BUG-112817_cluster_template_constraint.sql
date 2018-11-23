-- // BUG-112817 cluster template constraint
-- Migration SQL that makes the change goes here.
ALTER TABLE clustertemplate DROP CONSTRAINT IF EXISTS fk_stack_template_stack;

ALTER TABLE clustertemplate
ADD COLUMN IF NOT EXISTS stacktemplate_id bigint,
ADD CONSTRAINT fk_stack_template_stack FOREIGN KEY (stacktemplate_id) REFERENCES stack (id);
ALTER TABLE clustertemplate DROP COLUMN template;
ALTER TABLE clustertemplate ADD COLUMN IF NOT EXISTS datalakeRequired character varying(255);
ALTER TABLE clustertemplate ALTER COLUMN cloudplatform DROP NOT NULL;

ALTER TABLE stack ALTER COLUMN consulservers DROP NOT NULL;
ALTER TABLE stack ALTER COLUMN name DROP NOT NULL;
ALTER TABLE stack ALTER COLUMN onfailureactionaction DROP NOT NULL;
ALTER TABLE stack ALTER COLUMN publicinaccount DROP NOT NULL;
ALTER TABLE stack ALTER COLUMN region DROP NOT NULL;

ALTER TABLE cluster ALTER COLUMN name DROP NOT NULL;
ALTER TABLE cluster ALTER COLUMN secure DROP NOT NULL;
ALTER TABLE cluster ALTER COLUMN status DROP NOT NULL;
ALTER TABLE cluster ALTER COLUMN username DROP NOT NULL;
ALTER TABLE cluster ALTER COLUMN password DROP NOT NULL;
ALTER TABLE cluster ALTER COLUMN configstrategy DROP NOT NULL;
ALTER TABLE cluster ALTER COLUMN topologyvalidation DROP NOT NULL;
ALTER TABLE cluster ADD COLUMN IF NOT EXISTS type character varying(255);

ALTER TABLE network ALTER COLUMN cloudplatform DROP NOT NULL;
ALTER TABLE network ALTER COLUMN status DROP NOT NULL;
ALTER TABLE network ALTER COLUMN name DROP NOT NULL;
ALTER TABLE network ALTER COLUMN name DROP NOT NULL;
ALTER TABLE network ADD COLUMN IF NOT EXISTS type character varying(255);

ALTER TABLE template DROP CONSTRAINT IF EXISTS uk_template_account_name;
ALTER TABLE template ALTER COLUMN name DROP NOT NULL;
ALTER TABLE template ALTER COLUMN instancetype DROP NOT NULL;
ALTER TABLE template ALTER COLUMN owner DROP NOT NULL;
ALTER TABLE template ALTER COLUMN account DROP NOT NULL;
ALTER TABLE template ALTER COLUMN volumecount DROP NOT NULL;
ALTER TABLE template ALTER COLUMN deleted DROP NOT NULL;
ALTER TABLE template ALTER COLUMN status DROP NOT NULL;
ALTER TABLE template ALTER COLUMN cloudplatform DROP NOT NULL;

ALTER TABLE instancemetadata ALTER COLUMN instancestatus DROP NOT NULL;
ALTER TABLE instancemetadata ALTER COLUMN instancemetadatatype DROP NOT NULL;
ALTER TABLE instancemetadata ALTER COLUMN privateid DROP NOT NULL;

ALTER TABLE securityrule ALTER COLUMN modifiable DROP NOT NULL;

ALTER TABLE instancegroup ALTER COLUMN groupname DROP NOT NULL;
ALTER TABLE instancegroup ALTER COLUMN instancegrouptype DROP NOT NULL;

ALTER TABLE securitygroup ALTER COLUMN name DROP NOT NULL;
ALTER TABLE securitygroup ALTER COLUMN status DROP NOT NULL;

ALTER TABLE gateway ALTER COLUMN gatewayType DROP NOT NULL;
ALTER TABLE gateway ALTER COLUMN ssoType DROP NOT NULL;
ALTER TABLE gateway ALTER COLUMN topologyName DROP NOT NULL;
ALTER TABLE gateway ALTER COLUMN cluster_id DROP NOT NULL;

-- //@UNDO
-- SQL to undo the change goes here.
