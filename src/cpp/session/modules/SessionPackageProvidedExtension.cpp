/*
 * SessionPackageProvidedExtension.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

#include <boost/regex.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>

#include <core/Algorithm.hpp>
#include <core/Exec.hpp>
#include <core/FileSerializer.hpp>
#include <core/text/DcfParser.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace ppe {

Error parseDcfResourceFile(
      const FilePath& resourcePath,
      boost::function<Error(const std::map<std::string, std::string>&)> callback)
{
   Error error;

   // read dcf contents
   std::string contents;
   error = core::readStringFromFile(resourcePath, &contents, string_utils::LineEndingPosix);
   if (error)
      return error;

   // attempt to parse as DCF -- multiple newlines used to separate records
   try
   {
      boost::regex reSeparator("\\n{2,}");
      boost::sregex_token_iterator it(contents.begin(), contents.end(), reSeparator, -1);
      boost::sregex_token_iterator end;

      for (; it != end; ++it)
      {
         // invoke parser on current record
         std::map<std::string, std::string> fields;
         std::string errorMessage;
         error = text::parseDcfFile(*it, true, &fields, &errorMessage);
         if (error)
            return error;

         // invoke callback on parsed dcf fields
         error = callback(fields);
         if (error)
            return error;
      }
   }
   CATCH_UNEXPECTED_EXCEPTION;
   
   return Success();
}

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
            true);
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
   std::string pkgName = pkgPath.getFilename();
   for (boost::shared_ptr<Worker> pWorker : workers_)
   {
      FilePath resourcePath = pkgPath.completeChildPath(pWorker->resourcePath());
      if (!resourcePath.exists())
         continue;
      
      try
      {
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
   for (const core::FilePath& libPath : libPaths)
   {
      if (!libPath.exists())
         continue;

      std::vector<core::FilePath> pkgPaths;
      core::Error error = libPath.getChildren(pkgPaths);
      if (error)
         LOG_ERROR(error);

      pkgDirs_.insert(
               pkgDirs_.end(),
               pkgPaths.begin(),
               pkgPaths.end());
   }
   n_ = pkgDirs_.size();
   
   for (boost::shared_ptr<Worker> pWorker : workers_)
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
   payload_.clear();
   
   for (boost::shared_ptr<Worker> pWorker : workers_)
   {
      try
      {
         pWorker->onIndexingCompleted(&payload_);
      }
      CATCH_UNEXPECTED_EXCEPTION
   }
   
   ClientEvent event(
            client_events::kPackageExtensionIndexingCompleted,
            payload_);
   
   module_context::enqueClientEvent(event);
}

Indexer& indexer()
{
   static Indexer instance;
   return instance;
}

namespace {

void reindex()
{
   indexer().start();
}

void reindexDeferred()
{
   module_context::scheduleDelayedWork(
            boost::posix_time::seconds(1),
            boost::bind(reindex),
            true);
}

void onDeferredInit(bool)
{
   if (module_context::disablePackages())
      return;
   
   reindexDeferred();
}

void onConsoleInput(const std::string& input)
{
   if (module_context::disablePackages())
      return;
   
   static const char* const commands[] = {
      "devtools::install_",
      "devtools::load_all",
      "install.packages",
      "install_github",
      "load_all",
      "pak::pkg_install",
      "pak::pkg_remove",
      "pkg_install",
      "pkg_remove",
      "remotes::install_",
      "remove.packages",
      "renv::install",
      "renv::rebuild",
      "renv::remove",
      "renv::restore",
      "utils::install.packages",
      "utils::remove.packages",
   };
   
   std::string inputTrimmed = boost::algorithm::trim_copy(input);
   for (const char* command : commands)
   {
      if (boost::algorithm::starts_with(inputTrimmed, command))
      {
         return reindexDeferred();
      }
   }
}

void onLibPathsChanged(const std::vector<std::string>& libPaths)
{
   if (module_context::disablePackages())
      return;
   
   reindexDeferred();
}

} // end anonymous namespace

Error initialize()
{
   using namespace module_context;
   using boost::bind;
   
   events().onDeferredInit.connect(onDeferredInit);
   events().onConsoleInput.connect(onConsoleInput);
   events().onLibPathsChanged.connect(onLibPathsChanged);
   
   return Success();
}

} // end namespace ppe
} // end namespace modules
} // end namespace session
} // end namespace rstudio
