-- New columns in ApprovalData are added by the JPA provider if there are sufficient privileges
-- if not added automatically the following SQL statements can be run to add the new columns 
-- If you are using an older version of Derby, such as 10.2, you need to drop and re-create this table. See doc/howto/create-tables-ejbca-derby.sql for drop/create statements.
-- ALTER TABLE ApprovalData ADD subjectDn VARCHAR(256);
-- ALTER TABLE ApprovalData ADD email VARCHAR(256);
