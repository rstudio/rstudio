/*
 * Win32FileLocktests.cpp
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

#ifdef _WIN32

#include <core/FileLock.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>
#include <core/system/Environment.hpp>

#include <gtest/gtest.h>

namespace rstudio {
namespace core {
namespace tests {

namespace {

FilePath tempLockFilePath()
{
   std::string home = core::system::getenv("USERPROFILE");
   return FilePath(home + "\\rstudio-lock-file");
}

FilePath s_lockFilePath = tempLockFilePath();

} // end anonymous namespace

TEST(Win32Test, FileLockingBasicAdvisoryFileLockAssumptionsHoldTrue)
{
   Error error;

   // create lock file
   error = s_lockFilePath.ensureFile();
   EXPECT_FALSE(error);

   // ensure file is not locked
   AdvisoryFileLock lock;
   EXPECT_TRUE(s_lockFilePath.exists());
   EXPECT_FALSE(lock.isLocked(s_lockFilePath));

   // attempt to acquire that lock
   error = lock.acquire(s_lockFilePath);
   EXPECT_FALSE(error);

   // attempt to acquire lock with alternate object
   AdvisoryFileLock otherLock;
   error = otherLock.acquire(s_lockFilePath);
   EXPECT_TRUE(error);

   // check that attempts to ascertain lockedness
   // don't incidentally release the lock
   EXPECT_TRUE(lock.isLocked(s_lockFilePath));
   EXPECT_TRUE(otherLock.isLocked(s_lockFilePath));

   // release lock
   error = lock.release();
   EXPECT_FALSE(error);

   // ensure released
   EXPECT_FALSE(lock.isLocked(s_lockFilePath));

   // check that destructor releases lock
   {
      AdvisoryFileLock scopedLock;
      error = scopedLock.acquire(s_lockFilePath);
   EXPECT_FALSE(error);
   }
   EXPECT_FALSE(lock.isLocked(s_lockFilePath));

   // clean up file
   s_lockFilePath.removeIfExists();
}

} // end namespace tests
} // end namespace core
} // end namespace rstudio

#endif // _WIN32
