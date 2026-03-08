/*
 * DatabaseTests.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <gtest/gtest.h>

#include <core/Database.hpp>
#include <core/FileSerializer.hpp>
#include <core/system/System.hpp>
#include <shared_core/SafeConvert.hpp>

#include <soci/session.h>
#include <soci/sqlite3/soci-sqlite3.h>

#include "config.h"

namespace rstudio {
namespace core {
namespace tests {

using namespace core;
using namespace core::database;

// Shared helper logic (keeps fixtures DRY)
namespace {
Error initializeCommonTestSchema(IConnection& connection)
{
   Query query = connection.query("create table Test(id int, text varchar(255))");
   Error error = connection.execute(query);
   return error;
}
} // anonymous namespace

class DatabaseTestsFixture : public ::testing::Test
{
protected:
   boost::shared_ptr<IConnection> sqliteConnection;
   FilePath dbPath;

   void SetUp() override
   {
      dbPath = FilePath("/tmp/rstudio-test-db");
      dbPath.removeIfExists();

      SqliteConnectionOptions options;
      options.file = dbPath.getAbsolutePath();
      Error error = connect(options, &sqliteConnection);
      ASSERT_FALSE(error) << "Failed to create SQLite test database in fixture: " << error.getMessage();

      error = initializeCommonTestSchema(*sqliteConnection);
      ASSERT_FALSE(error) << "Failed to initialize common test schema: " << error.getMessage();
   }

   void TearDown() override
   {
      sqliteConnection.reset();
      dbPath.removeIfExists();
   }
};

core::database::PostgresqlConnectionOptions postgresConnectionOptions()
{
   PostgresqlConnectionOptions options;
   options.connectionTimeoutSeconds = 10;
   options.database = "rstudio_test";
   options.host = "localhost";
   options.username = "postgres";
   const char* dbPass = std::getenv("RSTUDIO_TEST_DB_PASS");
   options.password = (dbPass) ? dbPass : "postgres";

   return options;
}

TEST_F(DatabaseTestsFixture, CanCreateSqliteDatabase)
{
   int id = 10;
   std::string text = "Hello, database!";

   Query query = sqliteConnection->query("insert into Test(id, text) values(:id, :text)")
          .withInput(id)
          .withInput(text);
   Error error = sqliteConnection->execute(query);
   ASSERT_FALSE(error) << "Failed to insert values into Test table: " << error.getMessage();

   int rowId;
   std::string rowText;
   query = sqliteConnection->query("select id, text from Test where id = (:id)")
          .withInput(id)
          .withOutput(rowId)
          .withOutput(rowText);
   error = sqliteConnection->execute(query);
   ASSERT_FALSE(error) << "Failed to select from Test table: " << error.getMessage();

   EXPECT_EQ(id, rowId);
   EXPECT_EQ(text, rowText);
}

TEST(DatabaseTest, CanCreatePostgresqlDatabase)
{
   if (!std::getenv("POSTGRES_ENABLED") || std::string(std::getenv("POSTGRES_ENABLED")) != "1")
   {
      GTEST_SKIP() << "Skipping Postgres migration tests as POSTGRES_ENABLED is not set";
   }

   boost::shared_ptr<IConnection> connection;
   Error error = connect(postgresConnectionOptions(), &connection);
   ASSERT_FALSE(error) << "Failed to connect to PostgreSQL database: " << error.getMessage();

   Query query = connection->query("create table Test(id int, text varchar(255))");
   error = connection->execute(query);
   ASSERT_FALSE(error) << "Failed to create Test table in PostgreSQL: " << error.getMessage();

   int id = 10;
   std::string text = "Hello, database!";

   query = connection->query("insert into Test(id, text) values(:id, :text)")
         .withInput(id)
         .withInput(text);
   error = connection->execute(query);
   ASSERT_FALSE(error) << "Failed to insert values into PostgreSQL Test table: " << error.getMessage();

   int rowId;
   std::string rowText;
   query = connection->query("select id, text from Test where id = (:id)")
         .withInput(id)
         .withOutput(rowId)
         .withOutput(rowText);
   error = connection->execute(query);
   ASSERT_FALSE(error) << "Failed to select from PostgreSQL Test table: " << error.getMessage();

   EXPECT_EQ(id, rowId);
   EXPECT_EQ(text, rowText);
}

TEST_F(DatabaseTestsFixture, CanPerformTransactions)
{
   Transaction transaction(sqliteConnection);
   bool dataReturned = false;

   // verify that we can commit a transaction
   Query query = sqliteConnection->query("insert into Test(id, text) values(:id, :text)");
   for (int id = 0; id < 100; ++id)
   {
      std::string text = "Test text " + core::safe_convert::numberToString(id);
      query.withInput(id).withInput(text);

      Error error = sqliteConnection->execute(query);
      ASSERT_FALSE(error) << "Failed to insert row " << id << ": " << error.getMessage();
   }
   transaction.commit();

   int rowId;
   std::string rowText;
   query = sqliteConnection->query("select id, text from Test where id = 50")
      .withOutput(rowId)
      .withOutput(rowText);

   Error error = sqliteConnection->execute(query, &dataReturned);
   ASSERT_FALSE(error) << "Failed to select test row: " << error.getMessage();
   ASSERT_TRUE(dataReturned) << "Expected data to be returned but none was";
   ASSERT_EQ(50, rowId);
   ASSERT_EQ(std::string("Test text 50"), rowText);

   // now attempt to rollback a transaction
   Transaction transaction2(sqliteConnection);
   query = sqliteConnection->query("insert into Test(id, text) values(:id, :text)");
   for (int id = 100; id < 200; ++id)
   {
      std::string text = "Test text " + core::safe_convert::numberToString(id);
      query.withInput(id).withInput(text);

      Error error = sqliteConnection->execute(query);
      ASSERT_FALSE(error) << "Failed to insert row " << id << " (for rollback): " << error.getMessage();
   }
   transaction2.rollback();

   query = sqliteConnection->query("select id, text from Test where id = 150")
      .withOutput(rowId)
      .withOutput(rowText);

   // expect no data
   error = sqliteConnection->execute(query, &dataReturned);
   ASSERT_FALSE(error) << "Failed to select test row after rollback: " << error.getMessage();
   ASSERT_FALSE(dataReturned) << "Expected no data after rollback, but data was returned";
}

TEST_F(DatabaseTestsFixture, CanBulkSelect)
{
   Rowset rows;
   Query query = sqliteConnection->query("select id, text from Test where id >= 50 and id <= 100 order by id asc");
   Error error = sqliteConnection->execute(query, rows);
   ASSERT_FALSE(error) << "Failed to execute bulk select query: " << error.getMessage();

   int i = 0;
   for (RowsetIterator it = rows.begin(); it != rows.end(); ++it)
   {
      Row& row = *it;
      ASSERT_EQ(i + 50, row.get<int>(0));
      ASSERT_EQ(std::string("Test text ") + safe_convert::numberToString(i + 50), row.get<std::string>(1));
      ++i;
   }
}

TEST_F(DatabaseTestsFixture, CanBulkInsert)
{
   std::vector<int> rowIds {1000, 2000, 3000, 4000, 5000};
   std::vector<std::string> rowTexts {"1000", "2000", "3000", "4000", "5000"};

   Query query = sqliteConnection->query("insert into Test values (:id, :txt)")
         .withInput(rowIds)
         .withInput(rowTexts);
   Error error = sqliteConnection->execute(query);
   ASSERT_FALSE(error) << "Failed to execute bulk insert query: " << error.getMessage();

   Query selectQuery = sqliteConnection->query("select * from Test where id >= 1000 order by id asc");

   Rowset rowset;
   error = sqliteConnection->execute(selectQuery, rowset);
   ASSERT_FALSE(error) << "Failed to select bulk inserted data: " << error.getMessage();

   int i = 1;
   for (RowsetIterator it = rowset.begin(); it != rowset.end(); ++it)
   {
      Row& row = *it;
      ASSERT_EQ(i * 1000, row.get<int>(0));
      ASSERT_EQ(safe_convert::numberToString(i * 1000), row.get<std::string>(1));
      ++i;
   }
}

TEST(DatabaseTest, CanUseConnectionPool)
{
   FilePath dbPath = FilePath("/tmp/rstudio-test-db-pool");
   dbPath.removeIfExists();

   SqliteConnectionOptions options;
   options.file = dbPath.getAbsolutePath();

   boost::shared_ptr<ConnectionPool> connectionPool;
   Error error = createConnectionPool(5, options, &connectionPool);
   ASSERT_FALSE(error) << "Failed to create connection pool: " << error.getMessage();
   boost::shared_ptr<IConnection> connection = connectionPool->getConnection();
   ASSERT_TRUE(connection) << "Failed to get connection from pool";

   Query query = connection->query("create table Test(id int, text varchar(255))");
   error = connection->execute(query);
   ASSERT_FALSE(error) << "Failed to create Test table in fixture: " << error.getMessage();

   // Insert some data into the Test table
   for (int id = 0; id < 100; ++id)
   {
      std::string text = "Test text " + core::safe_convert::numberToString(id);
      query = connection->query("insert into Test(id, text) values(:id, :text)")
            .withInput(id)
            .withInput(text);
      error = connection->execute(query);
      ASSERT_FALSE(error) << "Failed to insert row " << id << ": " << error.getMessage();
   }

   int rowId;
   std::string rowText;
   query = connection->query("select id, text from Test where id = 50")
      .withOutput(rowId)
      .withOutput(rowText);

   bool dataReturned = false;
   error = connection->execute(query, &dataReturned);
   ASSERT_FALSE(error) << "Failed to execute query from pool connection: " << error.getMessage();
   ASSERT_TRUE(dataReturned) << "Expected data to be returned but none was";

   boost::shared_ptr<IConnection> connection2 = connectionPool->getConnection();
   ASSERT_TRUE(connection2) << "Failed to get second connection from pool";

   Query query2 = connection2->query("select id, text from Test where id = 25")
      .withOutput(rowId)
      .withOutput(rowText);

   dataReturned = false;
   error = connection2->execute(query2, &dataReturned);
   ASSERT_FALSE(error) << "Failed to execute query from second pool connection: " << error.getMessage();
   ASSERT_TRUE(dataReturned) << "Expected data to be returned from second query but none was";

   dbPath.removeIfExists();
}

TEST_F(DatabaseTestsFixture, CanUpdateSchemas)
{
   if (!std::getenv("POSTGRES_ENABLED") || std::string(std::getenv("POSTGRES_ENABLED")) != "1")
   {
      GTEST_SKIP() << "Skipping Postgres migration tests as POSTGRES_ENABLED is not set";
   }
   Error error;
   // generate some schema files
   std::string schema1 =
      R""(
      CREATE TABLE TestTable1_Persons(
         id int NOT NULL,
         first_name varchar(255),
         last_name varchar(255) NOT NULL,
         email_address varchar(255)
      );

      CREATE TABLE TestTable2_AccountHolders(
         id int,
         fk_person_id int
      );
      )"";

   // sqlite cannot alter tables very well, so adding constraints necessitates dropping
   // and re-creating the tables
   std::string schema2Sqlite =
      R""(
      CREATE TABLE TestTable1_Persons_new(
         id int NOT NULL,
         first_name varchar(255),
         last_name varchar(255),
         email_address varchar(255),
         PRIMARY KEY (id)
      );

      DROP TABLE TestTable1_Persons;
      ALTER TABLE TestTable1_Persons_new RENAME TO TestTable1_Persons;

      CREATE TABLE TestTable2_AccountHolders_new(
         id int,
         fk_person_id int,
         PRIMARY KEY (id),
         FOREIGN KEY (fk_person_id) REFERENCES TestTable1_Persons(id)
      );

      DROP TABLE TestTable2_AccountHolders;
      ALTER TABLE TestTable2_AccountHolders_new RENAME TO TestTable2_AccountHolders;
      )"";

   // postgresql supports modification of tables
   std::string schema2Postgresql =
      R""(
      ALTER TABLE TestTable1_Persons
      ADD PRIMARY KEY (id);

      ALTER TABLE TestTable2_AccountHolders
      ADD PRIMARY KEY (id);

      ALTER TABLE TestTable2_AccountHolders
      ADD FOREIGN KEY (fk_person_id) REFERENCES TestTable1_Persons(id);
      )"";

   std::string schema3Sqlite =
      R""(
      CREATE TABLE TestTable2_AccountHolders_new(
         id int,
         fk_person_id int,
         creation_time text,
         PRIMARY KEY (id),
         FOREIGN KEY (fk_person_id) REFERENCES TestTable1_Persons(id)
      );

      DROP TABLE TestTable2_AccountHolders;
      ALTER TABLE TestTable2_AccountHolders_new RENAME TO TestTable2_AccountHolders;
      )"";

   std::string schema3Postgresql =
      R""(
      ALTER TABLE TestTable2_AccountHolders
      ADD COLUMN creation_time text;
      )"";

   FilePath workingDir = core::system::currentWorkingDir(core::system::currentProcessId());
   FilePath outFile1 = workingDir.completeChildPath("1_InitialTables.sql");
   FilePath outFile2Sqlite = workingDir.completeChildPath("2_ConstraintsForInitialTables.sqlite");
   FilePath outFile2Postgresql = workingDir.completeChildPath("2_ConstraintsForInitialTables.postgresql");
   FilePath outFile3Sqlite = workingDir.completeChildPath("3_AddAccountCreationTime.sqlite");
   FilePath outFile3Postgresql = workingDir.completeChildPath("3_AddAccountCreationTime.postgresql");

   error = writeStringToFile(outFile1, schema1);
   ASSERT_FALSE(error) << "Failed to write initial schema file: " << error.getMessage();

   error = writeStringToFile(outFile2Sqlite, schema2Sqlite);
   ASSERT_FALSE(error) << "Failed to write SQLite constraints schema file: " << error.getMessage();

   error = writeStringToFile(outFile2Postgresql, schema2Postgresql);
   ASSERT_FALSE(error) << "Failed to write PostgreSQL constraints schema file: " << error.getMessage();

   error = writeStringToFile(outFile3Sqlite, schema3Sqlite);
   ASSERT_FALSE(error) << "Failed to write SQLite account creation schema file: " << error.getMessage();

   error = writeStringToFile(outFile3Postgresql, schema3Postgresql);
   ASSERT_FALSE(error) << "Failed to write PostgreSQL account creation schema file: " << error.getMessage();

   boost::shared_ptr<IConnection> postgresConnection;
   error = connect(postgresConnectionOptions(), &postgresConnection);
   ASSERT_FALSE(error) << "Failed to connect to PostgreSQL database for schema update test: " << error.getMessage();

   SchemaUpdater sqliteUpdater(sqliteConnection, workingDir);
   SchemaUpdater postgresUpdater(postgresConnection, workingDir);

   error = sqliteUpdater.update();
   ASSERT_FALSE(error) << "Failed to update SQLite schema: " << error.getMessage();

   error = postgresUpdater.update();
   ASSERT_FALSE(error) << "Failed to update PostgreSQL schema: " << error.getMessage();

   SchemaVersion currentSchemaVersion;
   error = sqliteUpdater.databaseSchemaVersion(&currentSchemaVersion);
   ASSERT_FALSE(error) << "Failed to get SQLite schema version: " << error.getMessage();
   ASSERT_EQ(currentSchemaVersion, SchemaVersion("3", RSTUDIO_RELEASE_NAME));

   currentSchemaVersion = SchemaVersion();
   error = postgresUpdater.databaseSchemaVersion(&currentSchemaVersion);
   ASSERT_FALSE(error) << "Failed to get PostgreSQL schema version: " << error.getMessage();
   ASSERT_EQ(currentSchemaVersion, SchemaVersion("3", RSTUDIO_RELEASE_NAME));

   // ensure repeated calls to update work without error
   error = sqliteUpdater.update();
   ASSERT_FALSE(error) << "Failed to update SQLite schema (repeated call): " << error.getMessage();

   error = postgresUpdater.update();
   ASSERT_FALSE(error) << "Failed to update PostgreSQL schema (repeated call): " << error.getMessage();

   // ensure we can insert data as expected (given our expected constraints)
   int id = 1;
   std::string firstName = "Billy";
   std::string lastName = "Joel";
   std::string email = "bjoel@example.com";
   std::string creationTime = "03/03/2020 12:00:00";

   // create queries - we will be executing them multiple times, so bind input just before execution
   Query sqliteInsertQuery = sqliteConnection->query("INSERT INTO TestTable1_Persons VALUES (:id, :fname, :lname, :email)");
   Query postgresInsertQuery = postgresConnection->query("INSERT INTO TestTable1_Persons VALUES (:id, :fname, :lname, :email)");
   Query sqliteInsertQuery2 = sqliteConnection->query("INSERT INTO TestTable2_AccountHolders VALUES (:id, :pid, :time)");
   Query postgresInsertQuery2 = postgresConnection->query("INSERT INTO TestTable2_AccountHolders VALUES (:id, :pid, :time)");

   // should fail - FK constraint
   sqliteInsertQuery2
         .withInput(id, "id")
         .withInput(id, "pid")
         .withInput(creationTime, "time");
   postgresInsertQuery2
         .withInput(id, "id")
         .withInput(id, "pid")
         .withInput(creationTime, "time");
   EXPECT_TRUE(sqliteConnection->execute(sqliteInsertQuery2));
   ASSERT_TRUE(postgresConnection->execute(postgresInsertQuery2));

   // should succeed - properly ordered
   sqliteInsertQuery
         .withInput(id, "id")
         .withInput(firstName, "fname")
         .withInput(lastName, "lname")
         .withInput(email, "email");
   EXPECT_FALSE(sqliteConnection->execute(sqliteInsertQuery));
   sqliteInsertQuery2
         .withInput(id, "id")
         .withInput(id, "pid")
         .withInput(creationTime, "time");
   EXPECT_FALSE(sqliteConnection->execute(sqliteInsertQuery2));
   postgresInsertQuery
         .withInput(id, "id")
         .withInput(firstName, "fname")
         .withInput(lastName, "lname")
         .withInput(email, "email");
   EXPECT_FALSE(postgresConnection->execute(postgresInsertQuery));
   postgresInsertQuery2
         .withInput(id, "id")
         .withInput(id, "pid")
         .withInput(creationTime, "time");
   EXPECT_FALSE(postgresConnection->execute(postgresInsertQuery2));

   // should fail - PK constraint
   sqliteInsertQuery
         .withInput(id, "id")
         .withInput(firstName, "fname")
         .withInput(lastName, "lname")
         .withInput(email, "email");
   ASSERT_TRUE(sqliteConnection->execute(sqliteInsertQuery));
   sqliteInsertQuery2
         .withInput(id, "id")
         .withInput(id, "pid")
         .withInput(creationTime, "time");
   ASSERT_TRUE(sqliteConnection->execute(sqliteInsertQuery2));
   postgresInsertQuery
         .withInput(id, "id")
         .withInput(firstName, "fname")
         .withInput(lastName, "lname")
         .withInput(email, "email");
   ASSERT_TRUE(postgresConnection->execute(postgresInsertQuery));
   postgresInsertQuery2
         .withInput(id, "id")
         .withInput(id, "pid")
         .withInput(creationTime, "time");
   ASSERT_TRUE(postgresConnection->execute(postgresInsertQuery2));
}

TEST(DatabaseTest, SchemaVersionComparisonsAreCorrect)
{
   std::vector<SchemaVersion> versions {
      {"", ""},
      {"Ghost Orchid", "20210712182145921760944"},
      {"Prairie Trillium", "20210916132211194382021"},
   };

   for(int i=0; i < (int) versions.size(); i++)
   {
      //Compare against smaller
      for(int j=0; j < i; j++){
         ASSERT_LT(versions[j], versions[i]) << "Expected " << versions[j] << " to be less than " << versions[i];
         ASSERT_FALSE(versions[j] > versions[i]) << "Expected " << versions[j] << " to not be greater than " << versions[i];
         ASSERT_LE(versions[j], versions[i]) << "Expected " << versions[j] << " to be less than or equal to " << versions[i];
      }

      SchemaVersion sameVersion(versions[i]);
      ASSERT_EQ(versions[i], sameVersion);

      //Compare against larger
      for(int j=i+1; j < (int) versions.size(); j++)
      {
         ASSERT_LT(versions[i], versions[j]) << "Expected " << versions[i] << " to be less than " << versions[j];
         ASSERT_FALSE(versions[i] > versions[j]) << "Expected " << versions[i] << " to not be greater than " << versions[j];
         ASSERT_LE(versions[i], versions[j]) << "Expected " << versions[i] << " to be less than or equal to " << versions[j];
      }
   }
}

TEST_F(DatabaseTestsFixture, CanExecuteStrWithMultipleQueries)
{
   std::string queryStr =
         "CREATE TABLE TestTable_3("
         "A text, B text\n);            \n"
         "INSERT INTO TestTable_3 VALUES (\"Hello\", \"World;      \");\n"
         "INSERT INTO TestTable_3 VALUES (\"Hello2\", \";;;\");";

   Error error = sqliteConnection->executeStr(queryStr);
   ASSERT_FALSE(error) << "Failed to execute multi-statement query: " << error.getMessage();

   Query selectQuery = sqliteConnection->query("select * from TestTable_3 order by A asc");

   Rowset rowset;
   error = sqliteConnection->execute(selectQuery, rowset);
   ASSERT_FALSE(error) << "Failed to select from TestTable_3: " << error.getMessage();

   int i = 0;
   std::string vals[2][2];
   vals[0][0] = "Hello";
   vals[0][1] = "World;      ";
   vals[1][0] = "Hello2";
   vals[1][1] = ";;;";
   for (RowsetIterator it = rowset.begin(); it != rowset.end(); ++it)
   {
      Row& row = *it;
   ASSERT_EQ(std::string(vals[i][0]), row.get<std::string>(0));
   ASSERT_EQ(std::string(vals[i][1]), row.get<std::string>(1));
      ++i;
   }
}

TEST(DatabaseTest, CanCorrectlyParsePostgresqlConnectionUris)
{
#ifdef RSTUDIO_HAS_SOCI_POSTGRESQL
   PostgresqlConnectionOptions options;
   // Invalid URIs should return errors
   options.connectionUri = "bogus://not-a-uri";
   EXPECT_TRUE(validateOptions(options, nullptr));

   options.connectionUri = "postgresql://";
   EXPECT_TRUE(validateOptions(options, nullptr));

   std::string connectionStr;

   // There urls are invalid because they don't include passwords and we don't have ssl support.
   options.connectionUri = "postgres://localhost";
   EXPECT_TRUE(validateOptions(options, &connectionStr));

   options.connectionUri = "postgres://joe@myhost/";
   EXPECT_TRUE(validateOptions(options, &connectionStr));

   options.connectionUri = "postgres://joe@myhost/rstudio-test";
   EXPECT_TRUE(validateOptions(options, &connectionStr));

   // For valid URIs in the current environment configuration,
   // validateOptions returns true and the connection string is empty in our test environment
   options.connectionUri = "postgres://joe:mypass@myhost";
   EXPECT_FALSE(validateOptions(options, &connectionStr));
   EXPECT_EQ(std::string("host='myhost' user='joe' password='mypass'"), connectionStr);

   options.connectionUri = "postgres://joe@myhost/rstudio-test";
   options.password = "abc123";
   EXPECT_FALSE(validateOptions(options, &connectionStr));
   EXPECT_EQ(std::string("host='myhost' user='joe' dbname='rstudio-test' password='abc123'"), connectionStr);

   options.connectionUri = "postgres://joe@myhost/rstudio-test";
   options.password = "abc'\\123";
   EXPECT_FALSE(validateOptions(options, &connectionStr));
   EXPECT_EQ(std::string("host='myhost' user='joe' dbname='rstudio-test' password='abc'\\123'"), connectionStr);

   options.connectionUri = "postgres://joe@myhost/rstudio-test?sslmode=disable";
   options.password = "abc123";
   EXPECT_FALSE(validateOptions(options, &connectionStr));
   EXPECT_EQ(std::string("host='myhost' user='joe' dbname='rstudio-test' sslmode='disable' password='abc123'"), connectionStr);

   options.connectionUri = "postgres://joe@myhost:3342/rstudio-test?sslmode=disable&options=-csearch_path=public";
   options.password = "abc123";
   EXPECT_FALSE(validateOptions(options, &connectionStr));
   EXPECT_EQ(std::string("host='myhost' port='3342' user='joe' dbname='rstudio-test' sslmode='disable' options='-csearch_path=public' password='abc123'"), connectionStr);

   options.connectionUri = "postgres://joe@myhost/rstudio-test?sslmode=disable&options=-csearch_path=public&random-value=something%20with%20spaces";
   options.password = "abc123";
   EXPECT_FALSE(validateOptions(options, &connectionStr));
   EXPECT_EQ(std::string("host='myhost' user='joe' dbname='rstudio-test' sslmode='disable' options='-csearch_path=public' random-value='something with spaces' password='abc123'"), connectionStr);

   options.connectionUri = "postgres://joe@[fd9a:3b89:ca91:43a2:0:0:0:0]:2345/rstudio-test";
   options.password = "12345";
   EXPECT_FALSE(validateOptions(options, &connectionStr));
   EXPECT_EQ(std::string("host='[fd9a:3b89:ca91:43a2:0:0:0:0]' port='2345' user='joe' dbname='rstudio-test' password='12345'"), connectionStr);

   std::string password;
   EXPECT_FALSE(validateOptions(options, &connectionStr, &password));
   EXPECT_EQ(std::string("12345"), password) << "Expected password to be '12345' but got '" << password << "'";
   EXPECT_EQ(std::string("host='[fd9a:3b89:ca91:43a2:0:0:0:0]' port='2345' user='joe' dbname='rstudio-test'"), connectionStr);
#else
   GTEST_SKIP() << "Skipping Postgres connection URI tests as Postgres support is not enabled with RSTUDIO_HAS_SOCI_POSTGRESQL";
#endif
}

TEST_F(DatabaseTestsFixture,WhenUsedWithMultipleRowsSelectsFromTheFirst)
{
   Query query = sqliteConnection->query("create table MultiRowTest(id int, text varchar(255))");
   Error error = sqliteConnection->execute(query);
   ASSERT_FALSE(error) << "Failed to create MultiRowTest table: " << error.getMessage();

   int id = 1;
   std::string text = "stuff";

   query = sqliteConnection->query("insert into MultiRowTest(id, text) values(:id, :text)")
         .withInput(id)
         .withInput(text);
   error = sqliteConnection->execute(query);
   ASSERT_FALSE(error) << "Failed to insert first row into MultiRowTest: " << error.getMessage();

   id = 2;
   text = "stuff";
   query = sqliteConnection->query("insert into MultiRowTest(id, text) values(:id, :text)")
         .withInput(id)
         .withInput(text);
   error = sqliteConnection->execute(query);
   ASSERT_FALSE(error) << "Failed to insert second row into MultiRowTest: " << error.getMessage();

   int rowId;
   std::string rowText;
   query = sqliteConnection->query("select id, text from MultiRowTest where text = (:text)")
         .withInput(text)
         .withOutput(rowId)
         .withOutput(rowText);

   bool dataReturned = false;
   error = sqliteConnection->execute(query, &dataReturned);
   ASSERT_FALSE(error) << "Failed to select from MultiRowTest: " << error.getMessage();
   ASSERT_TRUE(dataReturned) << "Expected data to be returned but none was";
   ASSERT_EQ(1, rowId) << "Expected first row ID to be 1 but got " << rowId;
}

TEST_F(DatabaseTestsFixture, WithoutputReturnsErrorWhenValueIsNull)
{
   Query query = sqliteConnection->query("create table NullValueTest(id int, text varchar(255))");
   Error error = sqliteConnection->execute(query);
   ASSERT_FALSE(error) << "Failed to create NullValueTest table: " << error.getMessage();

   int id = 1;
   query = sqliteConnection->query("insert into NullValueTest(id, text) values(:id, null)")
         .withInput(id);
   error = sqliteConnection->execute(query);
   ASSERT_FALSE(error) << "Failed to insert null value into NullValueTest: " << error.getMessage();

   int rowId;
   std::string rowText = "stuff";
   query = sqliteConnection->query("select id, text from NullValueTest where id = (:id)")
         .withInput(id)
         .withOutput(rowId)
         .withOutput(rowText);

   bool dataReturned = false;
   error = sqliteConnection->execute(query, &dataReturned);

   // The query fails with error for selecting a null value without a null indicator
   ASSERT_TRUE(error) << "Expected error when retrieving NULL value without indicator, but got success. Error message: " << error.getMessage();
}

TEST_F(DatabaseTestsFixture, GetOptionalValueCastsNumericTypes)
{
   Query query = sqliteConnection->query("create table CastTest(i_val integer, f_val real, b_val bigint, t_val text)");
   Error error = sqliteConnection->execute(query);
   ASSERT_FALSE(error) << "Failed to create CastTest table: " << error.getMessage();

   query = sqliteConnection->query("insert into CastTest (i_val, f_val, b_val, t_val) values (1, 2.2, 3, 'test')");
   error = sqliteConnection->execute(query);
   ASSERT_FALSE(error) << "Failed to insert values into CastTest: " << error.getMessage();

   Rowset rows;
   query = sqliteConnection->query("select i_val, f_val, b_val, t_val from CastTest");
   error = sqliteConnection->execute(query, rows);

   for (RowsetIterator it = rows.begin(); it != rows.end(); ++it)
   {
      boost::optional<int> i_val;
      boost::optional<double> f_val;
      boost::optional<long long> b_val;
      boost::optional<std::string> t_val;

      // Test without conversions first
      error = rows.getOptionalValue(it, "i_val", &i_val);
      EXPECT_FALSE(error);
      EXPECT_EQ(i_val.value(), 1);
      error = rows.getOptionalValue(it, "f_val", &f_val);
      EXPECT_FALSE(error);
      EXPECT_EQ(f_val.value(), 2.2);
      error = rows.getOptionalValue(it, "b_val", &b_val);
      EXPECT_FALSE(error);
      EXPECT_EQ(b_val.value(), 3);
      error = rows.getOptionalValue(it, "t_val", &t_val);
      EXPECT_FALSE(error);
      EXPECT_EQ(t_val.value(), "test");

      // Test int conversions
      error = rows.getOptionalValue<int>(it, "i_val", &i_val);
      EXPECT_FALSE(error);
      EXPECT_EQ(i_val.value(), 1);
      error = rows.getOptionalValue<int>(it, "f_val", &i_val);
      EXPECT_FALSE(error);
      EXPECT_EQ(i_val.value(), 2);
      error = rows.getOptionalValue<int>(it, "b_val", &i_val);
      EXPECT_FALSE(error);
      EXPECT_EQ(i_val.value(), 3);
      error = rows.getOptionalValue<int>(it, "t_val", &i_val);
      EXPECT_TRUE(error) << "no error despite casting t_val to int";

      // Test float conversions
      error = rows.getOptionalValue<double>(it, "i_val", &f_val);
      EXPECT_FALSE(error);
      EXPECT_EQ(f_val.value(), 1.0);
      error = rows.getOptionalValue<double>(it, "f_val", &f_val);
      EXPECT_FALSE(error);
      EXPECT_EQ(f_val.value(), 2.2);
      error = rows.getOptionalValue<double>(it, "b_val", &f_val);
      EXPECT_FALSE(error);
      EXPECT_EQ(f_val.value(), 3.0);
      error = rows.getOptionalValue<double>(it, "t_val", &f_val);
      EXPECT_TRUE(error) << "no error despite casting t_val to double";

      // Test long long conversions
      error = rows.getOptionalValue<long long>(it, "i_val", &b_val);
      EXPECT_FALSE(error);
      EXPECT_EQ(b_val.value(), 1);
      error = rows.getOptionalValue<long long>(it, "f_val", &b_val);
      EXPECT_FALSE(error);
      EXPECT_EQ(b_val.value(), 2);
      error = rows.getOptionalValue<long long>(it, "b_val", &b_val);
      EXPECT_FALSE(error);
      EXPECT_EQ(b_val.value(), 3);
      error = rows.getOptionalValue<long long>(it, "t_val", &b_val);
      EXPECT_TRUE(error) << "no error despite casting t_val to long long";

      // Make sure strings don't mingle
      error = rows.getOptionalValue<std::string>(it, "i_val", &t_val);
      EXPECT_TRUE(error) << "no error despite casting i_val to string";
      error = rows.getOptionalValue<std::string>(it, "f_val", &t_val);
      EXPECT_TRUE(error) << "no error despite casting f_val to string";
      error = rows.getOptionalValue<std::string>(it, "b_val", &t_val);
      EXPECT_TRUE(error) << "no error despite casting b_val to string";
      error = rows.getOptionalValue<std::string>(it, "t_val", &t_val);
      EXPECT_FALSE(error);
      EXPECT_EQ(t_val.value(), "test");
   }
}

class ConnectionPoolTestsFixture : public ::testing::Test
{
protected:
   FilePath dbPath;
   boost::shared_ptr<ConnectionPool> connectionPool;

   void SetUp() override
   {
      dbPath = FilePath("/tmp/rstudio-test-db-pool");
      dbPath.removeIfExists();

      SqliteConnectionOptions options;
      options.file = dbPath.getAbsolutePath();

      Error error = createConnectionPool(5, options, &connectionPool);
      ASSERT_FALSE(error) << "Failed to create connection pool: " << error.getMessage();

      boost::shared_ptr<IConnection> connection = connectionPool->getConnection();
      ASSERT_TRUE(connection) << "Failed to get initial connection from pool";

      error = initializeCommonTestSchema(*connection);
      ASSERT_FALSE(error) << "Failed to initialize common test schema (pool): " << error.getMessage();
   }

   void TearDown() override
   {
      connectionPool.reset();
      dbPath.removeIfExists();
   }
};

TEST_F(ConnectionPoolTestsFixture, CanUseConnectionPool)
{
   boost::shared_ptr<IConnection> connection = connectionPool->getConnection();
   ASSERT_TRUE(connection) << "Failed to get connection from pool";

   // Insert some data into the Test table
   Query query = connection->query("insert into Test(id, text) values(:id, :text)");
   for (int id = 0; id < 100; ++id)
   {
      std::string text = "Test text " + core::safe_convert::numberToString(id);
      query.withInput(id).withInput(text);
      Error error = connection->execute(query);
      ASSERT_FALSE(error) << "Failed to insert row " << id << ": " << error.getMessage();
      // Rebind fresh for next iteration
      query = connection->query("insert into Test(id, text) values(:id, :text)");
   }

   int rowId;
   std::string rowText;
   Query select50 = connection->query("select id, text from Test where id = 50")
         .withOutput(rowId)
         .withOutput(rowText);

   bool dataReturned = false;
   Error error = connection->execute(select50, &dataReturned);
   ASSERT_FALSE(error) << "Failed to execute first query: " << error.getMessage();
   ASSERT_TRUE(dataReturned) << "Expected data for id=50";

   boost::shared_ptr<IConnection> connection2 = connectionPool->getConnection();
   ASSERT_TRUE(connection2) << "Failed to get second connection from pool";

   Query select25 = connection2->query("select id, text from Test where id = 25")
         .withOutput(rowId)
         .withOutput(rowText);

   dataReturned = false;
   error = connection2->execute(select25, &dataReturned);
   ASSERT_FALSE(error) << "Failed to execute second query: " << error.getMessage();
   ASSERT_TRUE(dataReturned) << "Expected data for id=25";
}
} // namespace tests
} // namespace core
} // namespace rstudio
