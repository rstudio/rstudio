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

#include <string>
#include <vector>

#include <core/json/Json.hpp>

#include <boost/function.hpp>
#include <boost/noncopyable.hpp>
#include <boost/shared_ptr.hpp>

namespace rstudio {
namespace core {

class Error;
class FilePath;

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
   
private:
   void beginIndexing();
   bool work();
   void endIndexing();
   
private:
   std::vector<boost::shared_ptr<Worker> > workers_;
   std::vector<core::FilePath> pkgDirs_;
   core::json::Object payload_;
   
   std::size_t index_;
   std::size_t n_;
   bool running_;
};

Indexer& indexer();
core::Error initialize();

} // end namespace ppe
} // end namespace modules
} // end namespace session
} // end namespace rstudio

#endif /* SESSION_MODULES_PACKAGE_PROVIDED_EXTENSION_HPP */
