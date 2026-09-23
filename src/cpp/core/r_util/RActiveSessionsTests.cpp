/*
 * RActiveSessionsTests.cpp
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

#include <core/r_util/RActiveSessions.hpp>
#include <core/r_util/RActiveSessionsStorage.hpp>

#include <ctime>
#include <memory>
#include <string>
#include <vector>

#ifndef _WIN32
#include <sys/stat.h>
#include <unistd.h>
#endif

#include <gtest/gtest.h>

#include <core/FileSerializer.hpp>

namespace rstudio {
namespace core {
namespace r_util {

namespace {

const std::time_t kMaxAgeSeconds = 60 * 60 * 24;

class ActiveSessionsTest : public testing::Test
{
protected:
   void SetUp() override
   {
      ASSERT_FALSE(FilePath::tempFilePath(root_));
      ASSERT_FALSE(root_.ensureDirectory());

      auto storage = std::make_shared<FileActiveSessionsStorage>(
         root_,
         [](const std::string&) { return ProjectId(); });
      sessions_.reset(new ActiveSessions(storage, root_));
   }

   void TearDown() override
   {
      EXPECT_FALSE(root_.removeIfExists());
   }

   FilePath sessionDir(const std::string& id) const
   {
      return ActiveSessions::storagePath(root_).completeChildPath(kSessionDirPrefix + id);
   }

   // an R session whose project property is empty, as left behind by a crash
   // while its properties were being written
   FilePath createInvalidSession(const std::string& id) const
   {
      FilePath dir = sessionDir(id);
      FilePath properties = dir.completeChildPath("properites");
      EXPECT_FALSE(properties.ensureDirectory());
      EXPECT_FALSE(writeStringToFile(properties.completeChildPath(ActiveSession::kEditor), kWorkbenchRStudio));
      EXPECT_FALSE(writeStringToFile(properties.completeChildPath(ActiveSession::kProject), ""));
      return dir;
   }

   void setModified(const FilePath& dir, std::time_t time) const
   {
      EXPECT_FALSE(dir.getChildrenRecursive([&](int, const FilePath& child)
      {
         child.setLastWriteTime(time);
         return true;
      }));
      dir.setLastWriteTime(time);
   }

   std::vector<boost::shared_ptr<ActiveSession>> listInvalid() const
   {
      std::vector<boost::shared_ptr<ActiveSession>> invalid;
      sessions_->list(true, &invalid);
      return invalid;
   }

   FilePath root_;
   std::unique_ptr<ActiveSessions> sessions_;
};

} // anonymous namespace

TEST_F(ActiveSessionsTest, RemovesLongAbandonedInvalidSessions)
{
   std::time_t old = std::time(nullptr) - 2 * kMaxAgeSeconds;

   std::string validId;
   ASSERT_FALSE(sessions_->create("~/project", "~", &validId));
   setModified(sessionDir(validId), old);

   FilePath abandoned = createInvalidSession("aaaaaaaa");
   setModified(abandoned, old);

   FilePath recent = createInvalidSession("bbbbbbbb");

   std::vector<boost::shared_ptr<ActiveSession>> invalid = listInvalid();
   ASSERT_EQ(2u, invalid.size());
   sessions_->removeStaleInvalidSessions(invalid, kMaxAgeSeconds);

   EXPECT_FALSE(abandoned.exists());
   EXPECT_TRUE(recent.exists());

   // valid sessions aren't candidates, however old
   EXPECT_TRUE(sessionDir(validId).exists());
   std::vector<boost::shared_ptr<ActiveSession>> valid = sessions_->list(true);
   ASSERT_EQ(1u, valid.size());
   EXPECT_EQ(validId, valid.front()->id());
}

TEST_F(ActiveSessionsTest, KeepsInvalidSessionsBeingWritten)
{
   // an old session whose project property is being rewritten just now
   FilePath dir = createInvalidSession("cccccccc");
   setModified(dir, std::time(nullptr) - 2 * kMaxAgeSeconds);
   ASSERT_FALSE(writeStringToFile(dir.completeChildPath("properites").completeChildPath(ActiveSession::kProject), ""));

   sessions_->removeStaleInvalidSessions(listInvalid(), kMaxAgeSeconds);

   EXPECT_TRUE(dir.exists());
}

TEST_F(ActiveSessionsTest, KeepsInvalidSessionsWithSuspendedWorkspace)
{
   std::time_t old = std::time(nullptr) - 2 * kMaxAgeSeconds;

   FilePath suspended = createInvalidSession("dddddddd");
   ASSERT_FALSE(suspended.completeChildPath("suspended-session-data").ensureDirectory());
   setModified(suspended, old);

   // one whose suspended data was set aside after it failed to restore
   FilePath setAside = createInvalidSession("ffffffff");
   ASSERT_FALSE(setAside.completeChildPath("suspended-session-data-unrestored").ensureDirectory());
   setModified(setAside, old);

   sessions_->removeStaleInvalidSessions(listInvalid(), kMaxAgeSeconds);

   EXPECT_TRUE(suspended.exists());
   EXPECT_TRUE(setAside.exists());
}

#ifndef _WIN32
TEST_F(ActiveSessionsTest, KeepsSessionsWithUnreadableProperties)
{
   if (::geteuid() == 0)
      GTEST_SKIP() << "root bypasses file permissions";

   // an old session that may be fine, but whose project property was left
   // unreadable (e.g. owned by root after a sudo run)
   FilePath dir = createInvalidSession("eeeeeeee");
   FilePath project = dir.completeChildPath("properites").completeChildPath(ActiveSession::kProject);
   ASSERT_FALSE(writeStringToFile(project, "~/project"));
   setModified(dir, std::time(nullptr) - 2 * kMaxAgeSeconds);
   ASSERT_EQ(0, ::chmod(project.getAbsolutePath().c_str(), 0));

   std::vector<boost::shared_ptr<ActiveSession>> invalid = listInvalid();
   ASSERT_EQ(1u, invalid.size());
   sessions_->removeStaleInvalidSessions(invalid, kMaxAgeSeconds);

   EXPECT_TRUE(dir.exists());
}
#endif

} // namespace r_util
} // namespace core
} // namespace rstudio
