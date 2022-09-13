/*
 * ServerEnvVars.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include "ServerEnvVars.hpp"

#include <boost/algorithm/string.hpp>

#include <core/system/Environment.hpp>
#include <core/system/Xdg.hpp>

#include <core/FileSerializer.hpp>
#include <core/Log.hpp>

#include <shared_core/SafeConvert.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace server {
namespace env_vars {

Error readEnvConfigFile(bool emitInfoLog)
{
   // Look up config file location (log search path if enabled)
   FilePath envConfPath = emitInfoLog ?
      core::system::xdg::findSystemConfigFile("environment variables", "env-vars") :
      core::system::xdg::systemConfigFile("env-vars");

   // No work to do if we don't have a file specifying server environment variables
   if (!envConfPath.exists())
   {
      return Success();
   }

   // Read all environment variable assignments from the file
   core::system::Options env;
   Error error = readCollectionFromFile<core::system::Options>(envConfPath, &env,
      [=](const std::string& line, core::system::Option* pPair)
      {
          // Ignore commented lines
          if (boost::starts_with(line, "#"))
             return ReadCollectionIgnoreLine;

          // Ignore lines that don't contain an environment variable assignment
          size_t pos = line.find('=');
          if (pos == std::string::npos)
             return ReadCollectionIgnoreLine;

          pPair->first = line.substr(0, pos);
          pPair->second = line.substr(pos+1);

          return ReadCollectionAddLine;
      });

   if (error)
   {
      return error;
   }

   // Indicate where we read the logs from (for diagnostic purposes)
   if (emitInfoLog)
   {
      LOG_INFO_MESSAGE("Read server environment variables from " + 
         envConfPath.getAbsolutePath() + " (" +
         safe_convert::numberToString(env.size()) + " variables found)");
   }

   // Set each environment variable
   for (core::system::Option var: env)
   {
      if (emitInfoLog)
      {
         LOG_INFO_MESSAGE("Setting server environment variable '" +
               var.first + "' = '" + var.second + "'");
      }
      core::system::setenv(var.first, var.second);
   }

   return Success();
}

// Forwards any HTTP proxy variables from the current process into the given environment.
void forwardHttpProxyVars(core::system::Options *pEnvironment)
{
   for (auto&& proxyVar: {"HTTP_PROXY", "HTTPS_PROXY", "NO_PROXY"})
   {
      std::string val = core::system::getenv(proxyVar);
      if (!val.empty())
      {
         std::string oldVal = core::system::getenv(*pEnvironment, proxyVar);
         if (!oldVal.empty() && oldVal != val)
         {
             LOG_WARNING_MESSAGE("Overriding HTTP proxy setting " + std::string(proxyVar) +
                                 ": '" + oldVal + "' => '" + val + "'");
         }
         core::system::setenv(pEnvironment, proxyVar, val);
      }
   }
}

Error initialize()
{
   return readEnvConfigFile(true /* emit info log */);
}

} // namespace env_vars
} // namespace server
} // namespace rstudio
