/*
 * SessionAsyncNAMESPACECompletions.cpp
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

// #define RSTUDIO_ENABLE_DEBUG_MACROS

#include "SessionAsyncNAMESPACECompletions.hpp"

#include <core/Error.hpp>
#include <core/json/Json.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/Macros.hpp>

#include <session/projects/SessionProjects.hpp>

namespace rstudio {
namespace session {
namespace modules {
namespace r_completions {

using namespace core;

// static variables
bool AsyncNAMESPACECompletions::s_isUpdating_ = false;
AsyncNAMESPACECompletions::Completions AsyncNAMESPACECompletions::s_completions_;

AsyncNAMESPACECompletions::Completions AsyncNAMESPACECompletions::get()
{
   return AsyncNAMESPACECompletions::s_completions_;
}

class CompleteUpdateOnExit : boost::noncopyable {
public:
   ~CompleteUpdateOnExit()
   {
      AsyncNAMESPACECompletions::s_isUpdating_ = false;
   }
};

void AsyncNAMESPACECompletions::onCompleted(int exitStatus)
{
   CompleteUpdateOnExit updateScope;
   
   std::string stdOut = stdOut_.str();
   DEBUG("Received response:\n- " << stdOut);
   if (stdOut.size() == 0)
      return;
   
   // Returns output of the form:
   //
   // [{
   //    "package": <package name>,
   //    "exports": <array of objects exported by that package>,
   // }]
   json::Value value;
   
   if (!json::parse(stdOut, &value))
   {
      std::size_t max = std::max(60UL, stdOut.length());
      LOG_ERROR_MESSAGE("Failed to parse JSON: '" + stdOut.substr(0, max) + "'");
      return;
   }
   
   if (value.type() != json::ArrayType)
   {
      LOG_ERROR_MESSAGE("Invalid JSON: expected an array");
      return;
   }
   
   json::Array array = value.get_array();
   for (std::size_t i = 0; i < array.size(); ++i)
   {
      std::string package;
      json::Array exportsJson;
            
      json::Object object = array.at(i).get_obj();
      Error error = json::readObject(object,
                                     "package", &package,
                                     "exports", &exportsJson);
      if (error)
      {
         LOG_ERROR(error);
         return;
      }
      
      std::vector<std::string> exports;
      if (!json::fillVectorString(exportsJson, &exports))
         LOG_ERROR_MESSAGE("Failed to read JSON 'exports' array to vector");
      
      s_completions_[package] = exports;
   }
}

void AsyncNAMESPACECompletions::update()
{
   using namespace rstudio::core::r_util;
   if (s_isUpdating_)
      return;
   
   if (!projects::projectContext().hasProject())
      return;
   
   std::string projectPath =
         projects::projectContext().directory().absolutePath();
   
   s_isUpdating_ = true;
   
   boost::shared_ptr<AsyncNAMESPACECompletions> pProcess(
            new AsyncNAMESPACECompletions());
   
   std::string command = ".rs.asyncNAMESPACECompletions('" +
         string_utils::jsonLiteralEscape(projectPath) + "')";
   
   DEBUG("Executing command: " << command);
   pProcess->start(
            command.c_str(),
            core::FilePath(),
            async_r::R_PROCESS_VANILLA | async_r::R_PROCESS_AUGMENTED);
}

} // end namespace r_completions
} // end namespace modules
} // end namespace session
} // end namespace rstudio
