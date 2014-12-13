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

#include "SessionAsyncRCompletions.hpp"

#include <string>
#include <vector>
#include <sstream>

#include <core/FilePath.hpp>
#include <core/json/Json.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/Error.hpp>

#include <boost/format.hpp>
#include <boost/algorithm/string.hpp>

#define DEBUG(x) \
   std::cerr << x << std::endl;

namespace session {
namespace modules {
namespace r_completions {

using namespace core;

void AsyncRCompletions::onCompleted(int exitStatus)
{
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
   //    "functions": <object mapping function names to arguments>
   // }
   for (std::size_t i = 0; i < n; ++i)
   {
      json::Array exportsJson;
      json::Array typesJson;
      json::Object functionsJson;
      core::r_util::AsyncLibraryCompletions completions;

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
                                     "functions", &functionsJson);

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

      // Update the index
      core::r_util::RSourceIndex::addCompletions(completions.package, completions);

   }

}

boost::mutex AsyncRCompletions::mutex_;

void AsyncRCompletions::update()
{
   LOCK_MUTEX(mutex_)
   {
      static boost::shared_ptr<AsyncRCompletions> pProcess(
               new AsyncRCompletions());

      if (pProcess->isRunning())
         return;

      std::stringstream ss;
      std::vector<std::string> pkgs =
            core::r_util::RSourceIndex::getUnindexedPackages();

      if (pkgs.empty())
         return;

      for (std::vector<std::string>::const_iterator it = pkgs.begin();
           it != pkgs.end();
           ++it)
      {
         std::string const& pkg = *it;

         // NOTE: Since this is all going over the command line eventually
         // it's imperative that all statements are separated by semicolons.
         boost::format fmt(
                  "tryCatch({"
                  "   ns <- asNamespace('%1%');"
                  "   exports <- getNamespaceExports(ns);"
                  "   objects <- mget(exports, ns, inherits = TRUE);"
                  "   types <- unlist(lapply(objects, .rs.getCompletionType));"
                  "   isFunction <- unlist(lapply(objects, is.function));"
                  "   functions <- objects[isFunction];"
                  "   functions <- lapply(functions, function(x) { names(formals(x)) });"
                  "   output <- list("
                  "     package = I('%1%'),"
                  "     exports = exports,"
                  "     types = types,"
                  "     functions = functions"
                  "   );"
                  "   cat(.rs.toJSON(output), sep = '\\\\n');"
                  "}, error = function(e) invisible(NULL));"
                  );

         ss << boost::str(fmt % pkg);
      }

      std::string finalCmd = ss.str();

      pProcess->start(finalCmd.c_str(),
                      core::FilePath(),
                      async_r::R_PROCESS_VANILLA | async_r::R_PROCESS_AUGMENTED);
   }
   END_LOCK_MUTEX

}

} // end namespace r_completions
} // end namespace modules
} // end namespace session
