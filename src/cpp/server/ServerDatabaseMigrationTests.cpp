#include <tests/TestThat.hpp>
#include <core/Database.hpp>
#include <core/system/System.hpp>
#include <core/FileSerializer.hpp>
#include <boost/filesystem.hpp>

#include <iostream>

using namespace rstudio::core;
using namespace rstudio::core::database;

SqliteConnectionOptions sqliteConnectionOptions()
{
    boost::filesystem::path tempPath = boost::filesystem::temp_directory_path();
    FilePath tempDb = FilePath(boost::filesystem::canonical(tempPath).string());
    tempDb = tempDb.completeChildPath("rstudio-migration-test.sqlite");
    return SqliteConnectionOptions { tempDb.getAbsolutePath() };
}

PostgresqlConnectionOptions postgresConnectionOptions()
{
   PostgresqlConnectionOptions options;
   options.connectionTimeoutSeconds = 10;
   options.database = "rstudio_test";
   options.host = "localhost";
   options.username = "postgres";
   options.password = "postgres";

   return options;
}

TEST_CASE("Upgrading Sqlite Database","[.database][.integration][.upgrade][.sqlite]")
{
    GIVEN("An Empty Sqlite Database")
    {
        FilePath workingDir = system::currentWorkingDir(system::currentProcessId());

        boost::shared_ptr<IConnection> connection;
        SqliteConnectionOptions options = sqliteConnectionOptions();
        
        //Delete the db if it exists
        FilePath db(options.file);
        db.removeIfExists();

        REQUIRE_FALSE(connect(options, &connection));

        FilePath migrationPath = workingDir.completeChildPath("db");
        SchemaUpdater schemaUpdater = {connection, migrationPath};

        THEN("Schema is out of date")
        {
            bool emptyUpToDate = false;
            // Is up to date will fail.
            REQUIRE(schemaUpdater.isUpToDate(&emptyUpToDate));
            REQUIRE_FALSE(emptyUpToDate);
        }

        WHEN("The initial DB is created")
        {
            FilePath createDbPath = workingDir.completeChildPath("db/CreateTables.sqlite");
            std::string createDbStr;
            REQUIRE_FALSE(readStringFromFile(createDbPath, &createDbStr));
            REQUIRE_FALSE(connection->executeStr(createDbStr));

            THEN("The Schema is now up to date")
            {
                bool createdUpToDate = false;
                REQUIRE_FALSE(schemaUpdater.isUpToDate(&createdUpToDate));
                REQUIRE(createdUpToDate);
            }
        }
    }

    GIVEN("A populated Juliet Rose Sqlite Database")
    {
        FilePath workingDir = system::currentWorkingDir(system::currentProcessId());

        boost::shared_ptr<IConnection> connection;
        SqliteConnectionOptions options = sqliteConnectionOptions();
        
        //Delete the db if it exists
        FilePath db(options.file);
        db.removeIfExists();

        REQUIRE_FALSE(connect(options, &connection));

        std::string dbDump;
        FilePath julietRoseDumpPath = workingDir.completeChildPath("db/test/juliet-rose-1.4.1717.sqlite.sql");
        REQUIRE_FALSE(readStringFromFile(julietRoseDumpPath, &dbDump));
        REQUIRE_FALSE(connection->executeStr(dbDump));

        FilePath migrationPath = workingDir.completeChildPath("db");
        SchemaUpdater schemaUpdater = {connection, migrationPath};

        THEN("Schema is out of date")
        {
            bool initiallyUpToDate = false;
            REQUIRE_FALSE(schemaUpdater.isUpToDate(&initiallyUpToDate));
            REQUIRE_FALSE(initiallyUpToDate);
        }

        WHEN("Schema updater is run")
        {
            REQUIRE_FALSE(schemaUpdater.update());

            THEN("Schema is up to date")
            {
                bool finallyUpToDate = false;
                REQUIRE_FALSE(schemaUpdater.isUpToDate(&finallyUpToDate));
                REQUIRE(finallyUpToDate);
            }
        }
    }

    GIVEN("A populated Ghost Orchid Sqlite Database")
    {
        FilePath workingDir = system::currentWorkingDir(system::currentProcessId());

        boost::shared_ptr<IConnection> connection;
        SqliteConnectionOptions options = sqliteConnectionOptions();
        
        //Delete the db if it exists
        FilePath db(options.file);
        db.removeIfExists();

        REQUIRE_FALSE(connect(options, &connection));

        std::string dbDump;
        FilePath julietRoseDumpPath = workingDir.completeChildPath("db/test/ghost-orchid-2021.09.1-372.sqlite.sql");
        REQUIRE_FALSE(readStringFromFile(julietRoseDumpPath, &dbDump));
        REQUIRE_FALSE(connection->executeStr(dbDump));

        FilePath migrationPath = workingDir.completeChildPath("db");
        SchemaUpdater schemaUpdater = {connection, migrationPath};

        THEN("Schema is out of date")
        {
            bool initiallyUpToDate = false;
            REQUIRE_FALSE(schemaUpdater.isUpToDate(&initiallyUpToDate));
            REQUIRE_FALSE(initiallyUpToDate);
        }

        WHEN("Schema updater is run")
        {
            REQUIRE_FALSE(schemaUpdater.update());

            THEN("Schema is up to date")
            {
                bool finallyUpToDate = false;
                REQUIRE_FALSE(schemaUpdater.isUpToDate(&finallyUpToDate));
                REQUIRE(finallyUpToDate);
            }
        }
    }
}

