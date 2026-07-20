/*
 * SessionPackageProvidedExtension.hpp
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

#ifndef SESSION_MODULES_PACKAGE_PROVIDED_EXTENSION_HPP
#define SESSION_MODULES_PACKAGE_PROVIDED_EXTENSION_HPP

#include <string>
#include <vector>

#include <shared_core/FilePath.hpp>
#include <shared_core/json/Json.hpp>

#include <boost/function.hpp>
#include <boost/noncopyable.hpp>
#include <boost/shared_ptr.hpp>

namespace rstudio {
namespace core {

class Error;

} // end namespace core
} // end namespace rstudio

namespace rstudio {
namespace session {
namespace modules {
namespace ppe {

core::Error parseDcfResourceFile(
      const core::FilePath& resourcePath,
      boost::function<core::Error(const std::map<std::string, std::string>&)> callback);

class Worker : boost::noncopyable
{
public:
   virtual void onIndexingStarted() = 0;
   virtual void onIndexingCompleted(core::json::Object* pPayload) = 0;
   virtual void onWork(const std::string& pkgName, const core::FilePath& resourcePath) = 0;
   virtual ~Worker() {}
   
public:
   
   Worker()
   {
   }

   Worker(const std::string& resourcePath)
      : resourcePath_(resourcePath)
   {
   }

   const std::string& resourcePath() const { return resourcePath_; }
   
private:
   std::string resourcePath_;
};

// A unit of indexing work produced by the background library scan: a package
// resource matching a worker's resourcePath() (or the package directory
// itself, for workers with an empty resourcePath).
struct IndexEntry
{
   std::string pkgName;
   std::string resource;
   core::FilePath resourcePath;
};

struct IndexScan;

class Indexer : boost::noncopyable
{
public:
   explicit Indexer();
   virtual ~Indexer() {}

public:
   void addWorker(boost::shared_ptr<Worker> pWorker);
   void removeWorker(boost::shared_ptr<Worker> pWorker);

public:
   void start();
   bool running() { return running_; }
   core::json::Object getPayload() { return payload_; }

   // package directories discovered by the most recent library scan;
   // stable from the first onWork() until the next scan completes
   const std::vector<core::FilePath>& packageDirs() { return pkgDirs_; }

private:
   void beginIndexing();
   bool checkScan(boost::shared_ptr<IndexScan> pScan);
   bool work();
   void endIndexing();

private:
   std::vector<boost::shared_ptr<Worker> > workers_;
   std::vector<core::FilePath> pkgDirs_;
   std::vector<IndexEntry> entries_;
   core::json::Object payload_;

   std::size_t index_;
   std::size_t n_;
   bool running_;
   bool pending_;
};

Indexer& indexer();
core::Error initialize();

} // end namespace ppe
} // end namespace modules
} // end namespace session
} // end namespace rstudio

#endif /* SESSION_MODULES_PACKAGE_PROVIDED_EXTENSION_HPP */
