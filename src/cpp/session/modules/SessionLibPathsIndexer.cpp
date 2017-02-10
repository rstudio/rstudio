/*
 * SessionLibPathsIndexer.cpp
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

#include "SessionLibPathsIndexer.hpp"

#include <vector>
#include <map>

#include <core/Error.hpp>
#include <core/FilePath.hpp>

#include <session/SessionPackageProvidedExtension.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace libpaths {

namespace {

std::vector<FilePath> s_installedPackages_;

class Worker : public ppe::Worker
{
   void onIndexingStarted()
   {
      s_installedPackages_.clear();
   }
   
   void onWork(const std::string& pkgName, const FilePath& pkgPath)
   {
      s_installedPackages_.push_back(pkgPath);
   }
   
   void onIndexingCompleted(json::Object* pPayload)
   {
      // no need to update client state
   }
   
public:
   
   Worker() : ppe::Worker() {}
};

boost::shared_ptr<Worker>& worker()
{
   static boost::shared_ptr<Worker> instance(new Worker);
   return instance;
}

} // end anonymous namespace

const std::vector<FilePath>& getInstalledPackages()
{
   return s_installedPackages_;
}

Error initialize()
{
   ppe::indexer().addWorker(worker());
   return Success();
}

} // end namespace libpaths
} // end namespace modules
} // end namespace session
} // end namespace rstudio
