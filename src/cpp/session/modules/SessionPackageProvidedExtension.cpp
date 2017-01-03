/*
 * SessionPackageProvidedExtension.cpp
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <session/SessionPackageProvidedExtension.hpp>

#include <boost/foreach.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>

#include <core/Algorithm.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace ppe {

Indexer::Indexer() : index_(0), n_(0), running_(false) {}

void Indexer::addWorker(boost::shared_ptr<Worker> pWorker)
{
   workers_.push_back(pWorker);
}

void Indexer::removeWorker(boost::shared_ptr<Worker> pWorker)
{
   core::algorithm::expel(workers_, pWorker);
}

void Indexer::start()
{
   if (running_)
      return;

   running_ = true;
   beginIndexing();
   module_context::scheduleIncrementalWork(
            boost::posix_time::milliseconds(300),
            boost::posix_time::milliseconds(20),
            boost::bind(&Indexer::work, this),
            false);
}

bool Indexer::work()
{
   // check whether we've run out of work items
   if (index_ == n_)
   {
      endIndexing();
      return false;
   }

   std::size_t index = index_++;

   // invoke workers with package name + path
   FilePath pkgPath = pkgDirs_[index];
   std::string pkgName = pkgPath.filename();
   BOOST_FOREACH(boost::shared_ptr<Worker> pWorker, workers_)
   {
      try
      {
         FilePath resourcePath = pkgPath.childPath(pWorker->resourcePath());
         pWorker->onWork(pkgName, resourcePath);
      }
      CATCH_UNEXPECTED_EXCEPTION
   }
   return true;
}

void Indexer::beginIndexing()
{
   // reset indexer state
   pkgDirs_.clear();
   index_ = 0;

   // discover packages available on the current library paths
   std::vector<core::FilePath> libPaths = module_context::getLibPaths();
   BOOST_FOREACH(const core::FilePath& libPath, libPaths)
   {
      if (!libPath.exists())
         continue;

      std::vector<core::FilePath> pkgPaths;
      core::Error error = libPath.children(&pkgPaths);
      if (error)
         LOG_ERROR(error);

      pkgDirs_.insert(
               pkgDirs_.end(),
               pkgPaths.begin(),
               pkgPaths.end());
   }
   n_ = pkgDirs_.size();
   
   BOOST_FOREACH(boost::shared_ptr<Worker> pWorker, workers_)
   {
      try
      {
         pWorker->onIndexingStarted();
      }
      CATCH_UNEXPECTED_EXCEPTION
   }
}

void Indexer::endIndexing()
{
   running_ = false;
   BOOST_FOREACH(boost::shared_ptr<Worker> pWorker, workers_)
   {
      try
      {
         pWorker->onIndexingCompleted();
      }
      CATCH_UNEXPECTED_EXCEPTION
   }
}

namespace {

Indexer& indexer()
{
   static Indexer instance;
   return instance;
}

void reindex()
{
   if (module_context::disablePackages())
      return;
   
   indexer().start();
}

void onDeferredInit(bool)
{
   reindex();
}

void onConsoleInput(const std::string& input)
{
   if (module_context::disablePackages())
      return;
   
   const char* const commands[] = {
      "install.packages",
      "utils::install.packages",
      "remove.packages",
      "utils::remove.packages",
      "install_github",
      "devtools::install_github",
      "load_all",
      "devtools::load_all",
   };
   
   std::string inputTrimmed = boost::algorithm::trim_copy(input);
   BOOST_FOREACH(const char* command, commands)
   {
      if (boost::algorithm::starts_with(inputTrimmed, command))
      {
         // we need to give R a chance to actually process the package library
         // mutating command before we update the index. schedule delayed work
         // with idleOnly = true so that it waits until the user has returned
         // to the R prompt
         module_context::scheduleDelayedWork(
                  boost::posix_time::seconds(1),
                  boost::bind(reindex),
                  true);
      }
   }
}

} // end anonymous namespace

Error initialize()
{
   using namespace module_context;
   using boost::bind;
   
   events().onDeferredInit.connect(onDeferredInit);
   events().onConsoleInput.connect(onConsoleInput);
   
   return Success();
}

} // end namespace ppe
} // end namespace modules
} // end namespace session
} // end namespace rstudio
