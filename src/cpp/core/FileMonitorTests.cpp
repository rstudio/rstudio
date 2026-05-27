/*
 * FileMonitorTests.cpp
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

// The behavior covered here -- per-file event delivery for non-recursive
// watches via FSEvents kFSEventStreamCreateFlagFileEvents -- is specific to
// MacFileMonitor.cpp. The Linux (inotify) and Windows (ReadDirectoryChangesW)
// backends have parallel semantics but different code paths. core/CMakeLists
// globs all *Tests.cpp, so we gate the whole body rather than introduce a
// per-platform conditional list.
#ifdef __APPLE__

#include <chrono>
#include <cerrno>
#include <thread>
#include <fstream>
#include <vector>

#include <unistd.h>

#include <boost/algorithm/string/predicate.hpp>
#include <boost/bind/bind.hpp>

#include <gtest/gtest.h>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

#include <core/FileInfo.hpp>
#include <core/collection/Tree.hpp>
#include <core/system/FileChangeEvent.hpp>
#include <core/system/FileMonitor.hpp>
#include <core/system/System.hpp>

namespace rstudio {
namespace core {
namespace tests {

namespace {

struct CallbackState
{
   bool registered = false;
   bool registrationError = false;
   bool monitoringError = false;
   bool unregistered = false;
   system::file_monitor::Handle handle;
   std::vector<system::FileChangeEvent> events;
};

void onRegistered(CallbackState* pState,
                  system::file_monitor::Handle handle,
                  const tree<FileInfo>& /*files*/)
{
   pState->handle = handle;
   pState->registered = true;
}

void onRegistrationError(CallbackState* pState, const Error& /*error*/)
{
   pState->registrationError = true;
}

void onMonitoringError(CallbackState* pState, const Error& /*error*/)
{
   pState->monitoringError = true;
}

void onFilesChanged(CallbackState* pState,
                    const std::vector<system::FileChangeEvent>& events)
{
   for (const auto& event : events)
      pState->events.push_back(event);
}

void onUnregistered(CallbackState* pState, system::file_monitor::Handle /*handle*/)
{
   pState->unregistered = true;
}

// Pumps the file_monitor callback queue on the current thread and polls
// the supplied predicate until it returns true or the timeout elapses.
template <typename Predicate>
bool waitFor(Predicate pred,
             std::chrono::milliseconds timeout = std::chrono::seconds(8))
{
   auto deadline = std::chrono::steady_clock::now() + timeout;
   while (std::chrono::steady_clock::now() < deadline)
   {
      system::file_monitor::checkForChanges();
      if (pred())
         return true;
      std::this_thread::sleep_for(std::chrono::milliseconds(50));
   }
   system::file_monitor::checkForChanges();
   return pred();
}

bool hasEventFor(const CallbackState& state,
                 system::FileChangeEvent::Type type,
                 const std::string& path)
{
   for (const auto& event : state.events)
   {
      if (event.type() == type &&
          event.fileInfo().absolutePath() == path)
      {
         return true;
      }
   }
   return false;
}

void writeFile(const FilePath& path, const std::string& contents)
{
   std::ofstream out(path.getAbsolutePath());
   ASSERT_TRUE(out.is_open());
   out << contents;
}

system::file_monitor::Handle startMonitor(
      const FilePath& dir,
      CallbackState* pState,
      boost::function<bool(const FileInfo&)> filter =
         boost::function<bool(const FileInfo&)>())
{
   using namespace boost::placeholders;

   system::file_monitor::Callbacks cb;
   cb.onRegistered = boost::bind(onRegistered, pState, _1, _2);
   cb.onRegistrationError = boost::bind(onRegistrationError, pState, _1);
   cb.onMonitoringError = boost::bind(onMonitoringError, pState, _1);
   cb.onFilesChanged = boost::bind(onFilesChanged, pState, _1);
   cb.onUnregistered = boost::bind(onUnregistered, pState, _1);

   system::file_monitor::registerMonitor(
      dir,
      /*recursive=*/false,
      filter,
      cb);

   EXPECT_TRUE(waitFor([&] { return pState->registered || pState->registrationError; }));
   EXPECT_TRUE(pState->registered);
   EXPECT_FALSE(pState->registrationError);
   return pState->handle;
}

void stopMonitor(system::file_monitor::Handle handle, CallbackState* pState)
{
   system::file_monitor::unregisterMonitor(handle);
   waitFor([&] { return pState->unregistered; });
}

} // anonymous namespace

class FileMonitorTest : public ::testing::Test
{
protected:
   static void SetUpTestSuite()
   {
      system::file_monitor::initialize();
   }

   static void TearDownTestSuite()
   {
      system::file_monitor::stop();
   }

