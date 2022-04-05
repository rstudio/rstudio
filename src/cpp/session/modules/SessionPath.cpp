/*
 * SessionPath.cpp
 *
 * Copyright (C) 2022 by RStudio, PBC
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

#include <boost/bind/bind.hpp>
#include <boost/regex.hpp>
#include <boost/system/errc.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

#include <core/Algorithm.hpp>
#include <core/Log.hpp>
#include <core/FileSerializer.hpp>

#include <core/system/Process.hpp>
#include <core/system/System.hpp>
#include <core/system/Environment.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;
using namespace boost::placeholders;

namespace rstudio {
namespace session {
namespace modules { 
namespace path {

namespace {

bool containsPathEntry(
      const std::vector<std::string>& pathEntries,
      const std::string& entry)
{
   // tolerate paths with trailing slashes
   for (const std::string& item : { entry, entry + "/" })
      if (core::algorithm::contains(pathEntries, item))
         return true;
   
   return false;
   
}

Error initializePathViaShell(const std::string& shellPath,
                             std::string* pPath)
{
   // double-check that the requested shell exists
   if (!FilePath(shellPath).exists())
      return fileNotFoundError(shellPath, ERROR_LOCATION);
   
   std::vector<std::string> args = { "-l", "-c", "printf \"%s\" \"$PATH\"" };
   
   // try running it to see what the default PATH looks like
   core::system::ProcessOptions options;
   
   // don't inherit the PATH from this process
   core::system::Options environment;
   core::system::environment(&environment);
   core::system::unsetenv(&environment, "PATH");
   options.environment = environment;
   
   // run the program
   core::system::ProcessResult result;
   Error error = core::system::runProgram(shellPath, args, options, &result);
   
   if (error)
   {
      return error;
   }
   else if (result.exitStatus != EXIT_SUCCESS)
   {
      return systemError(
               boost::system::errc::state_not_recoverable,
               result.stdErr,
               ERROR_LOCATION);
   }
   else
   {
      // only include last line of output, in case the shell printed
      // something extra during login / processing of scripts
      std::string output = core::string_utils::trimWhitespace(result.stdOut);
      std::vector<std::string> lines = core::algorithm::split(output, "\n");
      auto n = lines.size();
      if (n == 0)
      {
         return systemError(
                  boost::system::errc::state_not_recoverable,
                  result.stdErr,
                  ERROR_LOCATION);
      }
      
      // extract last line of output
      std::string path = lines[n - 1];
      pPath->assign(path);
      return Success();
   }
}   

// this routine is a little awkward -- if RStudio was launched from a terminal,
// it's possible that the PATH is already set as appropriate for any program
// launched through a shell. as a heuristic, we try to see if 'usr/local/bin'
// is already on the PATH; if it is, we assume the path is appropriately
// initialized; otherwise, we try to initialize it in the same way a shell
// might.
std::string initializePath()
{
   // if the user's path already contains '/usr/local/bin', assume that
   // they're running RStudio through a shell / terminal and so we don't
   // need to re-read the shell PATH
   std::string defaultPath = core::system::getenv("PATH");
   boost::regex reUsrLocalbin("(^|:)/usr/local/bin(:|$)");
   if (boost::regex_search(defaultPath, reUsrLocalbin))
      return defaultPath;
   
   // first, try to initialize with user's default shell
   // (RSTUDIO_SESSION_SHELL is primarily for internal use)
   std::string shell = core::system::getenv("RSTUDIO_SESSION_SHELL");
   if (shell.empty())
      shell = core::system::getenv("SHELL");
   
   if (!shell.empty())
   {
      std::string path;
      Error error = initializePathViaShell(shell, &path);
      if (!error)
         return path;
   }
   
   // next, try to initialize with default shell
   if (shell != "/bin/sh")
   {
      std::string path;
      Error error = initializePathViaShell("/bin/sh", &path);
      if (!error)
         return path;
   }
   
   // all else fails, use the current application path
   return core::system::getenv("PATH");
}

std::string homePath(const std::string& suffix)
{
   return module_context::userHomePath()
         .completeChildPath(suffix)
         .getAbsolutePath();
}

} // anonymous namespace


Error initialize()
{
   
#ifdef __APPLE__
   std::string path = initializePath();
   
   // split into parts
   std::vector<std::string> pathEntries = core::algorithm::split(path, ":");
   
   // check for some components that we might need to append to the path
   std::vector<std::string> extraEntries = {
      homePath("Applications/quarto/bin"),
      "/Library/TeX/texbin",
      "/usr/texbin",
   };
   
   for (const std::string& entry : extraEntries)
   {
      if (!containsPathEntry(pathEntries, entry))
      {
         pathEntries.push_back(entry);
      }
   }
   
   // remove empty entries
   core::algorithm::expel_if(pathEntries, [](const std::string& entry)
   {
      return entry.empty();
   });
   
   // set the path
   core::system::setenv("PATH", core::algorithm::join(pathEntries, ":"));
#endif

   return Success();
}
   
} // namespace path
} // namespace modules
} // namespace session
} // namespace rstudio

