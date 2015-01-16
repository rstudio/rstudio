/*
 * SessionInstallRtools.cpp
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

#include "SessionInstallRtools.hpp"

#include <boost/format.hpp>
#include <boost/foreach.hpp>

#include <core/Error.hpp>
#include <core/StringUtils.hpp>

#include <core/r_util/RToolsInfo.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionConsoleProcess.hpp>
#include <session/SessionUserSettings.hpp>

#include "SessionBuildEnvironment.hpp"

using namespace core ;

namespace session {  
namespace modules {
namespace build {

namespace {

void onDownloadCompleted(const core::system::ProcessResult& result,
                         const std::string& version,
                         const FilePath& installerPath)
{
   if (result.exitStatus == EXIT_SUCCESS)
   {
      json::Object data;
      data["version"] = version;
      data["installer_path"] = installerPath.absolutePath();
      ClientEvent event(client_events::kInstallRtools, data);
      module_context::enqueClientEvent(event);
   }
   else
   {
      module_context::consoleWriteError(result.stdOut + "\n");
   }
}

} // anonymous namespace

Error installRtools()
{
   // determine the correct version of rtools
   std::string version, url;
   FilePath installPath("C:\\Rtools");
   std::vector<r_util::RToolsInfo> availableRtools;
   availableRtools.push_back(r_util::RToolsInfo("3.2", installPath));
   availableRtools.push_back(r_util::RToolsInfo("3.1", installPath));
   availableRtools.push_back(r_util::RToolsInfo("3.0", installPath));
   availableRtools.push_back(r_util::RToolsInfo("2.15", installPath));
   availableRtools.push_back(r_util::RToolsInfo("2.14", installPath));
   availableRtools.push_back(r_util::RToolsInfo("2.13", installPath));
   availableRtools.push_back(r_util::RToolsInfo("2.12", installPath));
   availableRtools.push_back(r_util::RToolsInfo("2.11", installPath));
   BOOST_FOREACH(const r_util::RToolsInfo& rTools, availableRtools)
   {
      if (isRtoolsCompatible(rTools))
      {
         version = rTools.name();

         std::string repos = userSettings().cranMirror().url;
         if (repos.empty())
            repos = "http://cran.rstudio.com/";
         url = rTools.url(repos);
         break;
      }
   }
   if (version.empty())
      return core::pathNotFoundError(ERROR_LOCATION);

   // R binary
   FilePath rProgramPath;
   Error error = module_context::rScriptPath(&rProgramPath);
   if (error)
      return error;

   // get a temp file path to download into
   FilePath tempPath;
   error = FilePath::tempFilePath(&tempPath);
   if (error)
      return error;
   error = tempPath.ensureDirectory();
   if (error)
      return error;

   // create the command
   std::string rtoolsBinary =
       "Rtools" + boost::algorithm::replace_all_copy(version, ".", "") + ".exe";
   FilePath installerPath = tempPath.childPath(rtoolsBinary);
   std::string dest = string_utils::utf8ToSystem(installerPath.absolutePath());
   boost::format fmt("utils::download.file('%1%', '%2%', mode = 'wb')");
   std::string cmd = boost::str(fmt % url % dest);

   // build args
   std::vector<std::string> args;
   args.push_back("--slave");
   args.push_back("--no-save");
   args.push_back("--no-restore");
   args.push_back("-e");
   args.push_back(cmd);

   // create and execute the process
   core::system::ProcessOptions options;
   options.redirectStdErrToStdOut = true;
   options.terminateChildren = true;
   module_context::processSupervisor().runProgram(
            string_utils::utf8ToSystem(rProgramPath.absolutePath()),
            args,
            "",
            options,
            boost::bind(onDownloadCompleted, _1, version, installerPath));

   return Success();
}


} // namespace build
} // namespace modules
} // namespace session