   void SetUp() override
   {
      std::string name = "rstudio-file-monitor-test-" + system::generateUuid(false);
      FilePath created = FilePath("/tmp").completeChildPath(name);
      Error error = created.ensureDirectory();
      ASSERT_FALSE(error) << error.asString();

      // Use the canonical path; FSEvents reports paths in canonical form
      // (e.g. /private/tmp/... rather than /tmp/...) and registerMonitor
      // canonicalizes the rootPath internally before scanning the tree.
      std::string canonical = created.getCanonicalPath();
      ASSERT_FALSE(canonical.empty());
      tempDir_ = FilePath(canonical);
   }

   void TearDown() override
   {
      tempDir_.removeIfExists();
   }

   FilePath tempDir_;
};

TEST_F(FileMonitorTest, NonRecursiveDetectsFileAdded)
{
   CallbackState state;
   auto handle = startMonitor(tempDir_, &state);
   state.events.clear();

   FilePath child = tempDir_.completeChildPath("added.txt");
   writeFile(child, "hello");

   ASSERT_TRUE(waitFor([&] {
      return hasEventFor(state, system::FileChangeEvent::FileAdded,
                         child.getAbsolutePath());
   }));

   stopMonitor(handle, &state);
}

TEST_F(FileMonitorTest, NonRecursiveDetectsFileModified)
{
   FilePath child = tempDir_.completeChildPath("modified.txt");
   writeFile(child, "initial");
   // FileInfo stores st_mtimespec.tv_sec (truncated to whole seconds), so
   // back-to-back writes within the same wall-clock second produce identical
   // FileInfos and processFileAdded suppresses the modify. Sleep past the
   // second boundary so the next write produces an observably newer mtime.
   std::this_thread::sleep_for(std::chrono::milliseconds(1100));

   CallbackState state;
   auto handle = startMonitor(tempDir_, &state);
   state.events.clear();

   writeFile(child, "updated contents that are longer than the initial");

   ASSERT_TRUE(waitFor([&] {
      return hasEventFor(state, system::FileChangeEvent::FileModified,
                         child.getAbsolutePath());
   }));

   stopMonitor(handle, &state);
}

TEST_F(FileMonitorTest, NonRecursiveDetectsFileRemoved)
{
   FilePath child = tempDir_.completeChildPath("doomed.txt");
   writeFile(child, "soon to be gone");

   CallbackState state;
   auto handle = startMonitor(tempDir_, &state);
   state.events.clear();

   ASSERT_FALSE(child.remove());

   ASSERT_TRUE(waitFor([&] {
      return hasEventFor(state, system::FileChangeEvent::FileRemoved,
                         child.getAbsolutePath());
   }));

   stopMonitor(handle, &state);
}

TEST_F(FileMonitorTest, NonRecursiveIgnoresSubtreeChanges)
{
   FilePath subDir = tempDir_.completeChildPath("subdir");
   ASSERT_FALSE(subDir.ensureDirectory());

   CallbackState state;
   auto handle = startMonitor(tempDir_, &state);
   // The subdir itself was created before the watch started; we should NOT
   // receive any events for activity inside it.
   state.events.clear();

   FilePath nested = subDir.completeChildPath("deep.txt");
   writeFile(nested, "nested activity");

   // Drain the event pump using a sentinel file in tempDir_ as positive
   // evidence: once we see its FileAdded, anything FSEvents was going to
   // deliver for the nested write has already passed through. A fixed sleep
   // here would let a delayed callback land just after the sleep elapses and
   // pass the assertion spuriously.
   FilePath sentinel = tempDir_.completeChildPath("sentinel.txt");
   writeFile(sentinel, "drain");
   ASSERT_TRUE(waitFor([&] {
      return hasEventFor(state, system::FileChangeEvent::FileAdded,
                         sentinel.getAbsolutePath());
   }));

   for (const auto& event : state.events)
   {
      EXPECT_NE(event.fileInfo().absolutePath(), nested.getAbsolutePath())
         << "Unexpected event for nested file";
   }

   stopMonitor(handle, &state);
}

TEST_F(FileMonitorTest, NonRecursiveAppliesUserFilter)
{
   // Filter rejects *.tmp; the .txt sibling should still pass through. The
   // filter path was previously untested (startMonitor's default left filter
   // empty), so a polarity inversion here would silently expose hidden files.
   auto filter = [](const FileInfo& info) -> bool {
      return !boost::algorithm::ends_with(info.absolutePath(), ".tmp");
   };

   CallbackState state;
   auto handle = startMonitor(tempDir_, &state, filter);
   state.events.clear();

   FilePath rejected = tempDir_.completeChildPath("ignored.tmp");
   FilePath accepted = tempDir_.completeChildPath("kept.txt");
   writeFile(rejected, "filtered out");
   writeFile(accepted, "filtered in");

   ASSERT_TRUE(waitFor([&] {
      return hasEventFor(state, system::FileChangeEvent::FileAdded,
                         accepted.getAbsolutePath());
   }));

   for (const auto& event : state.events)
   {
      EXPECT_NE(event.fileInfo().absolutePath(), rejected.getAbsolutePath())
         << "Filter should have suppressed the .tmp event";
   }

   stopMonitor(handle, &state);
}

