#include <tests/TestThat.hpp>
#include <core/Database.hpp>
#include <core/system/System.hpp>
#include <core/FileSerializer.hpp>
#include <boost/filesystem.hpp>
#include "ServerDatabaseDataset.hpp"

#include <map>

using namespace rstudio::core;
using namespace rstudio::core::database;

namespace rstudio {
namespace server {
namespace db {

//Forward Declare Overlay
namespace overlay {
    void overlayDatasetPath(std::map<DatasetVersion, std::map<DatasetType, std::string>>& datasetPath);
}

// When adding additional datasets are made available, the following pieces
// need to be updated. Add a DatasetVersion value to the enum in
// ServerDatabaseDataset.hpp. Then add a version entry to the datasetPath,
// providing the path to sqlite and postgres database dump for that version.
// Finally update the versionToString function to include the new dataset.
// That completes necessary updates to this test.
// In Summary:
//      1) Add new version name to ServerDatabaseDataset.hpp
//      2) Add entry to datasetPath
//      3) Add switch case to versionToString function.

std::map<DatasetVersion, std::map<DatasetType, std::string>> datasetPath {
    {
        JulietRose, {
            {Sqlite, "db/test/juliet-rose-1.4.1717.sqlite.sql"},
            {Postgres, "db/test/juliet-rose-1.4.1717.postgresql"}
        }
    },
    {
        GhostOrchid, {
            {Sqlite, "db/test/ghost-orchid-2021.09.1-372.sqlite.sql"},
            {Postgres, "db/test/ghost-orchid-2021.09.1-372.postgresql"}
        }
    },
    {
        PrairieTrillium, {
            {Sqlite, "db/test/prairie-trillium-2022.02.2-485.sqlite.sql"},
            {Postgres, "db/test/prairie-trillium-2022.02.2-485.postgresql"}
        }
    }
};

std::string versionToString(DatasetVersion version)
{
    switch(version)
    {
        case JulietRose:
            return "Juliet Rose";
        case GhostOrchid:
            return "Ghost Orchid";
        case PrairieTrillium:
            return "Prairie Trillium";
        default:
            return "UNKNOWN";
    }
}

std::string typeToString(DatasetType type)
{
    switch(type)
    {
        case Sqlite:
            return "Sqlite";
        case Postgres:
            return "Postgres";
        default:
            return "UNKNOWN";
    }
}

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
   const char* dbName = std::getenv("RSTUDIO_TEST_DB_NAME");
   options.database = (dbName) ? dbName : "rstudio_test";
   const char* dbHost = std::getenv("RSTUDIO_TEST_DB_HOST");
   options.host = (dbHost) ? dbHost : "localhost";
   const char* dbUser = std::getenv("RSTUDIO_TEST_DB_USER");
   options.username = (dbUser) ? dbUser : "postgres";
   const char* dbPass = std::getenv("RSTUDIO_TEST_DB_PASS");
   options.password = (dbPass) ? dbPass : "postgres";

   return options;
}

void updateTest(DatasetVersion version, DatasetType type, std::string path){
    std::string testName = "A "+ versionToString(version) + " " + typeToString(type) + " Database";
    GIVEN(testName)
    {
        FilePath workingDir = system::currentWorkingDir(system::currentProcessId());
        boost::shared_ptr<IConnection> connection;

        if (type == Sqlite)
        {
            SqliteConnectionOptions options = sqliteConnectionOptions();
            //Delete the db if it exists
            FilePath db(options.file);
            db.removeIfExists();

            REQUIRE_FALSE(connect(options, &connection));
        }
        else if (type == Postgres)
        {
            PostgresqlConnectionOptions options = postgresConnectionOptions();
            REQUIRE_FALSE(connect(options, &connection));

            std::string queryStr =
            R""(
            DROP SCHEMA public CASCADE;
            CREATE SCHEMA public;
            GRANT ALL ON SCHEMA public TO )"" + options.username + R""(;
            GRANT ALL ON SCHEMA public TO public;
            ALTER DATABASE )"" + options.database + R""( SET search_path = public;
            )"";

            REQUIRE_FALSE(connection->executeStr(queryStr));
        }

        std::string dbDump;
        FilePath dbDumpPath = workingDir.completeChildPath(datasetPath[version][type]);
        REQUIRE(dbDumpPath.exists());
        REQUIRE_FALSE(readStringFromFile(dbDumpPath, &dbDump));
        REQUIRE_FALSE(connection->executeStr(dbDump));

         // added, because dropping the schema ends up also dropping
         // the search_path for that connection, so we need to reset it
         // for postgres connections. Additionally some of our datasets also
         // overwrite the search_path.
         if (type == Postgres)
            REQUIRE_FALSE(connection->executeStr("SET search_path = public;"));

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

TEST_CASE("Upgrading Sqlite Database","[database][integration][upgrade][sqlite]")
{
    overlay::overlayDatasetPath(datasetPath);
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

    // Attempt an upgrade from each previous version
    for(auto version : datasetPath)
    {
        DatasetVersion ver = version.first;
        DatasetType type = Sqlite;
        std::string datasetPath = version.second[type];
        updateTest(ver, type, datasetPath);
    }
}

TEST_CASE("Upgrading Postgres Database", "[database][integration][upgrade][.postgres]")
{
    overlay::overlayDatasetPath(datasetPath);
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

    // Attempt an upgrade from each previous version
    for(auto version : datasetPath)
    {
        DatasetVersion ver = version.first;
        DatasetType type = Postgres;
        std::string datasetPath = version.second[type];
        updateTest(ver, type, datasetPath);
    }
}

} // namespace db
} // namespace server
} // namespace rstudio
