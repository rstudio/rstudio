/*
 * RSearchPath.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

// NOTE: known "holes" in search path persistence include:
//
//   - package internal state
//   - contents of UserDefinedDatabase objects installed by packages
//

#include "RSearchPath.hpp"

#include <string>
#include <vector>
#include <algorithm>

#include <boost/bind.hpp>
#include <boost/function.hpp>
#include <boost/format.hpp>
#include <boost/lexical_cast.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <core/Log.hpp>
#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/SafeConvert.hpp>
#include <core/FileSerializer.hpp>

#define R_INTERNAL_FUNCTIONS
#include <r/RInternal.hpp>
#include <r/RExec.hpp>
#include <r/RInterface.hpp>

using namespace rstudio::core ;

namespace rstudio {
namespace r {
   
using namespace exec ;
   
namespace session {
namespace search_path {

namespace {   

const char * const kEnvironmentFile = "environment";
const char * const kSearchPathDir = "search_path";
   
const char * const kSearchPathElementsDir = "search_path_elements";
const char * const kPackagePaths = "package_paths";
const char * const kEnvDataDir = "environment_data";

void reportRestoreError(const std::string& context, 
                        const Error& error,
                        const ErrorLocation& location)
{
   // build the message
   std::string message = "Error restoring session data";
   if (!context.empty())
      message += std::string(" (" + context + ")");
   
   // add context to error and log it
   Error restoreError = error ;
   restoreError.addProperty("context", message);
   core::log::logError(restoreError, location);
   
   // notify end-user
   std::string report = message + ": " + error.code().message() + "\n";
   REprintf(report.c_str());
}   
   
Error saveGlobalEnvironmentToFile(const FilePath& environmentFile)
{
   std::string envPath =
            string_utils::utf8ToSystem(environmentFile.absolutePath());
   return executeSafely(boost::bind(R_SaveGlobalEnvToFile, envPath.c_str()));
}
   
Error restoreGlobalEnvironment(const core::FilePath& environmentFile)
{
   // tolerate no environment saved
   if (!environmentFile.exists())
      return Success();
   
   return RFunction("load", environmentFile.absolutePath()).call();
}

bool isPackage(const std::string& elementName, std::string* pPackageName)
{
   std::string packagePrefix("package:");
   if ( boost::algorithm::starts_with(elementName, packagePrefix) && 
        (elementName.size() > packagePrefix.size()) )
   {
      *pPackageName = elementName.substr(packagePrefix.size());
      return true;
   }
   else
   {
      return false;
   }
}
   
bool hasEnvironmentData(const std::string& elementName)
{
   if (boost::algorithm::starts_with(elementName, "package:"))
      return false;
   else if (elementName == "Autoloads")
      return false;
   else if (elementName == "tools:rstudio")
      return false;
   else if (elementName == ".GlobalEnv") // saved and restored separately
      return false;
   else 
      return true;
}

// detach any search path elements which are not contained in the passed list
Error detachSearchPathElementsNotInList(
                       const std::vector<std::string>& searchPathList,
                       const std::vector<std::string>& currentSearchPathList)
{
   // make a copy of the list and sort it so we can use std::set_difference
   std::vector<std::string> sortedSearchPathList = searchPathList;
   std::sort(sortedSearchPathList.begin(), sortedSearchPathList.end());
   
   // make a copy of the current list and sort it as well
   std::vector<std::string> sortedCurrentSearchPathList = currentSearchPathList;
   std::sort(sortedCurrentSearchPathList.begin(), 
             sortedCurrentSearchPathList.end());
   
   // find items in the current list which are NOT in the saved list 
   std::vector<std::string> detachSearchPathList;
   std::set_difference(sortedCurrentSearchPathList.begin(),
                       sortedCurrentSearchPathList.end(),
                       sortedSearchPathList.begin(),
                       sortedSearchPathList.end(),
                       std::back_inserter(detachSearchPathList));
                   
   // detach these items
   for (std::vector<std::string>::const_iterator 
            it = detachSearchPathList.begin();
            it != detachSearchPathList.end();
            ++it)
   {
      // get name of item to detach
      std::string detachItem = *it;
      
      // don't allow detach of core packages
      if ( detachItem == "tools:rstudio" ||
           detachItem == "package:utils" )
      {
         continue;
      }
      
      // do the detach
      boost::format fmt("detach(pos = match(\"%1%\", search()))");
      std::string detach = boost::str(fmt % detachItem);
      Error error = r::exec::executeString(detach);
      if (error)
         reportRestoreError("detaching " + detachItem, error, ERROR_LOCATION);
   }
   
   return Success();
}
   
bool packageIsLoaded(const std::string& packageElementName, 
                     const std::vector<std::string>& searchPathList)
{
   return std::find(searchPathList.begin(),
                    searchPathList.end(),
                    packageElementName) != searchPathList.end();
   
}
   
void loadPackage(const std::string& packageName, const std::string& path)
{
   // calculate the lib
   std::string lib;
   if (!path.empty())
      lib = string_utils::utf8ToSystem(FilePath(path).parent().absolutePath());

   Error error = r::exec::RFunction(".rs.loadPackage", packageName, lib).call();
   if (error)
   {
      reportRestoreError("loading package " + packageName,
                         error,
                         ERROR_LOCATION);
   }
}
   
void attachEnvironmentData(const FilePath& dataFilePath, 
                           const std::string& name)
{
   if (dataFilePath.exists())
   {
      Error error = r::exec::RFunction(".rs.attachDataFile",
                                       dataFilePath.absolutePath(),
                                       name).call();
      
      if (error)
      {
         reportRestoreError("attaching search path element "+ name,
                            error,
                            ERROR_LOCATION);
      }
   }
   else
   {
      LOG_ERROR_MESSAGE("environment data file not found: " +
                        dataFilePath.absolutePath());
   }
}


} // anonymous namespace
   

Error save(const FilePath& statePath)
{
   // save the global environment
   FilePath environmentFile = statePath.complete(kEnvironmentFile);
   Error error = saveGlobalEnvironmentToFile(environmentFile);
   if (error)
      return error;
   
   // reset the contents of the search path dir
   FilePath searchPathDir = statePath.complete(kSearchPathDir);
   error = searchPathDir.resetDirectory();
   if (error)
      return error ;
   
   // create environment data subdirectory
   FilePath environmentDataPath = searchPathDir.complete(kEnvDataDir);
   error = environmentDataPath.ensureDirectory();
   if (error)
      return error;
   
   // iterate throught the search path (build a list as we go). set 
   // .GlobalEnv and package:base as bookends of the list (note this code
   // is based on the implementation of do_search)
   std::vector<std::string> searchPathElements;
   searchPathElements.push_back(".GlobalEnv");
   std::map<std::string,std::string> packagePaths;
   for (SEXP envSEXP = ENCLOS(R_GlobalEnv); 
        envSEXP != R_BaseEnv ; 
        envSEXP = ENCLOS(envSEXP))
   {
      // screen out UserDefinedDatabase elements (attempting to perisist
      // a UserDefinedDatabase caused mischief in at least one case (e.g. see
      // RProtoBuf:DescriptorPool) so we exclude it globally.
      if (r::sexp::classOf(envSEXP) == "UserDefinedDatabase")
         continue;

      // get the name of the search path element and add it to our list
      SEXP nameSEXP = Rf_getAttrib(envSEXP, Rf_install("name"));
      std::string elementName;
      if (!Rf_isString(nameSEXP) || Rf_length(nameSEXP) < 1)
         elementName = "(unknown)";
      else
         elementName = r::sexp::asString(nameSEXP);

      // if this is a package also save it's path
      if (boost::algorithm::starts_with(elementName, "package:"))
      {
         std::string name = boost::algorithm::replace_first_copy(elementName,
                                                                 "package:",
                                                                 "");
         std::string path;
         Error error = r::exec::RFunction(".rs.pathPackage", name).call(&path);
         if (error)
            LOG_ERROR(error);

         if (!path.empty())
         {
            path = core::string_utils::systemToUtf8(path);
            packagePaths[name] = path;
         }
      }

      searchPathElements.push_back(elementName);

      // save the environment's data if necessary
      if (hasEnvironmentData(elementName))
      {
         // determine file path (index of item within list)
         std::string itemIndex = safe_convert::numberToString(
                                                searchPathElements.size()-1);
         FilePath dataFilePath = environmentDataPath.complete(itemIndex);
         
         // save the environment
         Error error = r::exec::RFunction(".rs.saveEnvironment",
                                          envSEXP,
                                          dataFilePath.absolutePath()).call();
         if (error)
            return error;
      }
   }
   searchPathElements.push_back("package:base");
   
   // save the search path list
   FilePath elementsPath = searchPathDir.complete(kSearchPathElementsDir);
   error =  writeStringVectorToFile(elementsPath, searchPathElements);
   if (error)
      return error;

   // save the package paths list
   FilePath packagePathsFile = searchPathDir.complete(kPackagePaths);
   return writeStringMapToFile(packagePathsFile, packagePaths);
}


Error saveGlobalEnvironment(const FilePath& statePath)
{
   FilePath environmentFile = statePath.complete(kEnvironmentFile);
   return saveGlobalEnvironmentToFile(environmentFile);
}

Error restoreSearchPath(const FilePath& statePath)
{
   Error error;
   
   // attempt to restore the search path if one has been saved
   FilePath searchPathDir = statePath.complete(kSearchPathDir);
   if (!searchPathDir.exists())
      return Success();
   
   // read the saved list
   std::vector<std::string> savedSearchPathList;
   FilePath elementsPath = searchPathDir.complete(kSearchPathElementsDir);
   error = readStringVectorFromFile(elementsPath, &savedSearchPathList);
   if (error)
      return error;

   // read the package paths list
   std::map<std::string,std::string> packagePaths;
   FilePath packagePathsFile = searchPathDir.complete(kPackagePaths);
   if (packagePathsFile.exists())
   {
      error = readStringMapFromFile(packagePathsFile, &packagePaths);
      if (error)
         return error;
   }

   // get the current search path
   std::vector<std::string> currentSearchPathList;
   error = r::exec::RFunction("search").call(&currentSearchPathList);
   if (error)
      return error;
   
   // detach any items in the current list which aren't in the saved list
   error = detachSearchPathElementsNotInList(savedSearchPathList,
                                             currentSearchPathList);
   if (error)
      return error;
   
   // iterate though the saved list in reverse, loading packages and 
   // environments saved in external data files as necessary. note that 
   // this excludes the first and last entries in the list (.GlobalEnv and
   // package:base respectively)
   FilePath environmentDataPath = searchPathDir.complete(kEnvDataDir);
   for (int i = (savedSearchPathList.size() - 2); i > 0; i--)
   {
      // get the path element
      std::string pathElement = savedSearchPathList[i];
      
      // if it is a package then load it if it is not already loaded
      std::string packageName;
      if ( isPackage(pathElement, &packageName) )
      {
         if ( !packageIsLoaded(packageName, currentSearchPathList) )
            loadPackage(packageName, packagePaths[packageName]);
      }
      
      // else if it has external environment data then load it
      else if (hasEnvironmentData(pathElement))
      {
         std::string itemIndex = safe_convert::numberToString(i);
         FilePath dataFilePath = environmentDataPath.complete(itemIndex);
         attachEnvironmentData(dataFilePath, pathElement);
      }
      
      else
      {
         // it must be "tools:rstudio" or "Autoloads", do nothing
      }
   }
   
   return Success();
}

Error restore(const FilePath& statePath, bool isCompatibleSessionState)
{
   // restore global environment
   FilePath environmentFile = statePath.complete(kEnvironmentFile);
   Error error = restoreGlobalEnvironment(environmentFile);
   if (error)
      return error;
   
   // only restore the search path if we have a compatible R version
   // (guard against attempts to attach incompatible packages to this
   // R session)
   if (isCompatibleSessionState)
   {
      Error error = restoreSearchPath(statePath);
      if (error)
         return error;
   }
   
   return Success();
}
   
} // namespace search_path
} // namespace session
} // namespace r
} // namespace rstudio



