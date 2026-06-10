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

#include <algorithm>
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
   std::vector<std::string> registeredPaths;
   std::vector<system::FileChangeEvent> events;
};

void onRegistered(CallbackState* pState,
                  system::file_monitor::Handle handle,
                  const tree<FileInfo>& files)
{
   pState->handle = handle;
   for (auto it = files.begin(); it != files.end(); ++it)
      pState->registeredPaths.push_back(it->absolutePath());
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

// Returns true on success. Tests should ASSERT on the result -- a void helper
// with ASSERT_TRUE inside would mark the test as failed but continue, leaving
// the actual failure buried under a downstream waitFor timeout.
bool writeFile(const FilePath& path, const std::string& contents)
{
   std::ofstream out(path.getAbsolutePath());
   if (!out.is_open())
      return false;
   out << contents;
   return out.good();
}

// Write a sentinel file in `dir` and block until its FileAdded event arrives.
// Use as a drain: any event FSEvents was going to deliver for prior writes has
// passed through by the time the sentinel's event lands -- safer than a fixed
// sleep, which can be slipped by a delayed callback. Returns false if the
// sentinel write or its event never lands.
bool drainViaSentinel(const FilePath& dir, CallbackState* pState)
{
   FilePath sentinel = dir.completeChildPath("__file_monitor_test_sentinel__");
   if (!writeFile(sentinel, "drain"))
      return false;
   return waitFor([&] {
      return hasEventFor(*pState, system::FileChangeEvent::FileAdded,
                         sentinel.getAbsolutePath());
   });
}

system::file_monitor::Handle startMonitor(
      const FilePath& dir,
      CallbackState* pState,
      boost::function<bool(const FileInfo&)> filter =
         boost::function<bool(const FileInfo&)>(),
      bool recursive = false)
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
      recursive,
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

      // Use the canonical path so expected paths in most tests are identical
      // to what FSEvents reports (e.g. /private/tmp/... rather than
      // /tmp/...). registerMonitor itself preserves whatever path form the
      // caller passes; the Symlinked* tests below cover that mapping.
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
   ASSERT_TRUE(writeFile(child, "hello"));

   ASSERT_TRUE(waitFor([&] {
      return hasEventFor(state, system::FileChangeEvent::FileAdded,
                         child.getAbsolutePath());
   }));

   stopMonitor(handle, &state);
}

TEST_F(FileMonitorTest, NonRecursiveDetectsFileModified)
{
   FilePath child = tempDir_.completeChildPath("modified.txt");
   ASSERT_TRUE(writeFile(child, "initial"));
   // FileInfo stores st_mtimespec.tv_sec (truncated to whole seconds), so
   // back-to-back writes within the same wall-clock second produce identical
   // FileInfos and processFileAdded suppresses the modify. Sleep past the
   // second boundary so the next write produces an observably newer mtime.
   std::this_thread::sleep_for(std::chrono::milliseconds(1100));

   CallbackState state;
   auto handle = startMonitor(tempDir_, &state);
   state.events.clear();

   ASSERT_TRUE(writeFile(child, "updated contents that are longer than the initial"));

   ASSERT_TRUE(waitFor([&] {
      return hasEventFor(state, system::FileChangeEvent::FileModified,
                         child.getAbsolutePath());
   }));

   stopMonitor(handle, &state);
}

