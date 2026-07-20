/*
 * SessionPackageProvidedExtensionTests.cpp
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

#include <gtest/gtest.h>

#include <boost/date_time/posix_time/posix_time.hpp>
#include <boost/function.hpp>
#include <boost/make_shared.hpp>
#include <boost/thread.hpp>

#include <core/Algorithm.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

#include <r/RExec.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionPackageProvidedExtension.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace ppe {
namespace tests {

namespace {

class TestWorker : public Worker
{
public:
   TestWorker() : Worker() {}
   explicit TestWorker(const std::string& resource) : Worker(resource) {}

   void onIndexingStarted() override
   {
      started++;
   }

   void onWork(const std::string& pkgName, const FilePath& resourcePath) override
   {
      work.push_back(std::make_pair(pkgName, resourcePath));
   }

   void onIndexingCompleted(json::Object* pPayload) override
   {
      completed++;
   }

   bool sawPackage(const std::string& pkgName)
   {
      for (const auto& entry : work)
      {
         if (entry.first == pkgName)
            return true;
      }
      return false;
   }

   int started = 0;
   int completed = 0;
   std::vector<std::pair<std::string, FilePath>> work;
};

// The library scan runs on a background thread and workers are invoked via
// scheduled commands on the main thread; the test harness has no scheduled
// command pump, so pump manually while waiting.
bool waitFor(boost::function<bool()> condition)
{
   auto deadline = boost::posix_time::microsec_clock::universal_time() +
         boost::posix_time::seconds(30);

   while (boost::posix_time::microsec_clock::universal_time() < deadline)
   {
      if (condition())
         return true;

      module_context::onBackgroundProcessing(true);
      boost::this_thread::sleep(boost::posix_time::milliseconds(20));
   }

   return condition();
}

class ScopedFakeLibrary
{
public:
   ScopedFakeLibrary()
   {
      libPath_ = module_context::tempFile("ppe-test-library-", "dir");

      // the library must exist before .libPaths() is set, as R silently
      // drops non-existent directories from the library paths
      Error error = libPath_.ensureDirectory();
      EXPECT_FALSE(error);

      // use the same normalization R applies to library paths, so that
      // paths reported by the indexer compare equal (e.g. on macOS, where
      // the temporary directory lives behind a /var -> /private/var symlink)
      std::string normalized;
      error = r::exec::RFunction("normalizePath")
            .addParam(libPath_.getAbsolutePath())
            .call(&normalized);
      EXPECT_FALSE(error);
      libPath_ = FilePath(normalized);

      error = r::exec::RFunction(".libPaths").call(&oldLibPaths_);
      EXPECT_FALSE(error);

      std::vector<std::string> newLibPaths;
      newLibPaths.push_back(libPath_.getAbsolutePath());
      newLibPaths.insert(newLibPaths.end(), oldLibPaths_.begin(), oldLibPaths_.end());

      error = r::exec::RFunction(".libPaths").addParam(newLibPaths).call();
      EXPECT_FALSE(error);
   }

   ~ScopedFakeLibrary()
   {
      Error error = r::exec::RFunction(".libPaths").addParam(oldLibPaths_).call();
      EXPECT_FALSE(error);

      error = libPath_.removeIfExists();
      EXPECT_FALSE(error);
   }

   void addPackage(const std::string& pkgName, const std::string& resource)
   {
      FilePath pkgPath = libPath_.completeChildPath(pkgName);
      Error error = pkgPath.ensureDirectory();
      EXPECT_FALSE(error);

      if (resource.empty())
         return;

      FilePath resourcePath = pkgPath.completeChildPath(resource);
      error = resourcePath.getParent().ensureDirectory();
      EXPECT_FALSE(error);
      error = resourcePath.ensureFile();
      EXPECT_FALSE(error);
   }

   const FilePath& path() { return libPath_; }

private:
   FilePath libPath_;
   std::vector<std::string> oldLibPaths_;
};

} // end anonymous namespace

TEST(SessionPPETest, IndexerInvokesWorkersAfterBackgroundScan)
{
   ScopedFakeLibrary library;
   library.addPackage("ppe.test.with.resource", "rstudio/ppe-test-resource.dcf");
   library.addPackage("ppe.test.without.resource", "");

   auto pResourceWorker = boost::make_shared<TestWorker>("rstudio/ppe-test-resource.dcf");
   auto pPackageWorker = boost::make_shared<TestWorker>();

   Indexer indexer;
   indexer.addWorker(pResourceWorker);
   indexer.addWorker(pPackageWorker);

   indexer.start();
   EXPECT_TRUE(indexer.running());

   ASSERT_TRUE(waitFor([&]() { return pResourceWorker->completed == 1; }));
   EXPECT_FALSE(indexer.running());

   // the resource worker should be invoked only for the package providing
   // the resource, with the resolved resource path
   EXPECT_EQ(1, pResourceWorker->started);
   ASSERT_EQ(1u, pResourceWorker->work.size());
   EXPECT_EQ("ppe.test.with.resource", pResourceWorker->work[0].first);
   EXPECT_EQ(
            library.path().completeChildPath("ppe.test.with.resource/rstudio/ppe-test-resource.dcf"),
            pResourceWorker->work[0].second);

   // a worker with an empty resource path should see every package, receiving
   // the package directory itself
   EXPECT_EQ(1, pPackageWorker->completed);
   EXPECT_TRUE(pPackageWorker->sawPackage("ppe.test.with.resource"));
   EXPECT_TRUE(pPackageWorker->sawPackage("ppe.test.without.resource"));
   EXPECT_TRUE(core::algorithm::contains(
                  pPackageWorker->work,
                  std::make_pair(
                     std::string("ppe.test.without.resource"),
                     library.path().completeChildPath("ppe.test.without.resource"))));

   // the discovered package directories should be published to workers
   EXPECT_TRUE(core::algorithm::contains(
                  indexer.packageDirs(),
                  library.path().completeChildPath("ppe.test.with.resource")));
}

TEST(SessionPPETest, IndexerCoalescesConcurrentStartRequests)
{
   ScopedFakeLibrary library;
   library.addPackage("ppe.test.coalesce", "rstudio/ppe-test-coalesce.dcf");

   auto pWorker = boost::make_shared<TestWorker>("rstudio/ppe-test-coalesce.dcf");

   Indexer indexer;
   indexer.addWorker(pWorker);

   // request an index, then request two more while the first is in flight;
   // the extra requests should coalesce into a single follow-up pass
   indexer.start();
   indexer.start();
   indexer.start();

   ASSERT_TRUE(waitFor([&]() { return pWorker->completed == 2; }));
   EXPECT_FALSE(indexer.running());

   EXPECT_EQ(2, pWorker->started);
   EXPECT_EQ(2u, pWorker->work.size());
}

} // end namespace tests
} // end namespace ppe
} // end namespace modules
} // end namespace session
} // end namespace rstudio
