/*
 * ZoteroCollectionsLocalTests.cpp
 *
 * Copyright (C) 2026 by Posit Software, PBC
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

#include "ZoteroCollectionsLocal.hpp"

#include <boost/shared_ptr.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

#include <core/Database.hpp>

#include <gtest/gtest.h>

namespace rstudio {
namespace session {
namespace modules {
namespace zotero {
namespace collections {
namespace tests {

using namespace rstudio::core;

namespace {

enum class JournalMode
{
   Wal,
   Rollback
};

// core::database puts every read-write connection into WAL mode, so a rollback
// journal -- what Zotero 9 and earlier leave behind -- has to be asked for.
Error openWritable(const FilePath& file,
                   JournalMode mode,
                   boost::shared_ptr<database::IConnection>* ppConnection)
{
   database::SqliteConnectionOptions options;
   options.file = file.getAbsolutePath();
   options.readonly = false;

   Error error = database::connect(options, ppConnection);
   if (error)
      return error;

   if (mode == JournalMode::Rollback)
      return (*ppConnection)->executeStr("PRAGMA journal_mode = DELETE;");

   return Success();
}

Error countRows(boost::shared_ptr<database::IConnection> pConnection,
                const std::string& sql,
                std::size_t* pCount)
{
   database::Rowset rows;
   database::Query query = pConnection->query(sql);
   Error error = pConnection->execute(query, rows);
   if (error)
      return error;

   *pCount = 0;
   for (database::RowsetIterator it = rows.begin(); it != rows.end(); ++it)
      (*pCount)++;

   return Success();
}

} // anonymous namespace

class ZoteroDatabaseCopyTest : public ::testing::Test
{
protected:
   void SetUp() override
   {
      ASSERT_FALSE(FilePath::tempFilePath(dir_));
      ASSERT_FALSE(dir_.ensureDirectory());

      dbFile_ = dir_.completeChildPath("zotero.sqlite");
      walFile_ = dir_.completeChildPath("zotero.sqlite-wal");
      copyFile_ = dir_.completeChildPath("copy.sqlite");
   }

   void TearDown() override
   {
      pSource_.reset();
      dir_.removeIfExists();
   }

   // Builds the source database with a single library. Holding the connection
   // open leaves the commits in an uncheckpointed -wal; otherwise the database
   // is left as a shut down client leaves it -- no -wal, header unchanged.
   void createSource(JournalMode mode, bool keepOpen)
   {
      Error error = openWritable(dbFile_, mode, &pSource_);
      ASSERT_FALSE(error) << error.asString();

      ASSERT_FALSE(pSource_->executeStr(
         "CREATE TABLE libraries (libraryID INTEGER PRIMARY KEY, type TEXT);"));
      ASSERT_FALSE(pSource_->executeStr("INSERT INTO libraries VALUES (1, 'user');"));

      if (keepOpen)
         return;

      if (mode == JournalMode::Wal)
         ASSERT_FALSE(pSource_->executeStr("PRAGMA wal_checkpoint(TRUNCATE);"));

      pSource_.reset();

      // sqlite on macOS keeps the sidecars across a close, so drop them here
      // rather than depending on which sqlite the platform links
      ASSERT_FALSE(walFile_.removeIfExists());
      ASSERT_FALSE(dir_.completeChildPath("zotero.sqlite-shm").removeIfExists());
   }

   void addLibraryToSource(int libraryID)
   {
      ASSERT_FALSE(pSource_->executeStr(
         "INSERT INTO libraries VALUES (" + std::to_string(libraryID) + ", 'group');"));
   }

   // Reads the copy exactly as connect() does: a read-only connection.
   std::size_t readCopyLibraryCount()
   {
      database::SqliteConnectionOptions options;
      options.file = copyFile_.getAbsolutePath();
      options.readonly = true;

      boost::shared_ptr<database::IConnection> pConnection;
      Error error = database::connect(options, &pConnection);
      EXPECT_FALSE(error) << error.asString();
      if (error)
         return 0;

      std::size_t count = 0;
      error = countRows(pConnection, "SELECT * FROM libraries", &count);
      EXPECT_FALSE(error) << error.asString();
      return count;
   }

   FilePath dir_;
   FilePath dbFile_;
   FilePath walFile_;
   FilePath copyFile_;
   boost::shared_ptr<database::IConnection> pSource_;
};

// A cleanly shut down Zotero 10 checkpoints and removes its -wal, but the
// database header still declares WAL mode.
TEST_F(ZoteroDatabaseCopyTest, ReadsCopyOfClosedWalModeDatabase)
{
   createSource(JournalMode::Wal, false /* keepOpen */);
   ASSERT_FALSE(walFile_.exists());

   Error error = prepareDatabaseCopy(dbFile_, copyFile_);
   ASSERT_FALSE(error) << error.asString();

   EXPECT_EQ(1u, readCopyLibraryCount());
}

// With Zotero 10 running, recent commits live only in the -wal.
TEST_F(ZoteroDatabaseCopyTest, ReadsLibrariesHeldInSourceWal)
{
   createSource(JournalMode::Wal, true /* keepOpen */);
   addLibraryToSource(2);
   ASSERT_TRUE(walFile_.exists());

   Error error = prepareDatabaseCopy(dbFile_, copyFile_);
   ASSERT_FALSE(error) << error.asString();

   EXPECT_EQ(2u, readCopyLibraryCount());
}

// Zotero 9 and earlier use a rollback journal; that path has to keep working.
TEST_F(ZoteroDatabaseCopyTest, ReadsCopyOfRollbackJournalDatabase)
{
   createSource(JournalMode::Rollback, false /* keepOpen */);
   ASSERT_FALSE(walFile_.exists());

   Error error = prepareDatabaseCopy(dbFile_, copyFile_);
   ASSERT_FALSE(error) << error.asString();

   EXPECT_EQ(1u, readCopyLibraryCount());
}

// Under WAL a commit leaves zotero.sqlite's mtime untouched, so a copy stamped
// from the main file alone would never look stale again.
TEST_F(ZoteroDatabaseCopyTest, RefreshesCopyWhenOnlyWalIsNewer)
{
   createSource(JournalMode::Wal, true /* keepOpen */);

   Error error = prepareDatabaseCopy(dbFile_, copyFile_);
   ASSERT_FALSE(error) << error.asString();
   ASSERT_EQ(1u, readCopyLibraryCount());

   addLibraryToSource(2);

   // pin the mtimes so the staleness decision can't turn on clock granularity
   std::time_t walWriteTime = dbFile_.getLastWriteTime() + 60;
   walFile_.setLastWriteTime(walWriteTime);

   error = prepareDatabaseCopy(dbFile_, copyFile_);
   ASSERT_FALSE(error) << error.asString();

   EXPECT_EQ(2u, readCopyLibraryCount());
   EXPECT_EQ(walWriteTime, copyFile_.getLastWriteTime());
}

} // end namespace tests
} // end namespace collections
} // end namespace zotero
} // end namespace modules
} // end namespace session
} // end namespace rstudio
