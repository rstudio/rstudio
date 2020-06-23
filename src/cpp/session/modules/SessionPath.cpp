/*
 * SessionPath.cpp
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

#include "SessionPath.hpp"

#include <string>
#include <vector>

#include <boost/bind.hpp>

#include <core/Algorithm.hpp>
#include <shared_core/Error.hpp>
#include <core/Log.hpp>
#include <shared_core/FilePath.hpp>
#include <core/FileSerializer.hpp>

#include <core/system/System.hpp>
#include <core/system/Environment.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace path {

#ifdef __APPLE__

namespace {

Error readPathsFromFile(const FilePath& filePath,
                        std::vector<std::string>* pPaths)
{
   std::vector<std::string> paths;
   Error error = core::readStringVectorFromFile(filePath, &paths);
   if (error)
   {
      error.addProperty("path-source", filePath.getAbsolutePath());
      return error;
   }

   std::copy(paths.begin(), paths.end(), std::back_inserter(*pPaths));

   return Success();
}

void safeReadPathsFromFile(const FilePath& filePath,
                           std::vector<std::string>* pPaths)
{
   Error error = readPathsFromFile(filePath, pPaths);
   if (error)
      LOG_ERROR(error);
}

void addToPathIfNecessary(
      const std::string& entry,
      std::vector<std::string>* pPathComponents)
{
   // tolerate paths with trailing slashes
   for (std::string item : {entry, entry + "/"})
   {
      auto it = std::find(
               pPathComponents->begin(),
               pPathComponents->end(),
               item);

      // if we find the path component, bail (no need to add to PATH)
      if (it != pPathComponents->end())
         return;
   }

   // failed to find PATH component; add it to the PATH
   pPathComponents->push_back(entry);
}

} // anonymous namespace


Error initialize()
{
   // read /etc/paths
   std::vector<std::string> paths;
   safeReadPathsFromFile(FilePath("/etc/paths"), &paths);

   // read /etc/paths.d/* (once again failure is not fatal as we
   // can fall back to the previous setting)
   FilePath pathsD("/etc/paths.d");
   if (pathsD.exists())
   {
      // enumerate the children
      std::vector<FilePath> pathsDChildren;
      Error error = pathsD.getChildren(pathsDChildren);
      if (error)
         LOG_ERROR(error);

      // collect their paths
      std::for_each(pathsDChildren.begin(),
                    pathsDChildren.end(),
                    boost::bind(safeReadPathsFromFile, _1, &paths));

   }

   // build the PATH
   std::vector<std::string> parts =
         core::algorithm::split(core::system::getenv("PATH"), ":");

   // add in components from paths.d etc.
   std::for_each(paths.begin(),
                 paths.end(),
                 boost::bind(addToPathIfNecessary, _1, &parts));

   // do we need to add /Library/TeX/texbin or /usr/texbin or (sometimes texlive
   // doesn't get this written into /etc/paths.d)
   FilePath libraryTexbinPath("/Library/TeX/texbin");
   if (libraryTexbinPath.exists())
      addToPathIfNecessary(libraryTexbinPath.getAbsolutePath(), &parts);
   FilePath texbinPath("/usr/texbin");
   if (texbinPath.exists())
      addToPathIfNecessary(texbinPath.getAbsolutePath(), &parts);

   // add /opt/local/bin if necessary
   FilePath optLocalBinPath("/opt/local/bin");
   if (optLocalBinPath.exists())
      addToPathIfNecessary(optLocalBinPath.getAbsolutePath(), &parts);

   // set the path
   core::system::setenv("PATH", core::algorithm::join(parts, ":"));

   return Success();

}

#else
Error initialize()
{
   return Success();
}
#endif



   
} // namespace path
} // namespace modules
} // namespace session
} // namespace rstudio