TEST_F(FileMonitorTest, NonRecursiveDetectsFileRemoved)
{
   FilePath child = tempDir_.completeChildPath("doomed.txt");
   ASSERT_TRUE(writeFile(child, "soon to be gone"));

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
   ASSERT_TRUE(writeFile(nested, "nested activity"));

   ASSERT_TRUE(drainViaSentinel(tempDir_, &state));

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
   ASSERT_TRUE(writeFile(rejected, "filtered out"));
   ASSERT_TRUE(writeFile(accepted, "filtered in"));

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

// Documents (does not strictly enforce) the lstat-vs-stat parity invariant
// from readFileInfoLStat. FSEvents only fires for paths under the watched
// root, so a target file living outside the watched directory can't trigger
// an event for the symlink path -- the inner loop has nothing to iterate on
// for the link, regardless of whether the (hypothetical) code path used
// lstat or stat. A future "simplify to stat()" refactor would not be caught
// by this assertion. We keep it as documentation of the expected behavior;
// the lstat parity guarantee proper lives in PosixFileScanner's tests.
TEST_F(FileMonitorTest, SymlinkToExternalTargetEmitsNoEventOnTargetChange)
{
   // RAII cleanup so an ASSERT failure mid-test doesn't leak the external
   // file (a bare lambda at the bottom of the body would be skipped).
   struct ExternalFileGuard
   {
      FilePath path;
      ~ExternalFileGuard() { path.removeIfExists(); }
   };

   ExternalFileGuard external{ FilePath("/tmp").completeChildPath(
      "rstudio-file-monitor-target-" + system::generateUuid(false) + ".txt") };
   ASSERT_TRUE(writeFile(external.path, "outside"));

   FilePath link = tempDir_.completeChildPath("link.txt");
   ASSERT_EQ(0, ::symlink(external.path.getAbsolutePath().c_str(),
                          link.getAbsolutePath().c_str()))
      << "symlink() failed: errno=" << errno;

   CallbackState state;
   auto handle = startMonitor(tempDir_, &state);
   state.events.clear();

   // Touch the target (outside tempDir_). FSEvents does not see this and
   // does not call our callback for the link path. The drain sentinel
   // exists only to give the event pump a known endpoint.
   ASSERT_TRUE(writeFile(external.path, "outside, modified"));
   ASSERT_TRUE(drainViaSentinel(tempDir_, &state));

   for (const auto& event : state.events)
   {
      if (event.fileInfo().absolutePath() == link.getAbsolutePath())
      {
         EXPECT_NE(event.type(), system::FileChangeEvent::FileModified)
            << "spurious FileModified for symlink whose target changed";
      }
   }

   stopMonitor(handle, &state);
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

   // Nested activity must remain filtered.
   FilePath nested = subDir.completeChildPath("deep.txt");
   ASSERT_TRUE(writeFile(nested, "nested after start"));
   ASSERT_TRUE(drainViaSentinel(tempDir_, &state));

   for (const auto& event : state.events)
   {
      EXPECT_NE(event.fileInfo().absolutePath(), nested.getAbsolutePath())
         << "Unexpected event for nested file under late-created subdir";
   }

   stopMonitor(handle, &state);
}

// Repro for #17909: watch a directory through a symlink. The registration
// snapshot and all change events must come back in the link's path form --
// the Files pane diffs the snapshot against a listing taken via the link
// path, and the GWT client drops events whose parent doesn't match the
// displayed directory. Canonical (target) path forms leaking out of the
// monitor empty the pane.
TEST_F(FileMonitorTest, SymlinkedRootReportsRegisteredPathForm)
{
   FilePath target = tempDir_.completeChildPath("real_dir");
   ASSERT_FALSE(target.ensureDirectory());
   ASSERT_TRUE(writeFile(target.completeChildPath("existing.txt"), "pre-existing"));

   FilePath link = tempDir_.completeChildPath("link_dir");
   ASSERT_EQ(0, ::symlink(target.getAbsolutePath().c_str(),
                          link.getAbsolutePath().c_str()))
      << "symlink() failed: errno=" << errno;

   CallbackState state;
   auto handle = startMonitor(link, &state);

   // every path in the registration snapshot uses the link form
   std::string linkPrefix = link.getAbsolutePath();
   ASSERT_FALSE(state.registeredPaths.empty());
   for (const auto& path : state.registeredPaths)
   {
      EXPECT_TRUE(path == linkPrefix ||
                  boost::algorithm::starts_with(path, linkPrefix + "/"))
         << "snapshot path not in link form: " << path;
   }
   EXPECT_TRUE(std::count(state.registeredPaths.begin(),
                          state.registeredPaths.end(),
                          link.completeChildPath("existing.txt").getAbsolutePath()) == 1);

   // change events arrive in link form too, even though FSEvents reports
   // them against the canonical target path
   state.events.clear();
   FilePath added = link.completeChildPath("added.txt");
   ASSERT_TRUE(writeFile(added, "hello"));

   ASSERT_TRUE(waitFor([&] {
      return hasEventFor(state, system::FileChangeEvent::FileAdded,
                         added.getAbsolutePath());
   }));

   ASSERT_FALSE(added.remove());

   ASSERT_TRUE(waitFor([&] {
      return hasEventFor(state, system::FileChangeEvent::FileRemoved,
                         added.getAbsolutePath());
   }));

   for (const auto& event : state.events)
   {
      EXPECT_TRUE(boost::algorithm::starts_with(event.fileInfo().absolutePath(),
                                                linkPrefix + "/"))
         << "event path not in link form: " << event.fileInfo().absolutePath();
   }

   stopMonitor(handle, &state);
}

// The recursive branch goes through directory-granularity events and
// discoverAndProcessFileChanges rather than the per-file path, so the
// canonical-to-registered mapping needs separate coverage there.
TEST_F(FileMonitorTest, SymlinkedRootReportsRegisteredPathFormRecursive)
{
   FilePath target = tempDir_.completeChildPath("real_dir");
   FilePath subDir = target.completeChildPath("subdir");
   ASSERT_FALSE(subDir.ensureDirectory());

   FilePath link = tempDir_.completeChildPath("link_dir");
   ASSERT_EQ(0, ::symlink(target.getAbsolutePath().c_str(),
                          link.getAbsolutePath().c_str()))
      << "symlink() failed: errno=" << errno;

   CallbackState state;
   auto handle = startMonitor(link, &state,
                              boost::function<bool(const FileInfo&)>(),
                              /*recursive=*/true);
   state.events.clear();

   FilePath nested = link.completeChildPath("subdir/nested.txt");
   ASSERT_TRUE(writeFile(nested, "nested"));

   ASSERT_TRUE(waitFor([&] {
      return hasEventFor(state, system::FileChangeEvent::FileAdded,
                         nested.getAbsolutePath());
   }));

   // the recursive branch can also emit events for enclosing directories
   // (e.g. a FileModified for subdir); every event must be in link form
   std::string linkPrefix = link.getAbsolutePath();
   for (const auto& event : state.events)
   {
      EXPECT_TRUE(boost::algorithm::starts_with(event.fileInfo().absolutePath(),
                                                linkPrefix + "/"))
         << "event path not in link form: " << event.fileInfo().absolutePath();
   }

   stopMonitor(handle, &state);
}

} // namespace tests
} // namespace core
} // namespace rstudio

#endif // __APPLE__
