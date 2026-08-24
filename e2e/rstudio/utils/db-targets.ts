/**
 * Database target descriptors for the Connections pane tests.
 *
 * A DbTarget describes one database engine end to end: the ODBC driver we
 * register for it, the throwaway server the suite provisions, the values the
 * New Connection wizard needs, the objects the test seeds through DBI, and
 * the shape the object explorer is expected to show. Everything
 * engine-specific lives here; the page objects, actions, and specs are
 * engine-agnostic and iterate over whichever targets are available.
 *
 * Zero configuration by default: the suite provisions a throwaway server on
 * the constants below and deletes it at teardown. The single override, for
 * pointing at an existing (e.g. remote) database instead, is one environment
 * variable per target:
 *
 *   PW_DB_<ID>="host=db.example.com;port=5432;database=x;user=y;password=z"
 *
 * (e.g. PW_DB_POSTGRES). When set, provisioning is skipped for that target
 * and all connection values come from the override.
 */

import * as fs from 'fs';
import * as path from 'path';

interface DbTargetBase {
  /** Short engine id: 'postgres', 'sqlite', ... */
  id: string;

  /**
   * The name we register the driver under (an odbcinst.ini stanza on
   * macOS/Linux, an HKLM ODBCINST.INI key on Windows). Also the name the New
   * Connection wizard displays, which makes the wizard list item id
   * deterministic (see wizardPageId in utils/connections.ts).
   *
   * Every one of these carries a " (pw)" suffix, and must keep it. Windows has
   * no ODBCSYSINI to isolate us, so the suite registers into the machine's own
   * driver list alongside whatever is already installed -- and psqlODBC's
   * installer registers a bare "PostgreSQL Unicode" (verified on
   * windows-2025, which also gets ANSI and (x64) variants). Reusing a vendor
   * name means either clobbering a real driver or refusing to register at all.
   * The suffix is carried on macOS and Linux too, so one name works
   * everywhere and the snippet filename and wizard element id stay identical
   * across platforms.
   */
  driverName: string;

  /**
   * The connection "type" the pane files this engine under: the driver's
   * reported DBMS name (`connection@info$dbms.name`), which together with
   * the host id forms the connection's identity in the pane's history. Both
   * verified empirically against the real drivers, not derived.
   */
  connectionType: string;

  /**
   * Candidate absolute paths for the driver library, per platform; the first
   * one that exists wins. Windows is absent for now: it has its own driver
   * manager (registry-based, no unixODBC), handled in the CI-enablement
   * phase.
   */
  driverLibraries: Partial<Record<NodeJS.Platform, string[]>>;

  /**
   * Windows only: matched against the driver names the vendor's own installer
   * registered under the machine-wide ODBCINST.INI, to discover where the DLL
   * actually landed. Windows has no ODBCSYSINI to redirect and no single
   * predictable install path -- psqlODBC installs under a version-numbered
   * directory (psqlODBC\1600\bin) -- so a literal candidate list like
   * driverLibraries would rot on every driver update. Reading the installer's
   * own registration is authoritative instead of guessing.
   *
   * Absent means the target has no Windows support yet.
   */
  windowsInstalledDriverPattern?: RegExp;

  /**
   * What the connection points at. For a 'server' target this is the database
   * name on that server. For a 'file' target it is the absolute path of the
   * database file, which only effectiveTarget can fill in: the file lives in
   * the sandbox, whose path is not known until globalSetup has created it.
   */
  database: string;

  /**
   * Values the test types into the wizard's labeled fields, keyed by the
   * snippet placeholder key (which the wizard renders as "<Key>:"). Fields
   * with defaults baked into the snippet (Server, Port) are not listed; the
   * test exercises typing into the blank ones.
   */
  wizardFields: Record<string, string>;

  /** SQL run through DBI after connecting, to seed schemas/tables/rows. */
  seedSql: string[];

  /**
   * R expressions evaluated over the wizard-opened connection `con`, each
   * yielding a single TRUE when the seeded data queries correctly. Engine
   * SQL (catalog views, dialect) lives here, keeping the specs engine-blind.
   */
  verifyQueriesR: string[];

