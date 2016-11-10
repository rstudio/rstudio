/*
 * SessionPackageProvidedExtension.hpp
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

#ifndef SESSION_MODULES_PACKAGE_PROVIDED_EXTENSION_HPP
#define SESSION_MODULES_PACKAGE_PROVIDED_EXTENSION_HPP

#include <cstddef>

#include <string>
#include <map>

#include <boost/bind.hpp>
#include <boost/foreach.hpp>
#include <boost/noncopyable.hpp>

#include <core/FilePath.hpp>

#include <session/SessionModuleContext.hpp>

namespace rstudio {
namespace session {
namespace modules {
namespace ppe {

template <typename Resource>
class Indexer : public boost::noncopyable
{
public:
   virtual void onIndexingStarted() = 0;
   virtual void onIndexingCompleted() = 0;
   
   explicit Indexer(const std::string& resourcePath)
      : resourcePath_(resourcePath)
   {
   }
   
   void start()
   {
      start(300, 20);
   }
   
   void start(int initialDurationMs, int incrementalDurationMs)
   {
      // check to see if we already have some work (ie, we've already
      // been initialized)
      if (index_ == n_)
         return;
      
      beginIndexing();
      module_context::scheduleIncrementalWork(
               boost::posix_time::milliseconds(initialDurationMs),
               boost::posix_time::milliseconds(incrementalDurationMs),
               boost::bind(&Indexer::work, this),
               false);
   }
   
   bool work()
   {
      // check whether we've finished indexing
      if (index_ == n_)
      {
         endIndexing();
         return false;
      }
      
      // discover path to package resource
      const core::FilePath& pkgPath = pkgDirs_[index_];
      core::FilePath resourcePath = pkgPath.childPath(resourcePath_);
      std::string pkgName = pkgPath.filename();
      
      // attempt to parse resource file
      std::vector<Resource> items;
      core::Error error = Resource::fromPackageResource(pkgName, resourcePath, &items);
      if (error)
         LOG_ERROR(error);
      
      // cache into package -> resource map
      resources_[pkgName] = items;
      
      return true;
   }
   
   bool running()
   {
      return index_ < n_;
   }
   
private:
   
   void beginIndexing()
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
      
      // store number of work items
      n_ = pkgDirs_.size();
      
      // call subclass method
      onIndexingStarted();
   }
   
   void endIndexing()
   {
      onIndexingCompleted();
   }

   std::string resourcePath_;
   std::map<std::string, std::vector<Resource> > resources_;
   
   // Indexer state
   std::vector<core::FilePath> pkgDirs_;
   std::size_t index_;
   std::size_t n_;
};

core::Error initialize();

} // end namespace ppe
} // end namespace modules
} // end namespace session
} // end namespace rstudio

#endif /* SESSION_MODULES_PACKAGE_PROVIDED_EXTENSION_HPP */
