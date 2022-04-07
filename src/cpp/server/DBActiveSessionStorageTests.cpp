#include <tests/vendor/catch.hpp>
#include <server/DBActiveSessionStorage.hpp>
#include <core/system/System.hpp>
#include <core/FileSerializer.hpp>
#include <boost/filesystem.hpp>

using namespace rstudio::core;
using namespace rstudio::core::database;
using namespace rstudio::server::storage;

SqliteConnectionOptions sqliteConnectionOptions()
{
   boost::filesystem::path tempPath = boost::filesystem::temp_directory_path();
   FilePath tempDb = FilePath(boost::filesystem::canonical(tempPath).string());
   tempDb = tempDb.completeChildPath("rstudio-active-session-storage-test.sqlite");
   return SqliteConnectionOptions { tempDb.getAbsolutePath() };
}

boost::shared_ptr<IConnection> initializeSQLiteDatabase(SqliteConnectionOptions options)
{
   // Delete the db if it exists
   FilePath dbPath(options.file);
   REQUIRE_FALSE(dbPath.removeIfExists());

   // Establish sqlite connection
   boost::shared_ptr<IConnection> connection;
   REQUIRE_FALSE(connect(options, &connection));

   // Execute the create db script
   FilePath workingDir = system::currentWorkingDir(system::currentProcessId());
   FilePath createDbPath = workingDir.completeChildPath("db/CreateTables.sqlite");
   std::string createDbStr;
   REQUIRE_FALSE(readStringFromFile(createDbPath, &createDbStr));
   REQUIRE_FALSE(connection->executeStr(createDbStr));

   // Insert a user to own the sessions
   REQUIRE_FALSE(connection->executeStr("INSERT INTO licensed_users (user_name, last_sign_in, user_id, id) VALUES ('test1', '2020-04-30T00:00:00.000Z', 5001, 7)"));
   REQUIRE_FALSE(connection->executeStr("INSERT INTO licensed_users (user_name, last_sign_in, user_id, id) VALUES ('test2', '2020-04-30T00:00:00.000Z', 5002, 8)"));
   
   return connection;
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

boost::shared_ptr<IConnection> initializePostgresqlDatabase(PostgresqlConnectionOptions options)
{
   // Establish the connection
   boost::shared_ptr<IConnection> connection;
   REQUIRE_FALSE(connect(options, &connection));

   // Drop all database info in our test database
   connection->executeStr("DROP SCHEMA public CASCADE;");
   connection->executeStr("CREATE SCHEMA public;");
   connection->executeStr("GRANT ALL ON SCHEMA public TO "+options.username+";");
   connection->executeStr("GRANT ALL ON SCHEMA public TO public");

   // Execute the create db script
   FilePath workingDir = system::currentWorkingDir(system::currentProcessId());
   FilePath createDbPath = workingDir.completeChildPath("db/CreateTables.postgresql");
   std::string createDbStr;
   REQUIRE_FALSE(readStringFromFile(createDbPath, &createDbStr));
   connection->executeStr("BEGIN TRANSACTION");
   REQUIRE_FALSE(connection->executeStr(createDbStr));
   connection->executeStr("COMMIT");

   // Insert a user to own the sessions
   REQUIRE_FALSE(connection->executeStr("INSERT INTO licensed_users (user_name, last_sign_in, user_id, id) VALUES ('test1', '2020-04-30T00:00:00.000Z', 5001, 7)"));
   REQUIRE_FALSE(connection->executeStr("INSERT INTO licensed_users (user_name, last_sign_in, user_id, id) VALUES ('test2', '2020-04-30T00:00:00.000Z', 5002, 8)"));

   return connection;
}

std::string sessionId = "testId";

std::map<std::string, std::string> initialProps
{
   {"user_id", "7"},
   {"workbench", "rstudio"},
   {"created", "2020-04-30T00:00:00.000Z"},
   {"last_used", "2020-04-30T00:00:00.000Z"},
   {"activity_state", "idle"},
   {"label", "initial session"},
   {"launch_parameters", "test"}
};

std::set<std::string> propList{
   "user_id",
   "workbench",
   "r_version",
   "r_version_label",
   "label",
   "last_used"
};

void runTests(DBActiveSessionStorage storage)
{
   GIVEN("An initialized database")
   {
      THEN("Querying properties for non-existent session returns error, and blank data")
      {
         // Query All Properties
         std::map<std::string, std::string> nonexistentAllProps{};
         REQUIRE(storage.readProperties(sessionId, &nonexistentAllProps));
         REQUIRE(nonexistentAllProps.empty());
         
         std::map<std::string, std::string> nonexistentPropSet{};
         REQUIRE(storage.readProperties(sessionId, propList, &nonexistentPropSet));
         REQUIRE(nonexistentPropSet.empty());

         // Query single property
         std::string launchParams{};
         REQUIRE(storage.readProperty(sessionId, "launch_parameters", &launchParams));
         REQUIRE(launchParams.empty());
      }

      WHEN("Initial minimal session data is inserted")
      {
         // Initial props is the smallest set of data that can be used to insert a new session row
         REQUIRE_FALSE(storage.writeProperties(sessionId, initialProps));

         THEN("Initial data readable from db")
         {
            std::map<std::string, std::string> readProps{};

            // Read All
            REQUIRE_FALSE(storage.readProperties(sessionId, &readProps));
            REQUIRE(readProps.size() > 0);
            REQUIRE(readProps.find("user_id")->second == "7");
            REQUIRE(readProps.find("workbench")->second == "rstudio");
            REQUIRE(readProps.find("r_version")->second == "");
            REQUIRE(readProps.find("activity_state")->second == "idle");

            // Read mixed property set
            std::map<std::string, std::string> initialPropertiesSubset{};
            REQUIRE_FALSE(storage.readProperties(sessionId, propList, &initialPropertiesSubset));
            REQUIRE(propList.size() == initialPropertiesSubset.size());
            REQUIRE(initialPropertiesSubset.find("user_id")->second == "7");
            REQUIRE(initialPropertiesSubset.find("r_version")->second == "");
            
            // Read single property with properties
            std::set<std::string> propToRead{"user_id"};
            std::map<std::string, std::string> singleProp{};
            REQUIRE_FALSE(storage.readProperties(sessionId, propToRead, &singleProp));
            REQUIRE(singleProp.size() == 1);
            REQUIRE(singleProp.find("user_id")->second == "7");

            // Single Property Reads
            // extant property
            std::string workbench{};
            REQUIRE_FALSE(storage.readProperty(sessionId, "workbench", &workbench));
            REQUIRE(workbench == "rstudio");

            // missing property
            std::string rVer{};
            REQUIRE_FALSE(storage.readProperty(sessionId, "r_version", &rVer));
            REQUIRE(rVer == "");

            // Property that isn't a column
            std::string nonProp{};
            REQUIRE(storage.readProperty(sessionId, "non-existent", &nonProp));
            REQUIRE(nonProp == "");
         }
      }
   }

   GIVEN("A Prepopulated database")
   {
      REQUIRE_FALSE(storage.writeProperties(sessionId, initialProps));

      WHEN("Data is updated individually")
      {
         // Assign previously null property
         REQUIRE_FALSE(storage.writeProperty(sessionId, "r_version", "4.0.0"));
         // Update existing property
         REQUIRE_FALSE(storage.writeProperty(sessionId, "activity_state", "running"));
         REQUIRE_FALSE(storage.writeProperty(sessionId, "user_id", "8"));
         
         THEN("Changes are visible")
         {
            std::map<std::string, std::string> readProps{};

            // Read All
            REQUIRE_FALSE(storage.readProperties(sessionId, &readProps));
            REQUIRE(readProps.size() > 0);
            REQUIRE(readProps.find("user_id")->second == "8");
            REQUIRE(readProps.find("workbench")->second == "rstudio");
            REQUIRE(readProps.find("r_version")->second == "4.0.0");
            REQUIRE(readProps.find("activity_state")->second == "running");
            REQUIRE(readProps.find("r_version_label")->second == "");

            // Read mixed property set
            std::map<std::string, std::string> initialPropertiesSubset{};
            REQUIRE_FALSE(storage.readProperties(sessionId, propList, &initialPropertiesSubset));
            REQUIRE(propList.size() == initialPropertiesSubset.size());
            REQUIRE(initialPropertiesSubset.find("user_id")->second == "8");
            REQUIRE(initialPropertiesSubset.find("r_version")->second == "4.0.0");
            REQUIRE(initialPropertiesSubset.find("r_version_label")->second == "");

            // Read single property with properties
            std::set<std::string> propToRead{"user_id"};
            std::map<std::string, std::string> singleProp{};
            REQUIRE_FALSE(storage.readProperties(sessionId, propToRead, &singleProp));
            REQUIRE(singleProp.size() == 1);
            REQUIRE(singleProp.find("user_id")->second == "8");

            // Single Property Reads
            // existing property
            std::string workbench{};
            REQUIRE_FALSE(storage.readProperty(sessionId, "workbench", &workbench));
            REQUIRE(workbench == "rstudio");
            std::string rVersion{};
            REQUIRE_FALSE(storage.readProperty(sessionId, "r_version", &rVersion));
            REQUIRE(rVersion == "4.0.0");

            // missing property
            std::string rVer{};
            REQUIRE_FALSE(storage.readProperty(sessionId, "r_version_label", &rVer));
            REQUIRE(rVer == "");
         }
      }
   }

   GIVEN("Initialized Database")
   {
      WHEN("Inserting too few properties for initial insert")
      {
         std::map<std::string, std::string> tooFewProps{
            {"session_id", "test"},
            {"r_version_label", "spicy r"}
         };
         Error error = storage.writeProperties(sessionId, tooFewProps);
         THEN("Error is returned")
         {
            REQUIRE(error);
            REQUIRE(error.getCode() == errc::DBError);
         }
      }

      WHEN("Database is populated")
      {
         REQUIRE_FALSE(storage.writeProperties(sessionId, initialProps));
         THEN("Ownership cannot be transferred to user that does not exist")
         {
            Error error = storage.writeProperty(sessionId, "user_id", "10");
            REQUIRE(error);
            REQUIRE(error.getCode() == errc::DBError);
         }
      }
   }
}

TEST_CASE("Database Session Storage, Sqlite","[database][integration][session][sqlite]"){
   
   SqliteConnectionOptions options = sqliteConnectionOptions();
   boost::shared_ptr<IConnection> connection = initializeSQLiteDatabase(options);
   DBActiveSessionStorage storage{connection};
   runTests(storage);
}

TEST_CASE("Databse Session Storage, Postgres","[database][integration][session][.postgres]"){
   
   PostgresqlConnectionOptions options = postgresConnectionOptions();
   boost::shared_ptr<IConnection> connection = initializePostgresqlDatabase(options);
   DBActiveSessionStorage storage{connection};
   runTests(storage);
}
