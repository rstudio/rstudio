BEGIN TRANSACTION;
CREATE TABLE IF NOT EXISTS "revoked_cookie" (
	"expiration"	text NOT NULL,
	"cookie_data"	text NOT NULL,
	PRIMARY KEY("cookie_data")
);
CREATE TABLE IF NOT EXISTS "schema_version" (
	"current_version"	text NOT NULL,
	"release_name"	text NOT NULL
);
INSERT INTO "revoked_cookie" ("expiration","cookie_data") VALUES ('2121-12-03T16:51:35Z','testuser|Fri%2C%2003%20Dec%202021%2016%3A51%3A35%20GMT|C6q0UIukPWyg%2BS3YrUHE4950moStx1zff%2BWLcG0H7G0%3D'),
 ('2121-12-03T16:54:06Z','testuser3|Fri%2C%2003%20Dec%202021%2016%3A54%3A06%20GMT|5AOkE0s9HxBdks65KmBl2%2FwC88yZV%2FR1bwy1cv9YlIA%3D');
INSERT INTO "schema_version" ("current_version","release_name") VALUES ('20210712182145921760944','Ghost Orchid');
CREATE INDEX IF NOT EXISTS "revoked_cookie_expiration_index" ON "revoked_cookie" (
	"expiration"
);
COMMIT;
