/*
 * RMarkdownInstall.cpp
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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

#include "RMarkdownInstall.hpp"

#include <boost/foreach.hpp>
#include <boost/regex.hpp>
#include <boost/algorithm/string/join.hpp>

#include <core/Error.hpp>
#include <core/system/Environment.hpp>
#include <core/system/Process.hpp>

#include <r/RExec.hpp>

#include <session/SessionUserSettings.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/SessionConsoleProcess.hpp>

using namespace core;

namespace session {
namespace modules { 
namespace rmarkdown {
namespace install {

namespace {

// note the current version
std::string s_currentVersion;
std::string s_currentSHA1;

std::string rmarkdownPackageArchive()
{
   FilePath archivesDir = session::options().sessionPackageArchivesPath();
   FilePath rmarkdownArchivePath = archivesDir.childPath("rmarkdown_" +
                                                         s_currentVersion +
                                                         "_" +
                                                         s_currentSHA1 +
                                                         ".tar.gz");
   return string_utils::utf8ToSystem(rmarkdownArchivePath.absolutePath());
}

} // anonymous namespace


Error initialize()
{
   // determine the current version based on the archive file
   // we have embedded in our packages directory
   FilePath archivesDir = session::options().sessionPackageArchivesPath();
   std::vector<FilePath> children;
   Error error = archivesDir.children(&children);
   if (error)
      return error;
   boost::regex re("rmarkdown_([0-9]+\\.[0-9]+\\.[0-9]+)_([\\d\\w]+)\\.tar\\.gz");
   BOOST_FOREACH(const FilePath& child, children)
   {
      boost::smatch match;
      if (boost::regex_match(child.filename(), match, re))
      {
         s_currentVersion = match[1];
         s_currentSHA1 = match[2];
         break;
      }
   }

   return Success();
}

Status status()
{
   if (module_context::isPackageVersionInstalled("rmarkdown",
                                                 s_currentVersion,
                                                 s_currentSHA1))
   {
      return Installed;
   }
   else if (module_context::isPackageInstalled("rmarkdown"))
   {
      return OlderVersionInstalled;
   }
   else
   {
      return NotInstalled;
   }
}

bool haveRequiredVersion()
{
   return status() == Installed;
}


Error installWithProgress(
                  boost::shared_ptr<console_process::ConsoleProcess>* ppCP)
{ 
   // first ensure we have a writeable user library
   Error error = r::exec::RFunction(".rs.ensureWriteableUserLibrary").call();
   if (error)
      return error;

   // R binary
   FilePath rProgramPath;
   error = module_context::rScriptPath(&rProgramPath);
   if (error)
      return error;

   // options/environment
   core::system::ProcessOptions options;
   options.terminateChildren = true;
   options.redirectStdErrToStdOut = true;
   core::system::Options childEnv;
   core::system::environment(&childEnv);
   // allow child process to inherit our R_LIBS
   std::string libPaths = module_context::libPathsString();
   if (!libPaths.empty())
      core::system::setenv(&childEnv, "R_LIBS", libPaths);
   options.environment = childEnv;

   // CRAN packages
   using namespace module_context;
   std::vector<std::string> cranPackages;
   if (!isPackageVersionInstalled("knitr", "1.2"))
      cranPackages.push_back("'knitr'");
   if (!isPackageVersionInstalled("yaml", "2.1.5"))
      cranPackages.push_back("'yaml'");

   // build install command
   std::string cmd = "{";
   if (!cranPackages.empty())
   {
      std::string pkgList = boost::algorithm::join(cranPackages, ",");
      cmd += "utils::install.packages(c(" + pkgList + "), " +
             "repos = '"+ userSettings().cranMirror().url + "');";
   }
   cmd += "utils::install.packages('" + rmarkdownPackageArchive() + "', "
                                   "repos = NULL, type = 'source');";
   cmd += "}";

   // build args
   std::vector<std::string> args;
   args.push_back("--slave");
   args.push_back("--no-save");
   args.push_back("--no-restore");
   args.push_back("-e");
   args.push_back(cmd);

   // create and execute console process
   *ppCP = console_process::ConsoleProcess::create(
            string_utils::utf8ToSystem(rProgramPath.absolutePath()),
            args,
            options,
            "Installing R Markdown Package",
            true,
            console_process::InteractionNever);

   return Success();
}

// perform a silent upgrade
Error silentUpgrade()
{
   return r::exec::RFunction(".rs.updateRMarkdownPackage",
                             rmarkdownPackageArchive()).call();
}

} // namespace presentation
} // namepsace rmarkdown
} // namespace modules
} // namesapce session

