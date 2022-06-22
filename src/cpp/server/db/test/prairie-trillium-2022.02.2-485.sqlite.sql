BEGIN TRANSACTION;
CREATE TABLE IF NOT EXISTS "active_session_metadata" (
	"session_id"	text NOT NULL,
	"user_id"	integer NOT NULL,
	"workbench"	text NOT NULL,
	"created"	text NOT NULL,
	"last_used"	text NOT NULL,
	"r_version"	text,
	"r_version_label"	text,
	"r_version_home"	text,
	"project"	text,
	"working_directory"	text,
	"activity_state"	text NOT NULL,
	"label"	text NOT NULL,
	"launch_parameters"	text NOT NULL,
	"save_prompt_required"	text NOT NULL DEFAULT 'not_required',
	FOREIGN KEY("user_id") REFERENCES "licensed_users"("id") ON DELETE CASCADE,
	PRIMARY KEY("session_id")
);
CREATE TABLE IF NOT EXISTS "licensed_users" (
	"user_name"	text NOT NULL,
	"locked"	boolean NOT NULL DEFAULT 0,
	"last_sign_in"	text NOT NULL,
	"is_admin"	boolean NOT NULL DEFAULT 0,
	"user_id"	integer NOT NULL DEFAULT -1,
	"id"	integer,
	PRIMARY KEY("id")
);
CREATE TABLE IF NOT EXISTS "revoked_cookie" (
	"expiration"	text NOT NULL,
	"cookie_data"	text NOT NULL,
	PRIMARY KEY("cookie_data")
);
CREATE TABLE IF NOT EXISTS "schema_version" (
	"current_version"	text NOT NULL,
	"release_name"	text NOT NULL
);
INSERT INTO "revoked_cookie" ("expiration","cookie_data") VALUES ('2022-05-18T21:24:10Z','testuser|Wed%2C%2018%20May%202022%2021%3A24%3A10%20GMT|Rl4BZ9mYCj2wlRtzUu%2BbHVaPp1377369CF6z7ySJumI%3D'),
 ('2022-05-18T21:24:58Z','testuser2|Wed%2C%2018%20May%202022%2021%3A24%3A58%20GMT|UpdYpwJkCiI2ToGXRJVOkd3DTQcNzddpWV2YeizVKhM%3D');
INSERT INTO "schema_version" ("current_version","release_name") VALUES ('20210916132211194382021','Prairie Trillium');
CREATE INDEX IF NOT EXISTS "revoked_cookie_expiration_index" ON "revoked_cookie" (
	"expiration"
);
COMMIT;