  /**
   * Whether the explorer tree's root node is the catalog (the connected
   * database itself), as with PostgreSQL. When true, drilling prepends the
   * effective database name to explorerPath.
   */
  explorerRootIsCatalog: boolean;

  /**
   * Container path the object explorer drills through below the root to
   * reach a seeded table (engine-specific nesting: PostgreSQL is schema,
   * then table; MySQL has no separate schema level). Last element is the
   * table.
   */
  explorerPath: string[];

  /** Column names expected under that table. */
  tableColumns: string[];
}

/**
 * An engine reached over TCP, on a server the suite provisions (or adopts).
 * Identity in the pane's history is built from database, user, and host.
 */
export interface ServerDbTarget extends DbTargetBase {
  kind: 'server';
  host: string;
  port: number;
  user: string;
  password: string;
}

/**
 * An embedded engine whose driver opens a database file directly, with no
 * server, port, or credentials anywhere in the picture (SQLite). Nothing is
 * provisioned and nothing is torn down; the file is created on first connect
 * and removed with the sandbox.
 *
 * The driver reports the file path as BOTH the connection's host id and its
 * display name (verified against sqliteodbc), so the path is the whole
 * identity -- see connectionHostId / connectionDisplayName.
 */
export interface FileDbTarget extends DbTargetBase {
  kind: 'file';

  /**
   * Basename of the database file. effectiveTarget joins it onto the
   * sandbox's db/<id>/ directory to produce the absolute path that becomes
   * `database`.
   */
  fileName: string;
}

/**
 * Discriminated on `kind` deliberately: it makes the type system refuse any
 * read of host/port/user/password on a file target, which is what keeps a
 * server assumption from silently reaching SQLite (e.g. probing a TCP port
 * that does not exist, and reporting the resulting failure as a skip).
 */
export type DbTarget = ServerDbTarget | FileDbTarget;

/** Everything a connection needs, after applying any override. */
export type EffectiveDbTarget = DbTarget & { overridden: boolean };

