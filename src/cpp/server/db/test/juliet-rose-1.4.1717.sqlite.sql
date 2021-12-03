BEGIN TRANSACTION;
CREATE TABLE IF NOT EXISTS "revoked_cookie" (
	"expiration"	text NOT NULL,
	"cookie_data"	text NOT NULL,
	PRIMARY KEY("cookie_data")
);
CREATE TABLE IF NOT EXISTS "schema_version" (
	"current_version"	text
);
INSERT INTO "revoked_cookie" ("expiration","cookie_data") VALUES ('2121-12-01T20:48:16Z','testuser3|Wed%2C%2001%20Dec%202021%2020%3A48%3A16%20GMT|eCwVWF2ey67r8Siueoojcw593q1omcc9LZY%2F4mEysC0%3D'),
 ('2121-12-01T20:48:44Z','testuser|Wed%2C%2001%20Dec%202021%2020%3A48%3A44%20GMT|vnT2IM5z7Aur4NXvJeHrhU2C%2FOLHoMtQF%2BQThrFLKkI%3D');
INSERT INTO "schema_version" ("current_version") VALUES ('20200226141952248123456_AddRevokedCookie');
CREATE INDEX IF NOT EXISTS "revoked_cookie_expiration_index" ON "revoked_cookie" (
	"expiration"
);
COMMIT;