TEST_CASE("Upgrading Postgres Database", "[.database][.integration][.upgrade][.postgres]")
{

    GIVEN("An empty Postgres Database")
    {
        FilePath workingDir = system::currentWorkingDir(system::currentProcessId());

        boost::shared_ptr<IConnection> connection;
        PostgresqlConnectionOptions options = postgresConnectionOptions();
        REQUIRE_FALSE(connect(options, &connection));

        std::string queryStr =
         R""(
         DROP SCHEMA public CASCADE;
         CREATE SCHEMA public;
         GRANT ALL ON SCHEMA public TO )"" + options.username + R""(;
         GRANT ALL ON SCHEMA public TO public;
         )"";

        connection->executeStr(queryStr);

        REQUIRE_FALSE(connect(options, &connection));

        FilePath migrationPath = workingDir.completeChildPath("db");
        SchemaUpdater schemaUpdater = {connection, migrationPath};

        THEN("Schema is out of date")
        {
            bool emptyUpToDate = false;
            // Is up to date will fail.
            REQUIRE(schemaUpdater.isUpToDate(&emptyUpToDate));
            REQUIRE_FALSE(emptyUpToDate);
        }

        WHEN("The initial DB is created")
        {
            FilePath createDbPath = workingDir.completeChildPath("db/CreateTables.postgresql");
            std::string createDbStr;
            REQUIRE_FALSE(readStringFromFile(createDbPath, &createDbStr));
            createDbStr = "BEGIN;\n" + createDbStr + "\nCOMMIT;";
            REQUIRE_FALSE(connection->executeStr(createDbStr));

            THEN("The Schema is now up to date")
            {
                bool createdUpToDate = false;
                REQUIRE_FALSE(schemaUpdater.isUpToDate(&createdUpToDate));
                REQUIRE(createdUpToDate);
            }
        }
    }

    GIVEN("A populated Juliet Rose Postgres Database")
    {
        FilePath workingDir = system::currentWorkingDir(system::currentProcessId());

        boost::shared_ptr<IConnection> connection;
        PostgresqlConnectionOptions options = postgresConnectionOptions();
        REQUIRE_FALSE(connect(options, &connection));

        std::string queryStr =
         R""(
         DROP SCHEMA public CASCADE;
         CREATE SCHEMA public;
         GRANT ALL ON SCHEMA public TO )"" + options.username + R""(;
         GRANT ALL ON SCHEMA public TO public;
         )"";

        connection->executeStr(queryStr);

        std::string dbDump;
        FilePath julietRoseDumpPath = workingDir.completeChildPath("db/test/juliet-rose-1.4.1717.postgresql");
        REQUIRE_FALSE(readStringFromFile(julietRoseDumpPath, &dbDump));
        REQUIRE_FALSE(connection->executeStr(dbDump));
        
        //Reconnect to fix a search path problem regarding the public schema
        REQUIRE_FALSE(connect(options, &connection));

        FilePath migrationPath = workingDir.completeChildPath("db");
        SchemaUpdater schemaUpdater = {connection, migrationPath};

        THEN("Schema is out of date")
        {
            bool initiallyUpToDate = false;
            REQUIRE_FALSE(schemaUpdater.isUpToDate(&initiallyUpToDate));
            REQUIRE_FALSE(initiallyUpToDate);
        }

        WHEN("Schema updater is run")
        {
            REQUIRE_FALSE(schemaUpdater.update());

            THEN("Schema is up to date")
            {
                bool finallyUpToDate = false;
                REQUIRE_FALSE(schemaUpdater.isUpToDate(&finallyUpToDate));
                REQUIRE(finallyUpToDate);
            }
        }
    }

    GIVEN("A populated Ghost Orchid Postgres Database")
    {
        FilePath workingDir = system::currentWorkingDir(system::currentProcessId());

        boost::shared_ptr<IConnection> connection;
        PostgresqlConnectionOptions options = postgresConnectionOptions();
        REQUIRE_FALSE(connect(options, &connection));

        std::string queryStr =
         R""(
         DROP SCHEMA public CASCADE;
         CREATE SCHEMA public;
         GRANT ALL ON SCHEMA public TO )"" + options.username + R""(;
         GRANT ALL ON SCHEMA public TO public;
         )"";

        connection->executeStr(queryStr);

        std::string dbDump;
        FilePath julietRoseDumpPath = workingDir.completeChildPath("db/test/ghost-orchid-2021.09.1-372.postgresql");
        REQUIRE_FALSE(readStringFromFile(julietRoseDumpPath, &dbDump));
        dbDump = "BEGIN;\n" + dbDump +"\nCOMMIT;";
        REQUIRE_FALSE(connection->executeStr(dbDump));
        
        //Reconnect to fix a search path problem regarding the public schema
        REQUIRE_FALSE(connect(options, &connection));

        FilePath migrationPath = workingDir.completeChildPath("db");
        SchemaUpdater schemaUpdater = {connection, migrationPath};

        THEN("Schema is out of date")
        {
            bool initiallyUpToDate = false;
            REQUIRE_FALSE(schemaUpdater.isUpToDate(&initiallyUpToDate));
            REQUIRE_FALSE(initiallyUpToDate);
        }

        WHEN("Schema updater is run")
        {
            REQUIRE_FALSE(schemaUpdater.update());

            THEN("Schema is up to date")
            {
                bool finallyUpToDate = false;
                REQUIRE_FALSE(schemaUpdater.isUpToDate(&finallyUpToDate));
                REQUIRE(finallyUpToDate);
            }
        }
    }
}