TEST_F(FileMonitorTest, NonRecursiveTreatsSymlinksLiterally)
{
   // readFileInfoLStat documents that symlink parity with PosixFileScanner
   // (lstat semantics, no target follow) is what keeps the tree consistent
   // with subsequent event-time lookups. A future "simplify to stat()"
   // refactor would diverge: the initial tree would carry the link's size
   // and mtime, while events for that path would carry the target's, and
   // FileInfo::operator== would emit a spurious FileModified for every
   // symlink whose target moved. Pin the expected behavior here.
   FilePath external = FilePath("/tmp").completeChildPath(
      "rstudio-file-monitor-target-" + system::generateUuid(false) + ".txt");
   writeFile(external, "outside");
   auto cleanupExternal = [&]() { external.removeIfExists(); };

   FilePath link = tempDir_.completeChildPath("link.txt");
   ASSERT_EQ(0, ::symlink(external.getAbsolutePath().c_str(),
                          link.getAbsolutePath().c_str()))
      << "symlink() failed: errno=" << errno;

   CallbackState state;
   auto handle = startMonitor(tempDir_, &state);
   state.events.clear();

   // Touch the target. The link's lstat info (size = link path length,
   // mtime = link creation time) is unchanged, so no FileModified for the
   // link should arrive. Use a drain sentinel rather than a fixed sleep so
   // a delayed callback can't slip past.
   writeFile(external, "outside, modified");
   FilePath sentinel = tempDir_.completeChildPath("sentinel.txt");
   writeFile(sentinel, "drain");
   ASSERT_TRUE(waitFor([&] {
      return hasEventFor(state, system::FileChangeEvent::FileAdded,
                         sentinel.getAbsolutePath());
   }));

   for (const auto& event : state.events)
   {
      if (event.fileInfo().absolutePath() == link.getAbsolutePath())
      {
         EXPECT_NE(event.type(), system::FileChangeEvent::FileModified)
            << "spurious FileModified for symlink whose target changed";
      }
   }

   stopMonitor(handle, &state);
   cleanupExternal();
}

TEST_F(FileMonitorTest, NonRecursiveDetectsSubdirectoryCreatedAfterStart)
{
   // The dynamic case is distinct from the pre-existing-subdir case covered
   // by NonRecursiveIgnoresSubtreeChanges: the subdir creation itself is a
   // top-level FileAdded that must surface as a directory, and subsequent
   // activity inside it must still be filtered (non-recursive semantics).
   CallbackState state;
   auto handle = startMonitor(tempDir_, &state);
   state.events.clear();

   FilePath subDir = tempDir_.completeChildPath("late-subdir");
   ASSERT_FALSE(subDir.ensureDirectory());

   ASSERT_TRUE(waitFor([&] {
      return hasEventFor(state, system::FileChangeEvent::FileAdded,
                         subDir.getAbsolutePath());
   }));

   // The FileAdded should carry isDirectory=true; without that flag the
   // FilesPane would render this as a regular file.
   bool foundDirectoryEntry = false;
   for (const auto& event : state.events)
   {
      if (event.fileInfo().absolutePath() == subDir.getAbsolutePath())
      {
         EXPECT_TRUE(event.fileInfo().isDirectory());
         foundDirectoryEntry = true;
      }
   }
   EXPECT_TRUE(foundDirectoryEntry);

   // Nested activity must remain filtered. Drain with a sentinel so the
   // assertion isn't a thinly-veiled race.
   FilePath nested = subDir.completeChildPath("deep.txt");
   writeFile(nested, "nested after start");

   FilePath sentinel = tempDir_.completeChildPath("sentinel.txt");
   writeFile(sentinel, "drain");
   ASSERT_TRUE(waitFor([&] {
      return hasEventFor(state, system::FileChangeEvent::FileAdded,
                         sentinel.getAbsolutePath());
   }));

   for (const auto& event : state.events)
   {
      EXPECT_NE(event.fileInfo().absolutePath(), nested.getAbsolutePath())
         << "Unexpected event for nested file under late-created subdir";
   }

   stopMonitor(handle, &state);
}

} // namespace tests
} // namespace core
} // namespace rstudio

#endif // __APPLE__
