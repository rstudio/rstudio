#include <gtest/gtest.h>
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

namespace tests {
// When additional datasets are made available the following pieces
// need to be updated: Add a DatasetVersion value to the enum in
// ServerDatabaseDataset.hpp. Then add a version entry to the datasetPath,
// providing the path to sqlite and postgres database dump for that version.
// Dataset entries for Workbench are separately added to overlayDatasetPath.
// Finally update the versionToString function to include the new dataset.
// That completes necessary updates to this test.
// In Summary:
//      1) Add new version name to ServerDatabaseDataset.hpp
//      2) Add entry to datasetPath ( and overlayDatasetPath )
//      3) Add switch case to versionToString function.
//
// Note: Datasets should only be added for *previous* releases after a
// migration file for the *current* release has been created. If you add a
// dataset for the current release the migration tests will fail as there
// is no newer schema for the current database to upgrade to

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
    },
    {
        SpottedWakerobin, {
            {Sqlite, "db/test/spotted-wakerobin-2022.07.2-576.sqlite.sql"},
            {Postgres, "db/test/spotted-wakerobin-2022.07.2-576.postgresql"}
        }
    },
    {
        ElsbethGeranium, {
            {Sqlite, "db/test/elsbeth-geranium-2022.12.0-353.workbench.sqlite.sql"},
            {Postgres, "db/test/elsbeth-geranium-2022.12.0-353.workbench.postgresql"}
        }
    },
    {
        CherryBlossom, {
            {Sqlite, "db/test/cherry-blossom-2023.03.2-454.workbench.sqlite.sql"},
            {Postgres, "db/test/cherry-blossom-2023.03.2-454.workbench.postgresql"}
        }
    },
    {
        MountainHydrangea, {
            {Sqlite, "db/test/mountain-hydrangea-2023.06.2-561.workbench.sqlite.sql"},
            {Postgres, "db/test/mountain-hydrangea-2023.06.2-561.workbench.postgresql"}
        }
    },
    {
        DesertSunflower, {
            {Sqlite, "db/test/desert-sunflower-2023.09.1-494.workbench.sqlite.sql"},
            {Postgres, "db/test/desert-sunflower-2023.09.1-494.workbench.postgresql"}
        }
    },
    {
        OceanStorm, {
            {Sqlite, "db/test/ocean-storm-2023.12.1-402.workbench.sqlite.sql"},
            {Postgres, "db/test/ocean-storm-2023.12.1-401.workbench.postgresql"}
        }
    },
    {
        ChocolateCosmos, {
            {Sqlite, "db/test/chocolate-cosmos-2024.04.2-764.workbench.sqlite.sql"},
            {Postgres, "db/test/chocolate-cosmos-2024.04.2-764.workbench.postgresql"}
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
        case SpottedWakerobin:
            return "Spotted Wakerobin";
        case ElsbethGeranium:
            return "Elsbeth Geranium";
        case CherryBlossom:
            return "Cherry Blossom";
        case MountainHydrangea:
            return "Mountain Hydrangea";
        case DesertSunflower:
            return "Desert Sunflower";
        case OceanStorm:
            return "Ocean Storm";
        case ChocolateCosmos:
            return "Chocolate Cosmos";
        case CranberryHibiscus:
            return "Cranberry Hibiscus";
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
   // Use an in-memory database instead of a file-based one
   // This is faster and ensures complete isolation between test runs
   return SqliteConnectionOptions { ":memory:" };
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

   // Setup test database
   FilePath workingDir = system::currentWorkingDir(system::currentProcessId());
   boost::shared_ptr<IConnection> connection;

   if (type == Sqlite)
   {
      SqliteConnectionOptions options = sqliteConnectionOptions();
      // Connect to the in-memory database
      ASSERT_FALSE(connect(options, &connection));
   }
   else if (type == Postgres)
   {
      PostgresqlConnectionOptions options = postgresConnectionOptions();
      ASSERT_FALSE(connect(options, &connection));

      std::string queryStr =
      R""(
      DROP SCHEMA public CASCADE;
      CREATE SCHEMA public;
      GRANT ALL ON SCHEMA public TO )"" + options.username + R""(;
      GRANT ALL ON SCHEMA public TO public;
      ALTER DATABASE )"" + options.database + R""( SET search_path = public;
      )"";

      ASSERT_FALSE(connection->executeStr(queryStr));
   }

   std::string dbDump;
   FilePath dbDumpPath = workingDir.completeChildPath(datasetPath[version][type]);
   ASSERT_TRUE(dbDumpPath.exists());
   ASSERT_FALSE(readStringFromFile(dbDumpPath, &dbDump));
   ASSERT_FALSE(connection->executeStr(dbDump));

   // added, because dropping the schema ends up also dropping
   // the search_path for that connection, so we need to reset it
   // for postgres connections. Additionally some of our datasets also
   // overwrite the search_path.
   if (type == Postgres)
   {
      ASSERT_FALSE(connection->executeStr("SET search_path = public;"));
   }

   FilePath migrationPath = workingDir.completeChildPath("db");
   SchemaUpdater schemaUpdater = {connection, migrationPath};

   // Test that schema is out of date initially
   bool initiallyUpToDate = false;
   ASSERT_FALSE(schemaUpdater.isUpToDate(&initiallyUpToDate));
   ASSERT_FALSE(initiallyUpToDate);

   // Run schema update
   ASSERT_FALSE(schemaUpdater.update());

   // Test that schema is up to date after update
   bool finallyUpToDate = false;
   ASSERT_FALSE(schemaUpdater.isUpToDate(&finallyUpToDate));
   ASSERT_TRUE(finallyUpToDate);
}

TEST(DatabaseMigrationTest, UpgradeSqliteDatabaseFull)
{
   // Test empty SQLite database
   FilePath workingDir = system::currentWorkingDir(system::currentProcessId());

   boost::shared_ptr<IConnection> connection;
   SqliteConnectionOptions options = sqliteConnectionOptions();
   
   //Delete the db if it exists
   FilePath db(options.file);
   db.removeIfExists();

   ASSERT_FALSE(connect(options, &connection));
   
   // Disable WAL mode to prevent -shm and -wal files from being created
   ASSERT_FALSE(connection->executeStr("PRAGMA journal_mode=DELETE;"));

   FilePath migrationPath = workingDir.completeChildPath("db");
   SchemaUpdater schemaUpdater = {connection, migrationPath};

   // Check schema is out of date
   bool emptyUpToDate = false;
   // Is up to date will fail.
   EXPECT_TRUE(schemaUpdater.isUpToDate(&emptyUpToDate));
   EXPECT_FALSE(emptyUpToDate);

   // Test creating initial DB
   FilePath createDbPath = workingDir.completeChildPath("db/CreateTables.sqlite");
   std::string createDbStr;
   ASSERT_FALSE(readStringFromFile(createDbPath, &createDbStr));
   ASSERT_FALSE(connection->executeStr(createDbStr));
   
   // Check schema is up to date after creation
   bool createdUpToDate = false;
   ASSERT_FALSE(schemaUpdater.isUpToDate(&createdUpToDate));
   ASSERT_TRUE(createdUpToDate);
}

TEST(DatabaseMigrationTest, UpgradeSqliteDatabaseIncremental)
{
   // Attempt an upgrade from each previous version
   overlay::overlayDatasetPath(datasetPath);
   for(auto version : datasetPath)
   {
      DatasetVersion ver = version.first;
      DatasetType type = Sqlite;
      std::string datasetPath = version.second[type];
      updateTest(ver, type, datasetPath);
   }
}

TEST(DatabaseMigrationTest, UpgradePostgresDatabaseFull)
{
   if (!std::getenv("POSTGRES_ENABLED") || std::string(std::getenv("POSTGRES_ENABLED")) != "1")
   {
      GTEST_SKIP() << "Skipping Postgres migration tests as POSTGRES_ENABLED is not set";
   }

   // Test empty Postgres database
   FilePath workingDir = system::currentWorkingDir(system::currentProcessId());

   boost::shared_ptr<IConnection> connection;
   PostgresqlConnectionOptions options = postgresConnectionOptions();
   ASSERT_FALSE(connect(options, &connection));

   std::string queryStr =
   R""(
   DROP SCHEMA public CASCADE;
   CREATE SCHEMA public;
   GRANT ALL ON SCHEMA public TO )"" + options.username + R""(;
   GRANT ALL ON SCHEMA public TO public;
   )"";

   connection->executeStr(queryStr);

   ASSERT_FALSE(connect(options, &connection));

   FilePath migrationPath = workingDir.completeChildPath("db");
   SchemaUpdater schemaUpdater = {connection, migrationPath};

   // Check schema is out of date
   bool emptyUpToDate = false;
   // Is up to date will fail.
   EXPECT_TRUE(schemaUpdater.isUpToDate(&emptyUpToDate));
   EXPECT_FALSE(emptyUpToDate);

   // Test creating initial DB
   FilePath createDbPath = workingDir.completeChildPath("db/CreateTables.postgresql");
   std::string createDbStr;
   ASSERT_FALSE(readStringFromFile(createDbPath, &createDbStr));
   createDbStr = "BEGIN;\n" + createDbStr + "\nCOMMIT;";
   ASSERT_FALSE(connection->executeStr(createDbStr));

   // Check schema is up to date after creation
   bool createdUpToDate = false;
   ASSERT_FALSE(schemaUpdater.isUpToDate(&createdUpToDate));
   ASSERT_TRUE(createdUpToDate);
}

TEST(DatabaseMigrationTest, UpgradePostgresDatabaseIncremental)
{
   if (!std::getenv("POSTGRES_ENABLED") || std::string(std::getenv("POSTGRES_ENABLED")) != "1")
   {
      GTEST_SKIP() << "Skipping Postgres migration tests as POSTGRES_ENABLED is not set";
   }

   overlay::overlayDatasetPath(datasetPath);
   // Attempt an upgrade from each previous version
   for(auto version : datasetPath)
   {
      DatasetVersion ver = version.first;
      DatasetType type = Postgres;
      std::string datasetPath = version.second[type];
      updateTest(ver, type, datasetPath);
   }
}


} // namespace tests
} // namespace db
} // namespace server
} // namespace rstudio