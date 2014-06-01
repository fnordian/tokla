
# --- !Ups

UPDATE token SET CLAIMEDBY=NULL WHERE CLAIMEDBY='';

UPDATE tokenapplicant SET applicantname=NULL WHERE applicantname not in (select distinct id from user);
ALTER TABLE TOKENAPPLICANT ADD  FOREIGN KEY ( APPLICANTNAME ) REFERENCES USER ( ID );


UPDATE token SET claimedby=NULL WHERE claimedby not in (select distinct id from user);
ALTER TABLE TOKEN ADD  FOREIGN KEY ( CLAIMEDBY ) REFERENCES USER ( ID );


# --- !Downs

