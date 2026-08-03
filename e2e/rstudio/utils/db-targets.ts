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

export interface DbTarget {
  /** Short engine id: 'postgres', later 'mysql', ... */
  id: string;

  /**
   * The odbcinst.ini stanza name we register the driver under. Also the name
   * the New Connection wizard displays, which makes the wizard list item id
   * deterministic (see wizardPageId in utils/connections.ts).
   */
  driverName: string;

  /**
   * Candidate absolute paths for the driver library, per platform; the first
   * one that exists wins. Windows is absent for now: it has its own driver
   * manager (registry-based, no unixODBC), handled in the CI-enablement
   * phase.
   */
  driverLibraries: Partial<Record<NodeJS.Platform, string[]>>;

  /** Connection endpoint and credentials (defaults; override replaces them). */
  host: string;
  port: number;
  database: string;
  user: string;
  password: string;

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

/** Everything a connection needs, after applying any override. */
export type EffectiveDbTarget = DbTarget & { overridden: boolean };

export const POSTGRES: DbTarget = {
  id: 'postgres',
  driverName: 'PostgreSQL Unicode',
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
  },
  host: '127.0.0.1',
  // Nonstandard port so the throwaway server can never collide with a
  // developer's own PostgreSQL on 5432.
  port: 55432,
  database: 'pwtest',
  user: 'pwtest',
  password: 'pwtest',
  wizardFields: {
    Database: 'pwtest',
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

export const MYSQL: DbTarget = {
  id: 'mysql',
  // The MariaDB ODBC connector drives MySQL servers; it is the one packaged
  // everywhere (Oracle's connector is not in Homebrew), and it speaks MySQL
  // 9's caching_sha2_password authentication (verified: connector 3.2.9
  // against MySQL 9.7).
  driverName: 'MySQL',
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
  database: 'pwtest',
  user: 'pwtest',
  password: 'pwtest',
  wizardFields: {
    Database: 'pwtest',
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
      ' "SELECT table_name AS tn FROM information_schema.tables WHERE table_schema = \'pwtest\'")$tn)',
    'identical(as.numeric(DBI::dbGetQuery(con, "SELECT sum(amount) AS s FROM orders")$s), 149.75)',
  ],
  explorerRootIsCatalog: true,
  explorerPath: ['orders'],
  tableColumns: ['id', 'customer', 'amount'],
};

export const ALL_DB_TARGETS: DbTarget[] = [POSTGRES, MYSQL];

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
 * Apply the PW_DB_<ID> override, if present, to a target. Malformed override
 * text throws: a typo should surface immediately, not as a connect failure.
 */
export function effectiveTarget(target: DbTarget): EffectiveDbTarget {
  const raw = process.env[`PW_DB_${target.id.toUpperCase()}`];
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

/** First driver library candidate that exists on this machine, or null. */
export function resolveDriverLibrary(target: DbTarget): string | null {
  const candidates = target.driverLibraries[process.platform] ?? [];
  return candidates.find((p) => fs.existsSync(p)) ?? null;
}