export const POSTGRES: ServerDbTarget = {
  id: 'postgres',
  kind: 'server',
  driverName: 'PostgreSQL Unicode (pw)',
  connectionType: 'PostgreSQL',
  driverLibraries: {
    darwin: [
      '/opt/homebrew/lib/psqlodbcw.so', // Homebrew, Apple Silicon
      '/usr/local/lib/psqlodbcw.so', // Homebrew, Intel
    ],
    linux: [
      '/usr/lib/x86_64-linux-gnu/odbc/psqlodbcw.so', // Debian/Ubuntu (odbc-postgresql)
      '/usr/lib/aarch64-linux-gnu/odbc/psqlodbcw.so',
      '/usr/lib64/psqlodbcw.so', // Fedora/Rocky (postgresql-odbc)
    ],
    // Windows resolves through the registry instead; see
    // windowsInstalledDriverPattern below.
  },
  // Verified on windows-2025: psqlODBC's installer registers four names --
  // "PostgreSQL ANSI", "PostgreSQL ANSI(x64)", "PostgreSQL Unicode" and
  // "PostgreSQL Unicode(x64)". Match the 64-bit Unicode one specifically: the
  // "(" excludes the ANSI pair and the suffixless variant, and the runner is
  // 64-bit. The DLL it points at is podbc35w.dll, in a versioned directory
  // (C:\Program Files\psqlODBC\1800\bin), which is why this is discovered
  // rather than hardcoded.
  windowsInstalledDriverPattern: /^PostgreSQL Unicode\(/,
  host: '127.0.0.1',
  // Nonstandard port so the throwaway server can never collide with a
  // developer's own PostgreSQL on 5432.
  port: 55432,
  // Database names are per-engine ("pw" + engine) while the role is shared.
  // The pane's display name is built from database, user, and host, so
  // per-engine databases keep every list entry distinguishable -- and no
  // name may contain another's, since a substring collision would silently
  // select the wrong row.
  database: 'pwpostgresql',
  user: 'pwtest',
  password: 'pwtest',
  wizardFields: {
    Database: 'pwpostgresql',
    User: 'pwtest',
    Password: 'pwtest',
  },
  // Idempotent: every connections spec seeds the same provisioned database
  // within one suite run, so re-running must converge, not error.
  seedSql: [
    'CREATE SCHEMA IF NOT EXISTS sales',
    'CREATE SCHEMA IF NOT EXISTS hr',
    'CREATE TABLE IF NOT EXISTS sales.orders (id int PRIMARY KEY, customer text, amount numeric)',
    'TRUNCATE sales.orders',
    "INSERT INTO sales.orders VALUES (1, 'Alfa', 100.50), (2, 'Bravo', 42.00), (3, 'Charlie', 7.25)",
    'CREATE TABLE IF NOT EXISTS sales.customers (id int PRIMARY KEY, name text)',
    'TRUNCATE sales.customers',
    "INSERT INTO sales.customers VALUES (1, 'Alfa'), (2, 'Bravo')",
    'CREATE TABLE IF NOT EXISTS hr.employees (id int PRIMARY KEY, name text, hired date)',
    'TRUNCATE hr.employees',
    "INSERT INTO hr.employees VALUES (1, 'Dora', '2024-01-15')",
  ],
  // Counts come back from the odbc driver as integer64: compare through
  // as.numeric(), never print raw (a bare cat() of an integer64 emits its
  // bit pattern).
  verifyQueriesR: [
    'identical(as.numeric(DBI::dbGetQuery(con, "SELECT count(*) AS n FROM sales.orders")$n), 3)',
    'all(c("customers", "orders") %in% DBI::dbGetQuery(con,' +
      ' "SELECT tablename FROM pg_tables WHERE schemaname = \'sales\'")$tablename)',
    'identical(as.numeric(DBI::dbGetQuery(con, "SELECT sum(amount) AS s FROM sales.orders")$s), 149.75)',
  ],
  explorerRootIsCatalog: true,
  explorerPath: ['sales', 'orders'],
  tableColumns: ['id', 'customer', 'amount'],
};

export const MYSQL: ServerDbTarget = {
  id: 'mysql',
  kind: 'server',
  // The MariaDB ODBC connector drives MySQL servers; it is the one packaged
  // everywhere (Oracle's connector is not in Homebrew), and it speaks MySQL
  // 9's caching_sha2_password authentication (verified: connector 3.2.9
  // against MySQL 9.7).
  driverName: 'MySQL',
  connectionType: 'MySQL',
  driverLibraries: {
    darwin: [
      '/opt/homebrew/lib/mariadb/libmaodbc.dylib', // Homebrew, Apple Silicon
      '/usr/local/lib/mariadb/libmaodbc.dylib', // Homebrew, Intel
    ],
    linux: [
      '/usr/lib/x86_64-linux-gnu/odbc/libmaodbc.so', // Debian/Ubuntu (odbc-mariadb)
      '/usr/lib/aarch64-linux-gnu/odbc/libmaodbc.so',
      '/usr/lib64/libmaodbc.so', // Fedora/Rocky (mariadb-connector-odbc)
    ],
  },
  host: '127.0.0.1',
  port: 53306,
  // Per-engine database name; see the POSTGRES target for why.
  database: 'pwmysql',
  user: 'pwtest',
  password: 'pwtest',
  wizardFields: {
    Database: 'pwmysql',
    User: 'pwtest',
    Password: 'pwtest',
  },
  // MySQL has no separate schema level (a "schema" IS a database), so the
  // seeded tables live directly in the connected database.
  seedSql: [
    'CREATE TABLE IF NOT EXISTS orders (id int PRIMARY KEY, customer text, amount numeric(10,2))',
    'TRUNCATE orders',
    "INSERT INTO orders VALUES (1, 'Alfa', 100.50), (2, 'Bravo', 42.00), (3, 'Charlie', 7.25)",
    'CREATE TABLE IF NOT EXISTS customers (id int PRIMARY KEY, name text)',
    'TRUNCATE customers',
    "INSERT INTO customers VALUES (1, 'Alfa'), (2, 'Bravo')",
    'CREATE TABLE IF NOT EXISTS employees (id int PRIMARY KEY, name text, hired date)',
    'TRUNCATE employees',
    "INSERT INTO employees VALUES (1, 'Dora', '2024-01-15')",
  ],
  // Aliased column names below: MySQL returns information_schema columns
  // uppercased unless aliased.
  verifyQueriesR: [
    'identical(as.numeric(DBI::dbGetQuery(con, "SELECT count(*) AS n FROM orders")$n), 3)',
    'all(c("customers", "orders") %in% DBI::dbGetQuery(con,' +
      ' "SELECT table_name AS tn FROM information_schema.tables WHERE table_schema = \'pwmysql\'")$tn)',
    'identical(as.numeric(DBI::dbGetQuery(con, "SELECT sum(amount) AS s FROM orders")$s), 149.75)',
  ],
  explorerRootIsCatalog: true,
  explorerPath: ['orders'],
  tableColumns: ['id', 'customer', 'amount'],
};

/**
 * SQLite through the sqliteodbc driver: the cheapest possible target, since
 * there is no server to install, start, or stop on any platform. Every value
 * below that the driver decides was read back from it rather than assumed
 * (see the FileDbTarget note about the path being the connection identity).
 */
export const SQLITE: FileDbTarget = {
  id: 'sqlite',
  kind: 'file',
  // Suffixed for the same reason as the PostgreSQL target; see driverName on
  // the interface. sqliteodbc's Windows installer registers "SQLite3 ODBC
  // Driver", so a bare "SQLite3" would not collide today, but the convention
  // is uniform so a future installer cannot take it.
  driverName: 'SQLite3 (pw)',
  connectionType: 'SQLite',
  driverLibraries: {
    darwin: [
      '/opt/homebrew/lib/libsqlite3odbc.dylib', // Homebrew, Apple Silicon
      '/usr/local/lib/libsqlite3odbc.dylib', // Homebrew, Intel
    ],
    linux: [
      '/usr/lib/x86_64-linux-gnu/odbc/libsqlite3odbc.so', // Debian/Ubuntu (libsqliteodbc)
      '/usr/lib/aarch64-linux-gnu/odbc/libsqlite3odbc.so',
      // Fedora's sqliteodbc package, and on RHEL/Rocky (which package none)
      // the same path is where install-deps/linux.sh installs its source build.
      '/usr/lib64/libsqlite3odbc.so',
    ],
  },
  // Verified on windows-2025: the installer registers "SQLite3 ODBC Driver",
  // and installs the DLL straight into C:\Windows\system32 rather than a
  // directory of its own -- which is why the sandbox copies the single DLL and
  // never its parent directory. This pattern also matches our own
  // "SQLite3 (pw)" name, which winInstalledDriverLibrary excludes explicitly.
  windowsInstalledDriverPattern: /^SQLite3 /,
  // Filled in by effectiveTarget; the sandbox path is a run-time value.
  database: '',
  fileName: 'pwsqlite.db',
  // The only field the snippet leaves blank, and the only one there is: the
  // driver takes a file path and nothing else.
  wizardFields: {
    Database: '',
  },
  // SQLite has no schema level and no TRUNCATE. DELETE FROM is the
  // equivalent, and like the other targets this has to converge on a re-run
  // rather than error.
  seedSql: [
    'CREATE TABLE IF NOT EXISTS orders (id int PRIMARY KEY, customer text, amount numeric)',
    'DELETE FROM orders',
    "INSERT INTO orders VALUES (1, 'Alfa', 100.50), (2, 'Bravo', 42.00), (3, 'Charlie', 7.25)",
    'CREATE TABLE IF NOT EXISTS customers (id int PRIMARY KEY, name text)',
    'DELETE FROM customers',
    "INSERT INTO customers VALUES (1, 'Alfa'), (2, 'Bravo')",
    'CREATE TABLE IF NOT EXISTS employees (id int PRIMARY KEY, name text, hired date)',
    'DELETE FROM employees',
    "INSERT INTO employees VALUES (1, 'Dora', '2024-01-15')",
  ],
  verifyQueriesR: [
    'identical(as.numeric(DBI::dbGetQuery(con, "SELECT count(*) AS n FROM orders")$n), 3)',
    'all(c("customers", "orders") %in% DBI::dbGetQuery(con,' +
      ' "SELECT name AS tn FROM sqlite_master WHERE type = \'table\'")$tn)',
    'identical(as.numeric(DBI::dbGetQuery(con, "SELECT sum(amount) AS s FROM orders")$s), 149.75)',
  ],
  // Flat hierarchy: odbcListObjects at the root returns the tables directly,
  // with no catalog or schema container above them (unlike both server
  // engines). Read off the driver, not assumed.
  explorerRootIsCatalog: false,
  explorerPath: ['orders'],
  tableColumns: ['id', 'customer', 'amount'],
};

/**
 * The targets every connections spec iterates.
 *
 * MYSQL is deliberately absent: it is the only target needing a server
 * installed on every platform (no runner image ships one), so it is parked
 * while the suite goes cross-platform. The descriptor above and
 * scripts/db/mysql/ stay in place, so re-enabling is this one line plus
 * uncommenting the two formulae in scripts/db/install-deps/macos.sh.
 */
export const ALL_DB_TARGETS: DbTarget[] = [POSTGRES, SQLITE];

/**
 * The wizard snippet registered next to the driver symlink
 * (snippets/<driverid>.R). Placeholders follow the product's
 * ${order:Key=default} grammar (NewConnectionSnippetHost). Server and Port
 * share order 1 deliberately: duplicate order numbers render as the wizard's
 * two-field first row, so the page object's handling of that layout stays
 * exercised. Database/User/Password are blank for the test to fill.
 *
 * Modeled on the snippet the professional drivers ship (e.g. the Redshift
 * driver's snippets/redshift.R), so Family A drives the same labeled-fields
 * wizard path users see with those drivers.
 */
export function wizardSnippet(t: EffectiveDbTarget): string {
  // A file target has one parameter and no defaults to prefill: the driver
  // wants a path and nothing else, so there is no two-field first row and no
  // credentials. Database is left blank for the test to type, matching how
  // the server snippet treats its credential fields.
  if (t.kind === 'file') {
    return [
      'library(DBI)',
      'con <- dbConnect(',
      '  odbc::odbc(),',
      `  Driver   = "${t.driverName}",`,
      '  Database = "${1:Database}",',
      '  timeout  = 10',
      ')',
      '',
    ].join('\n');
  }

  return [
    'library(DBI)',
    'con <- dbConnect(',
    '  odbc::odbc(),',
    `  Driver   = "${t.driverName}",`,
    `  Server   = "\${1:Server=${t.host}}",`,
    `  Port     = "\${1:Port=${t.port}}",`,
    '  Database = "${2:Database}",',
    '  UID      = "${3:User}",',
    '  PWD      = "${4:Password}",',
    '  timeout  = 10',
    ')',
    '',
  ].join('\n');
}

/**
 * The absolute directory provisionRemoteOdbcSandbox (utils/connections.ts)
 * resolved and created in-session for file-kind target `id`'s database, or
 * null if that step hasn't run (not a server-mode run needing it) or didn't
 * cover this target (its driver wasn't found remotely either).
 *
 * Read directly from the status file connections.ts writes
 * (remote-odbc-status.json), rather than through that module's own
 * readRemoteOdbcStatus, to avoid a circular import: connections.ts already
 * imports effectiveTarget from this file.
 */
function remoteFileDatabaseDir(sandbox: string, id: string): string | null {
  let raw: string;
  try {
    raw = fs.readFileSync(path.join(sandbox, 'remote-odbc-status.json'), 'utf-8');
  } catch {
    return null;
  }
  try {
    const parsed = JSON.parse(raw) as { fileDatabases?: Record<string, unknown> };
    const dir = parsed.fileDatabases?.[id];
    return typeof dir === 'string' ? dir : null;
  } catch {
    return null;
  }
}

/**
 * Apply the PW_DB_<ID> override, if present, to a target. Malformed override
 * text throws: a typo should surface immediately, not as a connect failure.
 */
export function effectiveTarget(target: DbTarget): EffectiveDbTarget {
  const raw = process.env[`PW_DB_${target.id.toUpperCase()}`];

  // A file target has one meaningful value, the path, so it takes the
  // override verbatim rather than parsing key=value pairs. Unset, the file
  // lives in the sandbox beside where a server target's data directory would
  // (db/<id>/), so it is removed with everything else at teardown. With no
  // sandbox at all the path stays empty and dbAvailability skips the specs
  // with that reason, rather than the driver creating a file somewhere
  // unexpected.
  //
  // The sandbox-relative path only works when the test session shares a
  // filesystem AND a user with this test runner -- true for Desktop and a
  // locally-spawned rserver-dev, false for a CI same-machine installed
  // server or a genuinely external one, which run rsessions as a different
  // (possibly remote) user. For those, provisionRemoteOdbcSandbox creates
  // the database directory in-session and records where it landed; prefer
  // that when it exists.
  if (target.kind === 'file') {
    const sandbox = process.env.PW_SANDBOX;
    const remoteDir = sandbox ? remoteFileDatabaseDir(sandbox, target.id) : null;
    // Forward slashes, on every platform. This path is typed into the wizard's
    // Database field, and the wizard interpolates it verbatim into the R code
    // it generates. A Windows path's backslashes are escape sequences there:
    // "\a" is a bell, "\r" a carriage return, and "\p" / "\d" / "\s" are
    // invalid, so the generated dbConnect() call either fails to parse or opens
    // some other file -- while the code panel still *looks* right, which is why
    // the wizard specs passed and only the connecting ones failed. R, SQLite
    // and the ODBC driver all accept forward slashes on Windows. remoteDir is
    // already an absolute path resolved on a Linux machine (RStudio Server is
    // Linux-only), so it needs no such normalization.
    const database = raw
      ? raw.trim()
      : remoteDir
        ? `${remoteDir}/${target.fileName}`
        : sandbox
          ? path.join(sandbox, 'db', target.id, target.fileName).split(path.sep).join('/')
          : '';
    return {
      ...target,
      overridden: !!raw,
      database,
      wizardFields: { Database: database },
    };
  }

  if (!raw) return { ...target, overridden: false };

  const allowed = ['host', 'port', 'database', 'user', 'password'] as const;
  const result: EffectiveDbTarget = { ...target, overridden: true };
  for (const part of raw.split(';')) {
    if (!part.trim()) continue;
    const eq = part.indexOf('=');
    const key = part.slice(0, eq).trim() as (typeof allowed)[number];
    const value = part.slice(eq + 1).trim();
    if (eq < 0 || !allowed.includes(key) || !value) {
      throw new Error(
        `PW_DB_${target.id.toUpperCase()}: bad segment "${part}" (expected ${allowed.join('|')}=value, separated by ";")`,
      );
    }
    if (key === 'port') result.port = Number(value);
    else result[key] = value;
  }
  // The wizard fields mirror the connection values.
  result.wizardFields = {
    Database: result.database,
    User: result.user,
    Password: result.password,
  };
  return result;
}

/**
 * The connection's display name in the pane's list, as the odbc package
 * builds it (`computeDisplayName`): "<database> - <user>@<server>".
 *
 * Matched exactly, never as a substring. The targets share a role name, so
 * every display name contains "pwtest" and a loose match selects whichever
 * row happens to come first -- which fails as a wrong-row success rather
 * than a missing-element error.
 */
export function connectionDisplayName(t: EffectiveDbTarget): string {
  // A file engine has no user or server to name it by, and the driver reports
  // the path itself. Verified against sqliteodbc: both the display name and
  // the host id come back as exactly the string passed as Database, so this
  // must stay byte-identical to what the wizard was given.
  if (t.kind === 'file') return t.database;
  return `${t.database} - ${t.user}@${t.host}`;
}

/**
 * The host half of the connection's identity in the pane's history, as odbc
 * builds it (`computeHostName`): user, database, and server joined by "_",
 * with duplicates dropped -- so a database named after its user yields two
 * parts, not three. Verified against both drivers.
 */
export function connectionHostId(t: EffectiveDbTarget): string {
  // See connectionDisplayName: for a file engine the path is the whole
  // identity, so the pane's history is keyed by it directly.
  if (t.kind === 'file') return t.database;
  return [...new Set([t.user, t.database, t.host])].join('_');
}

/** First driver library candidate that exists on this machine, or null. */
export function resolveDriverLibrary(target: DbTarget): string | null {
  const candidates = target.driverLibraries[process.platform] ?? [];
  return candidates.find((p) => fs.existsSync(p)) ?? null;
}
