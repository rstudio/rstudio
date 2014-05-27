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

#include <core/Error.hpp>
#include <core/StringUtils.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionConsoleProcess.hpp>

using namespace core ;

namespace session {  
namespace modules {
namespace build {

namespace {

} // anonymous namespace

Error installRtools()
{
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
   std::string rtoolsBinary = "Rtools31.exe";
   std::string rtoolsPath = "http://cran.rstudio.com/bin/windows/Rtools";
   FilePath downloadPath = tempPath.childPath(rtoolsBinary);
   std::string dest = string_utils::utf8ToSystem(downloadPath.absolutePath());
   boost::format fmt("download.file('%1%/%2%', '%3%')");
   std::string cmd = boost::str(fmt % rtoolsPath % rtoolsBinary % dest);

   // build args
   std::vector<std::string> args;
   args.push_back("--slave");
   args.push_back("--no-save");
   args.push_back("--no-restore");
   args.push_back("-e");
   args.push_back(cmd);

   // create and execute console process
   core::system::ProcessOptions options;
   options.terminateChildren = true;
   boost::shared_ptr<console_process::ConsoleProcess> pCP;
   pCP = console_process::ConsoleProcess::create(
            string_utils::utf8ToSystem(rProgramPath.absolutePath()),
            args,
            options,
            "Downloading Rtools",
            true,
            console_process::InteractionNever);

   json::Object data;
   data["console_process_info"] = pCP->toJson();
   data["installer_path"] = downloadPath.absolutePath();
   ClientEvent event(client_events::kInstallRtools, data);
   module_context::enqueClientEvent(event);

   return Success();
}


} // namespace build
} // namespace modules
} // namespace session
