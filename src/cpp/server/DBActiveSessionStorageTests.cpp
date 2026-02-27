#include <gtest/gtest.h>
#include <server/DBActiveSessionStorage.hpp>
#include <core/system/System.hpp>
#include <core/FileSerializer.hpp>
#include <boost/filesystem.hpp>
#include <core/Result.hpp>
#include <iostream>

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

Result<boost::shared_ptr<IConnection>> initializeSQLiteDatabase(SqliteConnectionOptions options, system::User user)
{
   // Delete the db if it exists
   FilePath dbPath(options.file);
   if (Error error = dbPath.removeIfExists())
      return Unexpected(error);

   // Establish sqlite connection
   boost::shared_ptr<IConnection> connection;
   if (Error error = connect(options, &connection))
      return Unexpected(error);

   // Execute the create db script
   FilePath workingDir = system::currentWorkingDir(system::currentProcessId());
   FilePath createDbPath = workingDir.completeChildPath("db/CreateTables.sqlite");
   std::string createDbStr;
   if (Error error = readStringFromFile(createDbPath, &createDbStr))
      return Unexpected(error);
   if (Error error = connection->executeStr(createDbStr))
      return Unexpected(error);

   // Insert a user to own the sessions
   if (Error error = connection->executeStr("INSERT INTO licensed_users (user_name, last_sign_in, user_id, id) VALUES ('"+user.getUsername()+"', '2020-04-30T00:00:00.000Z', "+std::to_string(user.getUserId())+", 7)"))
      return Unexpected(error);
   if (Error error = connection->executeStr("INSERT INTO licensed_users (user_name, last_sign_in, user_id, id) VALUES ('test2', '2020-04-30T00:00:00.000Z', 5002, 8)"))
      return Unexpected(error);
   
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

Result<boost::shared_ptr<IConnection>> initializePostgresqlDatabase(PostgresqlConnectionOptions options, system::User user)
{
   // Establish the connection
   boost::shared_ptr<IConnection> connection;
   if (Error error = connect(options, &connection))
      return Unexpected(error);

   // Drop all database info in our test database
   connection->executeStr("DROP SCHEMA public CASCADE;");
   connection->executeStr("CREATE SCHEMA public;");
   connection->executeStr("GRANT ALL ON SCHEMA public TO "+options.username+";");
   connection->executeStr("GRANT ALL ON SCHEMA public TO public");

   // Execute the create db script
   FilePath workingDir = system::currentWorkingDir(system::currentProcessId());
   FilePath createDbPath = workingDir.completeChildPath("db/CreateTables.postgresql");
   std::string createDbStr;
   if (Error error = readStringFromFile(createDbPath, &createDbStr))
      return Unexpected(error);
   connection->executeStr("BEGIN TRANSACTION");
   if (Error error = connection->executeStr(createDbStr))
      return Unexpected(error);
   connection->executeStr("COMMIT");

   // Insert a user to own the sessions
   if (Error error = connection->executeStr("INSERT INTO licensed_users (user_name, last_sign_in, user_id, id) VALUES ('"+user.getUsername()+"', '2020-04-30T00:00:00.000Z', "+std::to_string(user.getUserId())+", 7)"))
      return Unexpected(error);
   if (Error error = connection->executeStr("INSERT INTO licensed_users (user_name, last_sign_in, user_id, id) VALUES ('test2', '2020-04-30T00:00:00.000Z', 5002, 8)"))
      return Unexpected(error);

   return connection;
}

std::string sessionId = "testId";

std::map<std::string, std::string> initialProps
{
   {"editor", "RStudio"},
   {"created", "2020-04-30T00:00:00.000Z"},
   {"last_used", "2020-04-30T00:00:00.000Z"},
   {"activity_state", "launching"},
   {"label", "initial session"},
   {"launch_parameters", "{}"}
};

std::set<std::string> propList{
   "user_id",
   "editor",
   "r_version",
   "r_version_label",
   "label",
   "last_used"
};

class DBActiveSessionStorageTestFixture : public ::testing::Test
{
protected:
   system::User currentUser;
   std::string sessionId = "testId";
   boost::shared_ptr<IConnection> connection;
   std::unique_ptr<DBActiveSessionStorage> storage;
   
   void SetUp() override
   {
      Error error = system::User::getCurrentUser(currentUser);
      ASSERT_FALSE(error);
   }
   
   virtual void InitializeDatabase() = 0;
   
   void InitializeStorage()
   {
      storage = std::make_unique<DBActiveSessionStorage>(sessionId, currentUser, connection);
   }
};

class SqliteDBActiveSessionStorageTest : public DBActiveSessionStorageTestFixture
{
protected:
   void InitializeDatabase() override
   {
      SqliteConnectionOptions options = sqliteConnectionOptions();
      auto connectionResult = initializeSQLiteDatabase(options, currentUser);
      ASSERT_TRUE(connectionResult) << "SQLite initialization failed: " << connectionResult.error().asString();
      connection = connectionResult.value();
      InitializeStorage();
   }
   
   void SetUp() override
   {
      DBActiveSessionStorageTestFixture::SetUp();
      InitializeDatabase();
   }
};

class PostgresDBActiveSessionStorageTest : public DBActiveSessionStorageTestFixture
{
protected:
   void InitializeDatabase() override
   {
      // Skip if POSTGRES_ENABLED is not set
      if (!std::getenv("POSTGRES_ENABLED") || std::string(std::getenv("POSTGRES_ENABLED")) != "1")
      {
         GTEST_SKIP() << "PostgreSQL tests skipped because POSTGRES_ENABLED is not set to '1'.";
      }
      
      PostgresqlConnectionOptions options = postgresConnectionOptions();
      auto connectionResult = initializePostgresqlDatabase(options, currentUser);
      ASSERT_TRUE(connectionResult) << "PostgreSQL initialization failed: " << connectionResult.error().asString();
      connection = connectionResult.value();
      InitializeStorage();
   }
   
   void SetUp() override
   {
      DBActiveSessionStorageTestFixture::SetUp();
      InitializeDatabase();
   }
};

// Test querying properties for a non-existent session
TEST_F(SqliteDBActiveSessionStorageTest, NonexistentSessionReturnsError)
{
   // GIVEN: An initialized database
   
   // THEN: Querying properties for non-existent session returns error, and blank data
   // Query All Properties
   std::map<std::string, std::string> nonexistentAllProps{};
   ASSERT_TRUE(storage->readProperties(&nonexistentAllProps));
   ASSERT_TRUE(nonexistentAllProps.empty());
   
   std::map<std::string, std::string> nonexistentPropSet{};
   ASSERT_TRUE(storage->readProperties(propList, &nonexistentPropSet));
   ASSERT_TRUE(nonexistentPropSet.empty());

   // Query single property
   std::string launchParams{};
   ASSERT_TRUE(storage->readProperty("launch_parameters", &launchParams));
   ASSERT_TRUE(launchParams.empty());
}

// Test inserting initial minimal session data
TEST_F(SqliteDBActiveSessionStorageTest, InitialMinimalSessionDataInserted)
{
   // GIVEN: An initialized database

   // WHEN: Initial minimal session data is inserted
   // Initial props is the smallest set of data that can be used to insert a new session row
   Error writeError = storage->writeProperties(initialProps);
   ASSERT_FALSE(writeError) << "writeProperties failed: " << writeError.asString();

   // THEN: Initial data readable from db
   std::map<std::string, std::string> readProps{};

   // Read All
   Error readError = storage->readProperties(&readProps);
   ASSERT_FALSE(readError) << "readProperties (all) failed: " << readError.asString();
   ASSERT_TRUE(readProps.size() > 0);
   ASSERT_EQ(readProps.find("user_id")->second, "7");
   ASSERT_EQ(readProps.find("editor")->second, "RStudio");
   ASSERT_EQ(readProps.find("r_version")->second, "");
   ASSERT_EQ(readProps.find("activity_state")->second, "launching");

   // Read mixed property set
   std::map<std::string, std::string> initialPropsSubset{};
   ASSERT_FALSE(storage->readProperties(propList, &initialPropsSubset));
   ASSERT_EQ(initialPropsSubset.size(), propList.size());
   ASSERT_EQ(initialPropsSubset.find("user_id")->second, "7");
   ASSERT_EQ(initialPropsSubset.find("r_version")->second, "");
   
   // Read single property with properties
   std::set<std::string> initialPropToRead{"user_id"};
   std::map<std::string, std::string> initialSingleProp{};
   ASSERT_FALSE(storage->readProperties(initialPropToRead, &initialSingleProp));
   ASSERT_EQ(initialSingleProp.size(), 1u);
   ASSERT_EQ(initialSingleProp.find("user_id")->second, "7");

   // Single Property Reads
   // extant property
   std::string initialEditor{};
   ASSERT_FALSE(storage->readProperty("editor", &initialEditor));
   ASSERT_EQ(initialEditor, "RStudio");

   // missing property
   std::string initialRVer{};
   ASSERT_FALSE(storage->readProperty("r_version", &initialRVer));
   ASSERT_EQ(initialRVer, "");

   // Property that isn't a column
   std::string nonProp{};
   ASSERT_TRUE(storage->readProperty("non-existent", &nonProp));
   ASSERT_EQ(nonProp, "");
}

// Test updating session data individually
TEST_F(SqliteDBActiveSessionStorageTest, DataUpdatedIndividually)
{
   // GIVEN: A prepopulated database
   ASSERT_FALSE(storage->writeProperties(initialProps));

   // WHEN: Data is updated individually
   // Assign previously null property
   ASSERT_FALSE(storage->writeProperty("r_version", "4.0.0"));
   // Update existing property
   ASSERT_FALSE(storage->writeProperty("activity_state", "running"));
   ASSERT_FALSE(storage->writeProperty("user_id", "8"));
   
   // THEN: Changes are visible
   std::map<std::string, std::string> updatedProps{};

   // Read All
   ASSERT_FALSE(storage->readProperties(&updatedProps));
   ASSERT_TRUE(updatedProps.size() > 0);
   ASSERT_EQ(updatedProps.find("user_id")->second, "8");
   ASSERT_EQ(updatedProps.find("editor")->second, "RStudio");
   ASSERT_EQ(updatedProps.find("r_version")->second, "4.0.0");
   ASSERT_EQ(updatedProps.find("activity_state")->second, "running");
   ASSERT_EQ(updatedProps.find("r_version_label")->second, "");

   // Read mixed property set
   std::map<std::string, std::string> updatedPropsSubset{};
   ASSERT_FALSE(storage->readProperties(propList, &updatedPropsSubset));
   ASSERT_EQ(updatedPropsSubset.size(), propList.size());
   ASSERT_EQ(updatedPropsSubset.find("user_id")->second, "8");
   ASSERT_EQ(updatedPropsSubset.find("r_version")->second, "4.0.0");
   ASSERT_EQ(updatedPropsSubset.find("r_version_label")->second, "");

   // Read single property with properties
   std::set<std::string> updatedPropToRead{"user_id"};
   std::map<std::string, std::string> updatedSingleProp{};
   ASSERT_FALSE(storage->readProperties(updatedPropToRead, &updatedSingleProp));
   ASSERT_EQ(updatedSingleProp.size(), 1u);
   ASSERT_EQ(updatedSingleProp.find("user_id")->second, "8");

   // Single Property Reads
   // existing property
   std::string updatedEditor{};
   ASSERT_FALSE(storage->readProperty("editor", &updatedEditor));
   ASSERT_EQ(updatedEditor, "RStudio");
   std::string rVersion{};
   ASSERT_FALSE(storage->readProperty("r_version", &rVersion));
   ASSERT_EQ(rVersion, "4.0.0");

   // missing property
   std::string updatedRVer{};
   ASSERT_FALSE(storage->readProperty("r_version_label", &updatedRVer));
   ASSERT_EQ(updatedRVer, "");
}

// Test inserting too few properties for initial insert
TEST_F(SqliteDBActiveSessionStorageTest, InsertingTooFewPropertiesReturnsError)
{
   // GIVEN: Initialized Database
   
   // WHEN: Inserting too few properties for initial insert
   std::map<std::string, std::string> tooFewProps{
      {"session_id", "test"},
      {"r_version_label", "spicy r"}
   };
   Error error = storage->writeProperties(tooFewProps);
   
   // THEN: Error is returned
   ASSERT_TRUE(error);
   ASSERT_EQ(error.getCode(), errc::DBError);
}

// Test attempting to transfer ownership to non-existent user
TEST_F(SqliteDBActiveSessionStorageTest, OwnershipCannotBeTransferredToNonExistentUser)
{
   // GIVEN: Database is populated
   ASSERT_FALSE(storage->writeProperties(initialProps));
   
   // WHEN: Attempting to transfer ownership to user that does not exist
   Error error = storage->writeProperty("user_id", "10");
   
   // THEN: Error is returned
   ASSERT_TRUE(error);
   ASSERT_EQ(error.getCode(), errc::DBError);
}

// Test querying properties for a non-existent session with PostgreSQL
TEST_F(PostgresDBActiveSessionStorageTest, NonexistentSessionReturnsError)
{
   // GIVEN: An initialized database
   
   // THEN: Querying properties for non-existent session returns error, and blank data
   // Query All Properties
   std::map<std::string, std::string> nonexistentAllProps{};
   ASSERT_TRUE(storage->readProperties(&nonexistentAllProps));
   ASSERT_TRUE(nonexistentAllProps.empty());
   
   std::map<std::string, std::string> nonexistentPropSet{};
   ASSERT_TRUE(storage->readProperties(propList, &nonexistentPropSet));
   ASSERT_TRUE(nonexistentPropSet.empty());

   // Query single property
   std::string launchParams{};
   ASSERT_TRUE(storage->readProperty("launch_parameters", &launchParams));
   ASSERT_TRUE(launchParams.empty());
}

// Test inserting initial minimal session data with PostgreSQL
TEST_F(PostgresDBActiveSessionStorageTest, InitialMinimalSessionDataInserted)
{
   // GIVEN: An initialized database

   // WHEN: Initial minimal session data is inserted
   // Initial props is the smallest set of data that can be used to insert a new session row
   ASSERT_FALSE(storage->writeProperties(initialProps));

   // THEN: Initial data readable from db
   std::map<std::string, std::string> readProps{};

   // Read All
   ASSERT_FALSE(storage->readProperties(&readProps));
   ASSERT_TRUE(readProps.size() > 0);
   ASSERT_EQ(readProps.find("user_id")->second, "7");
   ASSERT_EQ(readProps.find("editor")->second, "RStudio");
   ASSERT_EQ(readProps.find("r_version")->second, "");
   ASSERT_EQ(readProps.find("activity_state")->second, "launching");

   // Read mixed property set
   std::map<std::string, std::string> pgInitialPropsSubset{};
   ASSERT_FALSE(storage->readProperties(propList, &pgInitialPropsSubset));
   ASSERT_EQ(pgInitialPropsSubset.size(), propList.size());
   ASSERT_EQ(pgInitialPropsSubset.find("user_id")->second, "7");
   ASSERT_EQ(pgInitialPropsSubset.find("r_version")->second, "");
   
   // Read single property with properties
   std::set<std::string> pgInitialPropToRead{"user_id"};
   std::map<std::string, std::string> pgInitialSingleProp{};
   ASSERT_FALSE(storage->readProperties(pgInitialPropToRead, &pgInitialSingleProp));
   ASSERT_EQ(pgInitialSingleProp.size(), 1u);
   ASSERT_EQ(pgInitialSingleProp.find("user_id")->second, "7");

   // Single Property Reads
   // extant property
   std::string pgInitialEditor{};
   ASSERT_FALSE(storage->readProperty("editor", &pgInitialEditor));
   ASSERT_EQ(pgInitialEditor, "RStudio");

   // missing property
   std::string pgInitialRVer{};
   ASSERT_FALSE(storage->readProperty("r_version", &pgInitialRVer));
   ASSERT_EQ(pgInitialRVer, "");

   // Property that isn't a column
   std::string pgNonProp{};
   ASSERT_TRUE(storage->readProperty("non-existent", &pgNonProp));
   ASSERT_EQ(pgNonProp, "");
}

// Test updating session data individually with PostgreSQL
TEST_F(PostgresDBActiveSessionStorageTest, DataUpdatedIndividually)
{
   // GIVEN: A prepopulated database
   ASSERT_FALSE(storage->writeProperties(initialProps));

   // WHEN: Data is updated individually
   // Assign previously null property
   ASSERT_FALSE(storage->writeProperty("r_version", "4.0.0"));
   // Update existing property
   ASSERT_FALSE(storage->writeProperty("activity_state", "running"));
   ASSERT_FALSE(storage->writeProperty("user_id", "8"));
   
   // THEN: Changes are visible
   std::map<std::string, std::string> updatedProps{};

   // Read All
   ASSERT_FALSE(storage->readProperties(&updatedProps));
   ASSERT_TRUE(updatedProps.size() > 0);
   ASSERT_EQ(updatedProps.find("user_id")->second, "8");
   ASSERT_EQ(updatedProps.find("editor")->second, "RStudio");
   ASSERT_EQ(updatedProps.find("r_version")->second, "4.0.0");
   ASSERT_EQ(updatedProps.find("activity_state")->second, "running");
   ASSERT_EQ(updatedProps.find("r_version_label")->second, "");

   // Read mixed property set
   std::map<std::string, std::string> pgUpdatedPropsSubset{};
   ASSERT_FALSE(storage->readProperties(propList, &pgUpdatedPropsSubset));
   ASSERT_EQ(pgUpdatedPropsSubset.size(), propList.size());
   ASSERT_EQ(pgUpdatedPropsSubset.find("user_id")->second, "8");
   ASSERT_EQ(pgUpdatedPropsSubset.find("r_version")->second, "4.0.0");
   ASSERT_EQ(pgUpdatedPropsSubset.find("r_version_label")->second, "");

   // Read single property with properties
   std::set<std::string> pgUpdatedPropToRead{"user_id"};
   std::map<std::string, std::string> pgUpdatedSingleProp{};
   ASSERT_FALSE(storage->readProperties(pgUpdatedPropToRead, &pgUpdatedSingleProp));
   ASSERT_EQ(pgUpdatedSingleProp.size(), 1u);
   ASSERT_EQ(pgUpdatedSingleProp.find("user_id")->second, "8");

   // Single Property Reads
   // existing property
   std::string pgUpdatedEditor{};
   ASSERT_FALSE(storage->readProperty("editor", &pgUpdatedEditor));
   ASSERT_EQ(pgUpdatedEditor, "RStudio");
   std::string pgRVersion{};
   ASSERT_FALSE(storage->readProperty("r_version", &pgRVersion));
   ASSERT_EQ(pgRVersion, "4.0.0");

   // missing property
   std::string pgUpdatedRVer{};
   ASSERT_FALSE(storage->readProperty("r_version_label", &pgUpdatedRVer));
   ASSERT_EQ(pgUpdatedRVer, "");
}

// Test inserting too few properties for initial insert with PostgreSQL
TEST_F(PostgresDBActiveSessionStorageTest, InsertingTooFewPropertiesReturnsError)
{
   // GIVEN: Initialized Database
   
   // WHEN: Inserting too few properties for initial insert
   std::map<std::string, std::string> tooFewProps{
      {"session_id", "test"},
      {"r_version_label", "spicy r"}
   };
   Error error = storage->writeProperties(tooFewProps);
   
   // THEN: Error is returned
   ASSERT_TRUE(error);
   ASSERT_EQ(error.getCode(), errc::DBError);
}

// Test attempting to transfer ownership to non-existent user with PostgreSQL
TEST_F(PostgresDBActiveSessionStorageTest, OwnershipCannotBeTransferredToNonExistentUser)
{
   // GIVEN: Database is populated
   ASSERT_FALSE(storage->writeProperties(initialProps));
   
   // WHEN: Attempting to transfer ownership to user that does not exist
   Error error = storage->writeProperty("user_id", "10");
   
   // THEN: Error is returned
   ASSERT_TRUE(error);
   ASSERT_EQ(error.getCode(), errc::DBError);
}

// ===== isEmpty / isValid tests =====

// Test isEmpty returns true for non-existent session
TEST_F(SqliteDBActiveSessionStorageTest, IsEmptyReturnsTrueForNonexistentSession)
{
   // GIVEN: An initialized database with no session rows
   
   // WHEN: Checking if the session is empty
   bool isEmpty = false;
   Error error = storage->isEmpty(&isEmpty);
   
   // THEN: No error and isEmpty is true
   ASSERT_FALSE(error) << "isEmpty failed: " << error.asString();
   ASSERT_TRUE(isEmpty);
}

// Test isEmpty returns false for existing session
TEST_F(SqliteDBActiveSessionStorageTest, IsEmptyReturnsFalseForExistingSession)
{
   // GIVEN: A session has been inserted
   ASSERT_FALSE(storage->writeProperties(initialProps));
   
   // WHEN: Checking if the session is empty
   bool isEmpty = true;
   Error error = storage->isEmpty(&isEmpty);
   
   // THEN: No error and isEmpty is false
   ASSERT_FALSE(error) << "isEmpty failed: " << error.asString();
   ASSERT_FALSE(isEmpty);
}

// Test isValid returns false for non-existent session
TEST_F(SqliteDBActiveSessionStorageTest, IsValidReturnsFalseForNonexistentSession)
{
   // GIVEN: An initialized database with no session rows
   
   // WHEN: Checking if the session is valid
   bool isValid = true;
   Error error = storage->isValid(&isValid);
   
   // THEN: No error and isValid is false
   ASSERT_FALSE(error) << "isValid failed: " << error.asString();
   ASSERT_FALSE(isValid);
}

// Test isValid returns false for R session without project set
TEST_F(SqliteDBActiveSessionStorageTest, IsValidReturnsFalseForRSessionWithoutProject)
{
   // GIVEN: An R session with no project property set (editor = "RStudio")
   ASSERT_FALSE(storage->writeProperties(initialProps));
   
   // WHEN: Checking if the session is valid
   bool isValid = true;
   Error error = storage->isValid(&isValid);
   
   // THEN: No error and isValid is false (R sessions require a project)
   ASSERT_FALSE(error) << "isValid failed: " << error.asString();
   ASSERT_FALSE(isValid);
}

// Test isValid returns true for R session with project set
TEST_F(SqliteDBActiveSessionStorageTest, IsValidReturnsTrueForRSessionWithProject)
{
   // GIVEN: An R session with project property set
   ASSERT_FALSE(storage->writeProperties(initialProps));
   ASSERT_FALSE(storage->writeProperty("project", "none"));
   
   // WHEN: Checking if the session is valid
   bool isValid = false;
   Error error = storage->isValid(&isValid);
   
   // THEN: No error and isValid is true
   ASSERT_FALSE(error) << "isValid failed: " << error.asString();
   ASSERT_TRUE(isValid);
}

// Test isValid returns true for VS Code session (non-R) without project
TEST_F(SqliteDBActiveSessionStorageTest, IsValidReturnsTrueForVSCodeSession)
{
   // GIVEN: A VS Code session (no project required)
   std::map<std::string, std::string> vscodeProps = initialProps;
   vscodeProps["editor"] = "VSCode";
   ASSERT_FALSE(storage->writeProperties(vscodeProps));
   
   // WHEN: Checking if the session is valid
   bool isValid = false;
   Error error = storage->isValid(&isValid);
   
   // THEN: No error and isValid is true (VS Code doesn't need project)
   ASSERT_FALSE(error) << "isValid failed: " << error.asString();
   ASSERT_TRUE(isValid);
}

// Test clearScratchPath succeeds for DB storage (no-op)
TEST_F(SqliteDBActiveSessionStorageTest, ClearScratchPathSucceeds)
{
   // GIVEN: A session has been inserted
   ASSERT_FALSE(storage->writeProperties(initialProps));
   
   // WHEN: Clearing the scratch path
   Error error = storage->clearScratchPath();
   
   // THEN: No error (DB storage has no scratch path to clear)
   ASSERT_FALSE(error) << "clearScratchPath failed: " << error.asString();
   
   // AND: Session is still present in the DB
   bool isEmpty = true;
   error = storage->isEmpty(&isEmpty);
   ASSERT_FALSE(error);
   ASSERT_FALSE(isEmpty);
}

// Test destroy removes the session from the DB
TEST_F(SqliteDBActiveSessionStorageTest, DestroyRemovesSession)
{
   // GIVEN: A session has been inserted
   ASSERT_FALSE(storage->writeProperties(initialProps));
   bool isEmpty = true;
   ASSERT_FALSE(storage->isEmpty(&isEmpty));
   ASSERT_FALSE(isEmpty);
   
   // WHEN: Destroying the session
   Error error = storage->destroy();
   ASSERT_FALSE(error) << "destroy failed: " << error.asString();
   
   // THEN: Session is no longer in the DB
   isEmpty = false;
   ASSERT_FALSE(storage->isEmpty(&isEmpty));
   ASSERT_TRUE(isEmpty);
}

// PostgreSQL equivalents

TEST_F(PostgresDBActiveSessionStorageTest, IsEmptyReturnsTrueForNonexistentSession)
{
   bool isEmpty = false;
   Error error = storage->isEmpty(&isEmpty);
   ASSERT_FALSE(error) << "isEmpty failed: " << error.asString();
   ASSERT_TRUE(isEmpty);
}

TEST_F(PostgresDBActiveSessionStorageTest, IsEmptyReturnsFalseForExistingSession)
{
   ASSERT_FALSE(storage->writeProperties(initialProps));
   bool isEmpty = true;
   Error error = storage->isEmpty(&isEmpty);
   ASSERT_FALSE(error) << "isEmpty failed: " << error.asString();
   ASSERT_FALSE(isEmpty);
}

TEST_F(PostgresDBActiveSessionStorageTest, IsValidReturnsFalseForNonexistentSession)
{
   bool isValid = true;
   Error error = storage->isValid(&isValid);
   ASSERT_FALSE(error) << "isValid failed: " << error.asString();
   ASSERT_FALSE(isValid);
}

TEST_F(PostgresDBActiveSessionStorageTest, IsValidReturnsTrueForRSessionWithProject)
{
   ASSERT_FALSE(storage->writeProperties(initialProps));
   ASSERT_FALSE(storage->writeProperty("project", "none"));
   bool isValid = false;
   Error error = storage->isValid(&isValid);
   ASSERT_FALSE(error) << "isValid failed: " << error.asString();
   ASSERT_TRUE(isValid);
}

TEST_F(PostgresDBActiveSessionStorageTest, IsValidReturnsTrueForVSCodeSession)
{
   std::map<std::string, std::string> vscodeProps = initialProps;
   vscodeProps["editor"] = "VSCode";
   ASSERT_FALSE(storage->writeProperties(vscodeProps));
   bool isValid = false;
   Error error = storage->isValid(&isValid);
   ASSERT_FALSE(error) << "isValid failed: " << error.asString();
   ASSERT_TRUE(isValid);
}
