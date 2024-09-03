## Server Database Schemas

This folder contains the database schemas for the RStudio Server database. These schemas are copied to the build output directory, and are applied to the database every time `rserver` is started.

Schema files rely on a file format convention containing the following:
* The date time at which the script was generated in the format YYYYMMDDHHmmssnnnnnnnnn (where nnnnnnnnn is nanoseconds)
* An underscore separating the date time from the friendly name
* A friendly name for what the script does
* A file extension, which can be `.sql`, `.sqlite`, or `.postgresql`. `.sql` files will be run for any database type, but the other two extensions are reserved for only running if the database in use corresponds to that file extension.

### Generating New Schemas

It can be cumbersome to generate the correct schema file names as they include a precise timestamp. For this purpose, simply run the `make-schema.sh` script from this folder, like so:

```
./make-schema AddNewTable sql
```

This will generate a file like `20200226141952248123456_AddNewTable.sql`. To generate schemas files that are database specific, leave off the `sql` part of the command, which will generate a schema file for each supported database type.

You should not generate both a `.sql` and specific database extension types for the same migration. In this case, both migrations would be run, but this is nonsensical. You should either generate one `.sql` migration for the generic case, or one migration for each specific targeted database, not both.

### Dev Schema Updates

In order to run any new schemas, you simply need to restart `rstudio-server` to ensure that any unapplied schemas are applied. Note that there is currently no way to rollback successful schema updates, so you are responsible for backing up your existing data if that is important to you.

### SQL Naming

Within your schema files, make sure to use `snake_case` naming for all table and column names. There are several reasons for preferring this. For more information, see https://github.com/rstudio/rstudio/issues/6589.

### Make Compatible Schema Changes

Ensure old code will work with the new schema for when users upgrade and downgrade. Use default values that makes sense for new columns. Always use column names in the schema (no wildcards), no destructive changes, no name changes. It's ok to abandon columns, that could eventually get cleaned up once affected versions are out of support.

## Documentation

When you change the database schema, ensure you also update the data dictionary in the documentation, found in `docs/server/data_dictionary`.

### Updating Schema Migration Tests

For each workbench version where there's a schema change, we generate a database dump of the previous version to test against the 'alter' script we are adding.

Generating these dumps is automated. Run the script:
```
./build-version-dump.sh
```
It will prompt you for the previous version's flower and version number, i.e. the current released version, and postgres user/password. It must be run on a system with psql and sqlite3 installed. It generates
files for the previous version in src/cpp/server/db/test that you commit with your schema changes.

Also update `ServerDatabaseMigrationTests.cpp` and `ServerDatabaseDataset.hpp` to add to the enum and and where it points to the new files in db/test that were just created.

If you are making the schema change in OS, run the script OS with the OS version, then again once the changes have been merged to pro with the pro version. The database dumps are specific to the CreateTables files that are different in OS and pro and so must be generated separately on each branch.
