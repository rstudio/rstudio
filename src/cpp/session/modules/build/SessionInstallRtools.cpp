/*
 * SessionInstallRtools.cpp
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

#include "SessionInstallRtools.hpp"

#include <boost/format.hpp>

#include <shared_core/Error.hpp>
#include <core/StringUtils.hpp>

#include <core/r_util/RToolsInfo.hpp>

#include <r/RExec.hpp>

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
   bool gcc49 = module_context::usingMingwGcc49();
   FilePath installPath("C:\\Rtools");
   std::vector<r_util::RToolsInfo> availableRtools;
   availableRtools.push_back(r_util::RToolsInfo("4.0", installPath, gcc49));
   availableRtools.push_back(r_util::RToolsInfo("3.5", installPath, gcc49));
   availableRtools.push_back(r_util::RToolsInfo("3.4", installPath, gcc49));
   availableRtools.push_back(r_util::RToolsInfo("3.3", installPath, gcc49));
   availableRtools.push_back(r_util::RToolsInfo("3.2", installPath, gcc49));
   availableRtools.push_back(r_util::RToolsInfo("3.1", installPath, gcc49));
   availableRtools.push_back(r_util::RToolsInfo("3.0", installPath, gcc49));
   availableRtools.push_back(r_util::RToolsInfo("2.15", installPath, gcc49));
   availableRtools.push_back(r_util::RToolsInfo("2.14", installPath, gcc49));
   availableRtools.push_back(r_util::RToolsInfo("2.13", installPath, gcc49));
   availableRtools.push_back(r_util::RToolsInfo("2.12", installPath, gcc49));
   availableRtools.push_back(r_util::RToolsInfo("2.11", installPath, gcc49));

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

   // form path to destination file
   std::string rtoolsBinary = url.substr(url.find_last_of('/') + 1);
   FilePath installerPath = tempPath.completeChildPath(rtoolsBinary);
   std::string destfile = string_utils::utf8ToSystem(installerPath.getAbsolutePath());

   // download it
   error = r::exec::RFunction("utils:::download.file")
       .addParam("url", url)
       .addParam("destfile", destfile)
       .addParam("mode", "wb")
       .call();

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
