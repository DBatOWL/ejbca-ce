-- New columns in ApprovalData are added by the JPA provider if there are sufficient privileges
-- if not added automatically the following SQL statements can be run to add the new columns 
-- ALTER TABLE ApprovalData ADD subjectDn VARCHAR(250) BINARY;
-- ALTER TABLE ApprovalData ADD email VARCHAR(250) BINARY;
-- ALTER TABLE CertificateData ADD accountBindingId VARCHAR(250) BINARY;
-- ALTER TABLE NoConflictCertificateData ADD accountBindingId VARCHAR(250) BINARY;