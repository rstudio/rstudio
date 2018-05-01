-- !preview dbGetQuery conn=DBI::dbConnect(RSQLite::SQLite())

CREATE TABLE t(x INTEGER PRIMARY KEY ASC, y, z);

INSERT INTO t(x, y, z) VALUES(1, 2, 3);

SELECT * FROM t;
