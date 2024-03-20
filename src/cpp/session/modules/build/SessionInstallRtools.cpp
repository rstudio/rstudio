/*
 * SessionInstallRtools.cpp
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

#include "SessionInstallRtools.hpp"

#include <boost/format.hpp>
#include <boost/regex.hpp>

#include <shared_core/Error.hpp>
#include <core/StringUtils.hpp>
#include <core/FileUtils.hpp>
#include <core/RegexUtils.hpp>

#include <core/r_util/RToolsInfo.hpp>

#include <r/RExec.hpp>
#include <r/ROptions.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionConsoleProcess.hpp>
#include <session/prefs/UserPrefs.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {  
namespace modules {
namespace build {

Error installRtools()
{
   // populate list of known Rtools installers
   FilePath installPath("C:\\Rtools");
   std::vector<r_util::RToolsInfo> availableRtools;
   availableRtools.push_back(r_util::RToolsInfo("4.4", installPath));
   availableRtools.push_back(r_util::RToolsInfo("4.3", installPath));
   availableRtools.push_back(r_util::RToolsInfo("4.2", installPath));
   availableRtools.push_back(r_util::RToolsInfo("4.0", installPath));
   availableRtools.push_back(r_util::RToolsInfo("3.5", installPath));
   availableRtools.push_back(r_util::RToolsInfo("3.4", installPath));

   // determine appropriate version of Rtools for this copy of R
   std::string version, url;
   for (const r_util::RToolsInfo& rTools : availableRtools)
   {
      if (module_context::isRtoolsCompatible(rTools))
      {
         std::string repos = prefs::userPrefs().getCRANMirror().url;
         if (repos.empty())
            repos = module_context::rstudioCRANReposURL();

         version = rTools.name();
         url = rTools.url(repos);
         break;
      }
   }

   if (version.empty())
      return core::pathNotFoundError(ERROR_LOCATION);

   // get path to R binary
   FilePath rProgramPath;
   Error error = module_context::rScriptPath(&rProgramPath);
   if (error)
      return error;

   // create private tempdir for download
   FilePath tempPath;
   error = FilePath::tempFilePath(tempPath);
   if (error)
      return error;

   error = tempPath.ensureDirectory();
   if (error)
      return error;

   if (version == "4.3" || version == "4.4")
   {
      Error error = r::exec::RFunction(".rs.findRtoolsInstaller")
                        .addParam("version", version)
                        .addParam("url", url)
                        .call(&url);
      if (error)
      {
         LOG_ERROR(error);
         return error;
      }
   }

   if (url.empty())
      return core::pathNotFoundError(ERROR_LOCATION);

   // form path to destination file
   std::string rtoolsBinary = url.substr(url.find_last_of('/') + 1);
   FilePath installerPath = tempPath.completeChildPath(rtoolsBinary);
   std::string destfile = string_utils::utf8ToSystem(installerPath.getAbsolutePath());

   // The Rtools installer can be a large file, so we want to increase the timeout option
   // and respect the user's original configured timeout settings
   int originalTimeoutOption = r::options::getOption<int>("timeout");
   error = r::options::setOption<int>("timeout", std::max(3600, originalTimeoutOption));
   if (error)
       module_context::consoleWriteOutput("To avoid timeouts when downloading the installer, set `options(timeout = 3600)`.\n");

   // download it
   error = r::exec::RFunction("utils:::download.file")
       .addParam("url", url)
       .addParam("destfile", destfile)
       .addParam("mode", "wb")
       .call();

   r::options::setOption<int>("timeout", originalTimeoutOption);

   if (error)
   {
      std::string errMsg;
      if (r::isCodeExecutionError(error, &errMsg))
         module_context::consoleWriteError(errMsg + "\n");
      return error;
   }

   // fire the event
   json::Object data;
   data["version"] = version;
   data["installer_path"] = installerPath.getAbsolutePath();
   ClientEvent event(client_events::kInstallRtools, data);
   module_context::enqueClientEvent(event);

   return Success();
}


} // namespace build
} // namespace modules
} // namespace session
} // namespace rstudio
