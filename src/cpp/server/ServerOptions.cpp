/*
 * ServerOptions.cpp
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

#include <server/ServerOptions.hpp>
#include <server/ServerConstants.hpp>

#include <iosfwd>
#include <fstream>

#include <boost/algorithm/string/trim.hpp>

#include <core/FileSerializer.hpp>
#include <core/r_util/RSessionContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace server {

namespace {

struct Deprecated
{
   Deprecated()
      : memoryLimitMb(0),
        stackLimitMb(0),
        userProcessLimit(0),
        authPamRequiresPriv(true)
   {
   }

   int memoryLimitMb;
   int stackLimitMb;
   int userProcessLimit;
   bool authPamRequiresPriv;
};

void reportDeprecationWarning(const std::string& option, std::ostream& os)
{
   os << "The option '" << option << "' is deprecated and will be discarded."
      << std::endl;
}

void reportDeprecationWarnings(const Deprecated& userOptions,
                               std::ostream& os)
{
   Deprecated defaultOptions;

   if (userOptions.memoryLimitMb != defaultOptions.memoryLimitMb)
      reportDeprecationWarning("rsession-memory-limit-mb", os);

   if (userOptions.stackLimitMb != defaultOptions.stackLimitMb)
      reportDeprecationWarning("rsession-stack-limit-mb", os);

   if (userOptions.userProcessLimit != defaultOptions.userProcessLimit)
      reportDeprecationWarning("rsession-process-limit", os);

   if (userOptions.authPamRequiresPriv != defaultOptions.authPamRequiresPriv)
      reportDeprecationWarning("auth-pam-requires-priv", os);
}

unsigned int stringToUserId(std::string minimumUserId,
                            unsigned int defaultMinimumId,
                            std::ostream& osWarnings)
{
   try
   {
      return boost::lexical_cast<unsigned int>(minimumUserId);
   }
   catch(boost::bad_lexical_cast&)
   {
      osWarnings << "Invalid value for auth-minimum-user-id '"
                 << minimumUserId << "'. Using default of "
                 << defaultMinimumId << "." << std::endl;

      return defaultMinimumId;
   }
}

unsigned int resolveMinimumUserId(std::string minimumUserId,
                                  std::ostream& osWarnings)
{
   // default for invalid input
   const unsigned int kDefaultMinimumId = 1000;

   // auto-detect if requested
   if (minimumUserId == "auto")
   {
      // if /etc/login.defs exists, scan it and look for a UID_MIN setting
      FilePath loginDefs("/etc/login.defs");
      if (loginDefs.exists())
      {
         const char uidMin[] = "UID_MIN";
         std::ifstream defStream(loginDefs.getAbsolutePath().c_str());
         std::string line;
         while (std::getline(defStream, line))
         {
            if (line.substr(0, sizeof(uidMin) - 1) == uidMin)
            {
               std::string value = boost::algorithm::trim_copy(
                                       line.substr(sizeof(uidMin) + 1));
               return stringToUserId(value, kDefaultMinimumId, osWarnings);
            }
         }
      }

      // none found, return default
      return kDefaultMinimumId;
   }
   else
   {
      return stringToUserId(minimumUserId, kDefaultMinimumId, osWarnings);
   }
}

} // anonymous namespace

Options& options()
{
   static Options instance;
   return instance;
}


ProgramStatus Options::read(int argc,
                            char * const argv[],
                            std::ostream& osWarnings)
{
   using namespace boost::program_options;

   // compute install path
   Error error = core::system::installPath("..", argv[0], &installPath_);
   if (error)
   {
      LOG_ERROR_MESSAGE("Unable to determine install path: "+error.getSummary());
      return ProgramStatus::exitFailure();
   }

   // compute the resource and binary paths
   FilePath resourcePath = installPath_;
   FilePath binaryPath = installPath_.completeChildPath("bin");

   // detect running in OSX bundle and tweak paths
#ifdef __APPLE__
   if (installPath_.completePath("Info.plist").exists())
   {
      resourcePath = installPath_.completePath("Resources");
      binaryPath = installPath_.completePath("MacOS");
   }
#endif

   // special program offline option (based on file existence at 
   // startup for easy bash script enable/disable of offline state)
   serverOffline_ = FilePath("/var/lib/rstudio-server/offline").exists();

   // generate monitor shared secret
   monitorSharedSecret_ = core::system::generateUuid();

   // build options
   options_description verify("verify");
   options_description server("server");
   options_description www("www");
   options_description rsession("rsession");
   options_description database("database");
   options_description auth("auth");
   options_description monitor("monitor");
   std::vector<std::string> wwwAllowedOrigins;
   std::string authLoginPageHtml;
   std::string authRdpLoginPageHtml;
   std::string authMinimumUserId;
   std::string sameSite;

   program_options::OptionsDescription optionsDesc =
         buildOptions(&verify, &server, &www, &rsession, &database, &auth, &monitor,
                      &sameSite, &wwwAllowedOrigins, &authLoginPageHtml, &authRdpLoginPageHtml,
                      &authMinimumUserId);

   // overlay hook
   addOverlayOptions(&verify, &server, &www, &rsession, &database, &auth, &monitor);

   optionsDesc.commandLine.add(verify).add(server).add(www).add(rsession).add(database).add(auth).add(monitor);
   optionsDesc.configFile.add(server).add(www).add(rsession).add(database).add(auth).add(monitor);
 
   // read options
   bool help = false;
   ProgramStatus status = core::program_options::read(optionsDesc,
                                                      argc,
                                                      argv,
                                                      &help);

   // terminate if this was a help request
   if (help)
      return ProgramStatus::exitSuccess();

   // report deprecation warnings
   Deprecated dep;
   dep.userProcessLimit = deprecatedUserProcessLimit_;
   dep.stackLimitMb = deprecatedStackLimitMb_;
   dep.memoryLimitMb = deprecatedMemoryLimitMb_;
   dep.authPamRequiresPriv = deprecatedAuthPamRequiresPriv_;
   reportDeprecationWarnings(dep, osWarnings);

   // check auth revocation dir - if unspecified, it should be put under the server data dir
   if (authRevocationListDir_.empty())
      authRevocationListDir_ = serverDataDir_;

   if (sameSite != kSameSiteOmitOption && 
      sameSite != kSameSiteNoneOption &&
      sameSite != kSameSiteLaxOption)
   {
      program_options::reportError("Invalid SameSite option: " + sameSite, ERROR_LOCATION, true);
      return ProgramStatus::exitFailure();
   }
   if (sameSite == kSameSiteNoneOption)
      wwwSameSite_ = rstudio::core::http::Cookie::SameSite::None;
   else if (sameSite == kSameSiteLaxOption)
      wwwSameSite_ = rstudio::core::http::Cookie::SameSite::Lax;
   else
      wwwSameSite_ = rstudio::core::http::Cookie::SameSite::Undefined;

   // call overlay hooks
   resolveOverlayOptions();
   std::string errMsg;
   if (!validateOverlayOptions(&errMsg, osWarnings))
   {
      program_options::reportError(errMsg, ERROR_LOCATION, true);
      return ProgramStatus::exitFailure();
   }

   // exit if the call to read indicated we should -- note we don't do this
   // immediately so that we can allow overlay validation to occur (otherwise
   // a --test-config wouldn't test overlay options)
   if (status.exit())
      return status;

   // rationalize auth settings
   if (authNone_)
      authValidateUsers_ = false;

   if (serverUser_.empty())
   {
      // it's an error to run with no server user at all, so presume root
      serverUser_ = "root";
      LOG_WARNING_MESSAGE("No server user specified, running as root instead "
            "(not recommended)");
   }
   else
   {
      // if specified, confirm that the program user exists. however, if the
      // program user is the default and it doesn't exist then allow that to pass,
      // this just means that the user did a simple make install and hasn't setup
      // an rserver user yet. in this case the program will run as root

      // look up details for the proposed user
      system::User user;
      Error error = system::User::getUserFromIdentifier(serverUser_, user);

      if (core::system::realUserIsRoot())
      {
         if (error || !user.exists())
         {
            if (serverUser_ == "rstudio-server")
            {
               // administrator hasn't created an rserver system account yet
               // so we'll end up running as root. note that we have to specify "root" as
               // the server user explicitly here since many codepaths look up the server
               // user by name (can't be empty)
               serverUser_ = "root";
               LOG_WARNING_MESSAGE("Server user ' " + serverUser_ + "' does not exist, running as "
                     "root instead (not recommended)");
            }
            else
            {
               LOG_ERROR_MESSAGE("Server user '" + serverUser_ + "' does not exist");
               return ProgramStatus::exitFailure();
            }
         }
         else
         {
            LOG_INFO_MESSAGE("Running as server user '" + serverUser_ + "' (with privilege)");
         }
      }
      else
      {
         if (error)
         {
            LOG_ERROR_MESSAGE("Could not find details for server user '" + serverUser_ + "'");
            LOG_ERROR(error);
            return ProgramStatus::exitFailure();
         }

         // We don't have privilege, which is okay so long as the server user is also the current
         // user (we can't change users without privilege)
         system::User currentUser;
         error = system::User::getCurrentUser(currentUser);
         if (error)
         {
            LOG_ERROR_MESSAGE("Could not find details for current user while attempting to "
                  "compare to server user '" + serverUser_ + "'");
            LOG_ERROR(error);
            return ProgramStatus::exitFailure();
         }

         if (user.getUserId() != currentUser.getUserId())
         {
            LOG_ERROR_MESSAGE("Attempt to run server as user '" + serverUser_ + "' (uid " +
                     safe_convert::numberToString(user.getUserId()) + ") "
                     "from account '" + currentUser.getUsername() + "' (uid " +
                     safe_convert::numberToString(currentUser.getUserId()) + ") "
                     "without privilege, which is required to run as a different uid");
            return ProgramStatus::exitFailure();
         }

         LOG_INFO_MESSAGE("Running as server user '" + serverUser_ + "' (without privilege)");
      }
   }

   // convert relative paths by completing from the system installation
   // path (this allows us to be relocatable)
   resolvePath(resourcePath, &wwwLocalPath_);
   resolvePath(resourcePath, &wwwSymbolMapsPath_);
   resolvePath(binaryPath, &authPamHelperPath_);
   resolvePath(binaryPath, &rsessionPath_);
   resolvePath(binaryPath, &rldpathPath_);
   resolvePath(resourcePath, &rsessionConfigFile_);

   // resolve minimum user id
   authMinimumUserId_ = resolveMinimumUserId(authMinimumUserId, osWarnings);
   core::r_util::setMinUid(authMinimumUserId_);

   // read auth login html
   FilePath loginPageHtmlPath(authLoginPageHtml);
   if (loginPageHtmlPath.exists())
   {
      Error error = core::readStringFromFile(loginPageHtmlPath, &authLoginPageHtml_);
      if (error)
         LOG_ERROR(error);
   }

   // read rdp auth login html
   FilePath rdpLoginPageHtmlPath(authRdpLoginPageHtml);
   if (rdpLoginPageHtmlPath.exists())
   {
      Error error = core::readStringFromFile(rdpLoginPageHtmlPath, &authRdpLoginPageHtml_);
      if (error)
         LOG_ERROR(error);
   }

   // trim any whitespace in allowed origins
   for (std::string& origin : wwwAllowedOrigins)
   {
      try
      {
         // escape domain part separators
         boost::replace_all(origin, ".", "\\.");

         // fix up wildcards
         boost::replace_all(origin, "*", ".*");

         boost::regex re(origin);
         wwwAllowedOrigins_.push_back(re);
      }
      catch (boost::bad_expression&)
      {
         LOG_ERROR_MESSAGE("Specified origin " + origin + " is an invalid domain. "
                           "It will not be available when performing origin safety checks.");
      }
   }

   // return status
   return status;
}

void Options::resolvePath(const FilePath& basePath,
                          std::string* pPath) const
{
   if (!pPath->empty())
      *pPath = basePath.completePath(*pPath).getAbsolutePath();
}

} // namespace server
} // namespace rstudio
