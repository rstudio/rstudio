/*
 * SessionAsyncCompletions.cpp
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

#include "SessionAsyncPackageInformation.hpp"

#include <string>
#include <vector>
#include <sstream>

#include <core/FilePath.hpp>
#include <core/json/Json.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/Error.hpp>

#include <boost/format.hpp>
#include <boost/algorithm/string.hpp>

#include <session/SessionModuleContext.hpp>

#include <core/Macros.hpp>

namespace rstudio {
namespace session {
namespace modules {
namespace r_packages {

// static variables
bool AsyncPackageInformationProcess::s_isUpdating_ = false;
std::vector<std::string> AsyncPackageInformationProcess::s_pkgsToUpdate_;

using namespace rstudio::core;

class CompleteUpdateOnExit : public boost::noncopyable {

public:

   ~CompleteUpdateOnExit()
   {
      using namespace rstudio::core::r_util;

      // Give empty completions to the packages which weren't updated
      for (std::vector<std::string>::const_iterator it = AsyncPackageInformationProcess::s_pkgsToUpdate_.begin();
           it != AsyncPackageInformationProcess::s_pkgsToUpdate_.end();
           ++it)
      {
         if (!RSourceIndex::hasInformation(*it))
         {
            RSourceIndex::addPackageInformation(*it, PackageInformation());
         }
      }

      AsyncPackageInformationProcess::s_pkgsToUpdate_.clear();
      AsyncPackageInformationProcess::s_isUpdating_ = false;
   }

};

void AsyncPackageInformationProcess::onCompleted(int exitStatus)
{
   CompleteUpdateOnExit updateScope;

   DEBUG("* Completed async library lookup");
   std::vector<std::string> splat;

   std::string stdOut = stdOut_.str();

   stdOut_.str(std::string());
   stdOut_.clear();

   if (stdOut == "" || stdOut == "\n")
   {
      DEBUG("- Received empty response");
      return;
   }

   boost::split(splat, stdOut, boost::is_any_of("\n"));

   std::size_t n = splat.size();
   DEBUG("- Received " << n << " lines of response");

   // Each line should be a JSON object with the format:
   //
   // {
   //    "package": <single package name>
   //    "exports": <array of object names in the namespace>,
   //    "types": <array of types (see .rs.acCompletionTypes)>,
   //    "functions": <object mapping function names to arguments>,
   //    "performs_nse": <array of 0, 1 indicating whether NSE performed by fn>
   // }
   for (std::size_t i = 0; i < n; ++i)
   {
      json::Array exportsJson;
      json::Array typesJson;
      json::Object functionsJson;
      json::Array performsNseJson;
      
      core::r_util::PackageInformation completions;

      if (splat[i].empty())
         continue;

      json::Value value;

      if (!json::parse(splat[i], &value))
      {
         std::string subset;
         if (splat[i].length() > 60)
            subset = splat[i].substr(0, 60) + "...";
         else
            subset = splat[i];

         LOG_ERROR_MESSAGE("Failed to parse JSON: '" + subset + "'");
         continue;
      }

      Error error = json::readObject(value.get_obj(),
                                     "package", &completions.package,
                                     "exports", &exportsJson,
                                     "types", &typesJson,
                                     "functions", &functionsJson,
                                     "performs_nse", &performsNseJson);

      if (error)
      {
         LOG_ERROR(error);
         continue;
      }

      DEBUG("Adding entry for package: '" << completions.package << "'");

      if (!json::fillVectorString(exportsJson, &(completions.exports)))
         LOG_ERROR_MESSAGE("Failed to read JSON 'objects' array to vector");

      if (!json::fillVectorInt(typesJson, &(completions.types)))
         LOG_ERROR_MESSAGE("Failed to read JSON 'types' array to vector");

      if (!json::fillMap(functionsJson, &(completions.functions)))
         LOG_ERROR_MESSAGE("Failed to read JSON 'functions' object to map");
      
      if (!json::fillVectorInt(performsNseJson, &(completions.performsNse)))
         LOG_ERROR_MESSAGE("Failed to read JSON 'performs_nse' array to vector");
      
      // Update the index
      core::r_util::RSourceIndex::addPackageInformation(completions.package, completions);
   }

}

void AsyncPackageInformationProcess::update()
{
   using namespace rstudio::core::r_util;
   
   if (s_isUpdating_)
      return;
   
   s_isUpdating_ = true;
   
   s_pkgsToUpdate_ =
      RSourceIndex::getAllUnindexedPackages();
   
   // alias for readability
   const std::vector<std::string>& pkgs = s_pkgsToUpdate_;
   
   DEBUG_BLOCK("Completions")
   {
      if (!pkgs.empty())
      {
         std::cerr << "Updating packages: [";
         std::cerr << "'" << pkgs[0] << "'";
         for (std::size_t i = 1; i < pkgs.size(); i++)
         {
            std::cerr << ", '" << pkgs[i] << "'";
         }
         std::cerr << "]\n";
      }
      else
      {
         std::cerr << "No packages to update; bailing out" << std::endl;
      }
   }
   
   if (pkgs.empty())
   {
      s_isUpdating_ = false;
      return;
   }
   
   std::stringstream ss;
   ss << ".rs.getPackageInformation(";
   ss << "'" << pkgs[0] << "'";
   
   if (pkgs.size() > 0)
   {
      for (std::vector<std::string>::const_iterator it = pkgs.begin() + 1;
           it != pkgs.end();
           ++it)
      {
         ss << ",'" << *it << "'";
      }
   }
   ss << ");";
   
   std::string finalCmd = ss.str();
   DEBUG("Running command: '" << finalCmd << "'");
   
   boost::shared_ptr<AsyncPackageInformationProcess> pProcess(
         new AsyncPackageInformationProcess());
   
   pProcess->start(
            finalCmd.c_str(),
            core::FilePath(),
            async_r::R_PROCESS_VANILLA | async_r::R_PROCESS_AUGMENTED);
   
}

} // end namespace r_completions
} // end namespace modules
} // end namespace session
} // end namespace rstudio
