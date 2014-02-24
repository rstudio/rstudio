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

#include <core/Error.hpp>

#include <r/RExec.hpp>

#include <session/SessionModuleContext.hpp>

using namespace core;

namespace session {
namespace modules { 
namespace rmarkdown {
namespace install {

namespace {

// note the current version
std::string s_currentVersion;

// perform a silent upgrade
void silentUpgrade(bool)
{
   // path to archive
   FilePath archivesDir = session::options().sessionPackageArchivesPath();
   FilePath rmarkdownArchivePath = archivesDir.childPath("rmarkdown_" +
                                                         s_currentVersion +
                                                         ".tar.gz");
   std::string pkg = string_utils::utf8ToSystem(
                                    rmarkdownArchivePath.absolutePath());

   Error error = r::exec::RFunction(".rs.updateRMarkdownPackage", pkg).call();
   if (error)
      LOG_ERROR(error);
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
   boost::regex re("rmarkdown_([0-9]+\\.[0-9]+\\.[0-9]+)\\.tar\\.gz");
   BOOST_FOREACH(const FilePath& child, children)
   {
      boost::smatch match;
      if (boost::regex_match(child.filename(), match, re))
      {
         s_currentVersion = match[1];
         break;
      }
   }

   // check the status, if we've got an older version then schedule
   // some deferred work to do a silent upgrade
   if (status() == OlderVersionInstalled)
   {
      using namespace boost::posix_time;
      module_context::events().onDeferredInit.connect(silentUpgrade);
   }

   return Success();
}

Status status()
{
   if (module_context::isPackageVersionInstalled("rmarkdown", s_currentVersion))
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


Error installWithProgress()
{

   return Success();
}


} // namespace presentation
} // namepsace rmarkdown
} // namespace modules
} // namesapce session

