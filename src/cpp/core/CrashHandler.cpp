/*
 * CrashHandler.cpp
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

#include <core/CrashHandler.hpp>

#include <sstream>

#include <core/FileUtils.hpp>
#include <core/Settings.hpp>
#include <core/StringUtils.hpp>
#include <core/system/System.hpp>
#include <core/system/Environment.hpp>
#include <core/system/Xdg.hpp>

#include "config.h"

#ifndef RSTUDIO_CRASHPAD_ENABLED
namespace rstudio {
namespace core {
namespace crash_handler {

Error initialize(ProgramMode programMode)
{
   return Success();
}

ConfigSource configSource()
{
   return ConfigSource::Default;
}

bool isHandlerEnabled()
{
   return false;
}

Error setUserHandlerEnabled(bool)
{
   return Success();
}

bool hasUserBeenPromptedForPermission()
{
   return true;
}

Error setUserHasBeenPromptedForPermission()
{
   return Success();
}

} // namespace crash_handler
} // namespace core
} // namespace rstudio
#else

#include <crashpad/client/crashpad_client.h>
#include <crashpad/client/crash_report_database.h>
#include <crashpad/client/settings.h>

#define kCrashHandlingEnabled         "crash-handling-enabled"
#define kCrashHandlingEnabledDefault  false
#define kCrashDatabasePath            "crash-db-path"
#define kCrashDatabasePathDefault     ""
#define kUploadUrl                    "upload-url"
#define kUploadUrlDefault             "https://sentry.io/api/1379214/minidump/?sentry_key=4e76b8d2cffb49419fec1b431e09247c"
#define kUploadDumps                  "uploads-enabled"
#define kUploadDumpsDefault           true
#define kUploadProxy                  "upload-proxy"
#define kUploadProxyDefault           ""
#define kCrashConfFile                "crash-handler.conf"
#define kCrashPermissionFile          "crash-handler-permission"

namespace rstudio {
namespace core {
namespace crash_handler {

namespace {

boost::shared_ptr<crashpad::CrashpadClient> s_crashpadClient;
boost::shared_ptr<Settings> s_settings;
ProgramMode s_programMode;

FilePath adminConfFile()
{
   return core::system::xdg::systemConfigFile(kCrashConfFile);
}

FilePath userConfFile()
{
   return core::system::xdg::userConfigDir().completeChildPath(kCrashConfFile);
}

void readOptions()
{
   s_settings.reset(new Settings());

   FilePath optionsFile = adminConfFile();

   if (s_programMode == ProgramMode::Desktop && !optionsFile.exists())
   {
      // admin-level file does not exist - check for existence of user file
      // this is only done for desktop mode, as only the admin file should be
      // respected in server mode
      optionsFile = userConfFile();

      // if the user options file does not explicitly exist, we will forcefully create it
      // this is to ensure that there is an actual file backing for the settings
      // as multiple user clients can update it simultaneously, so we need to have it
      // backed to file as opposed to just hanging around in memory so that all clients
      // can properly see the state at all times
      if (!optionsFile.exists())
      {
         FilePath oldOptionsFile = core::system::userSettingsPath(
            core::system::userHomePath(),
            "R",
            true).completePath(kCrashConfFile);

         Error error = Success();
         if (oldOptionsFile.exists())
         {
            // Migrate conf from older version of RStudio if present.
            error = oldOptionsFile.copy(optionsFile);
            if (error)
            {
               LOG_ERROR(error);
            }
         }

         // The new file might not exist because we failed to migrate the old file or
         // because there *was* no old file. Either way, create the new file now.
         if (!optionsFile.exists())
         {
            error = optionsFile.getParent().ensureDirectory();
            if (!error)
               error = optionsFile.ensureFile();
            if (error)
               LOG_ERROR(error);
         }
      }
   }

   if (optionsFile.exists())
   {
      Error error = s_settings->initialize(optionsFile);
      if (error)
         LOG_ERROR(error);
   }
}

base::FilePath googleFilePath(const std::string& str)
{
#ifdef _WIN32
   std::string utf8Str = core::string_utils::systemToUtf8(str);
   std::wstring wideStr = core::string_utils::utf8ToWide(utf8Str);
   return base::FilePath(wideStr);
#else
   return base::FilePath(str);
#endif
}

void logClientCreation(const base::FilePath& handlerPath,
                       const base::FilePath& databasePath,
                       const std::string& uploadUrl)
{
#ifdef _WIN32
   std::wstringstream msg;
   std::wstring uploadUrlStr = core::string_utils::utf8ToWide(uploadUrl);
#else
   std::stringstream msg;
   const std::string& uploadUrlStr = uploadUrl;
#endif

    msg << "Initializing crashpad client:" <<
           " handlerPath=" << handlerPath.value() <<
           " databasePath=" << databasePath.value() <<
           " uploadUrl=" << uploadUrlStr;

#ifdef _WIN32
    std::string message = core::string_utils::wideToUtf8(msg.str());
#else
    std::string message = msg.str();
#endif

    LOG_INFO_MESSAGE(message);
}

FilePath permissionFile()
{
   return core::system::xdg::userDataDir().completeChildPath(kCrashPermissionFile);
}

} // anonymous namespace

Error initialize(ProgramMode programMode)
{
#ifndef RSTUDIO_CRASHPAD_ENABLED
   return Success();
#endif
   s_programMode = programMode;

   readOptions();

   // if crash handling is explicitly disabled, exit out
   bool crashHandlingEnabled = s_settings->getBool(kCrashHandlingEnabled, kCrashHandlingEnabledDefault);
   if (!crashHandlingEnabled)
      return Success();

   // get the path to the crashpad database
   std::string databasePathStr = s_settings->get(kCrashDatabasePath, kCrashDatabasePathDefault);
   if (databasePathStr.empty())
   {
#ifdef RSTUDIO_SERVER
      if (s_programMode == ProgramMode::Server)
      {
         // server mode - default database path to default tmp location
         FilePath tmpPath;
         Error error = FilePath::tempFilePath(tmpPath);
         if (error)
            return error;

         FilePath databasePath = tmpPath.getParent().completeChildPath("crashpad_database");

         // ensure that the database path exists
         error = databasePath.ensureDirectory();
         if (error)
            return error;

         // ensure that it is writeable by all users
         // this is best case and we swallow the error because it is legitimately possible we
         // lack the permissions to perform this (such as if we are an unprivileged rsession user)
         databasePath.changeFileMode(core::FileMode::ALL_READ_WRITE_EXECUTE);

         databasePathStr = databasePath.getAbsolutePath();
      }
      else
      {
         // desktop mode - default database path to user settings path
         databasePathStr = core::system::userSettingsPath(
            core::system::userHomePath(),
            "R",
            true).completeChildPath("crashpad_database").getAbsolutePath();
      }
#else
      // desktop mode - default database path to user settings path
      databasePathStr = core::system::userSettingsPath(core::system::userHomePath(),
                                                       "R",
                                                       true).completeChildPath("crashpad_database").getAbsolutePath();
#endif
   }
   base::FilePath databasePath = googleFilePath(databasePathStr);

   // determine if dumps should be uploaded automatically
   std::string uploadUrl = s_settings->get(kUploadUrl, kUploadUrlDefault);
   bool uploadDumps = s_settings->getBool(kUploadDumps, kUploadDumpsDefault) && !uploadUrl.empty();

   // get the path to the crashpad handler
   FilePath exePath;
   Error error = core::system::executablePath(nullptr, &exePath);
   if (error)
      return error;

   // get the path for the crash handler - this may be overridden via env var
   // for supporting development setups
   base::FilePath handlerPath;
   std::string crashHandlerPathEnv = core::system::getenv(kCrashHandlerEnvVar);
   if (!crashHandlerPathEnv.empty())
   {
      handlerPath = googleFilePath(crashHandlerPathEnv);
   }
   else
   {
      #ifndef _WIN32
         // for server, we use the crash handler proxy to ensure that the crashpad handler
         // is run with the correct permissions (otherwise ptrace will not work if setuid is used)
         #ifdef RSTUDIO_SERVER
            if (s_programMode == ProgramMode::Server)
               handlerPath = googleFilePath(exePath.getParent().completeChildPath("crash-handler-proxy").getAbsolutePath());
            else
               handlerPath = googleFilePath(exePath.getParent().completeChildPath("crashpad_handler").getAbsolutePath());
         #else
            handlerPath = googleFilePath(exePath.getParent().completeChildPath("crashpad_handler").getAbsolutePath());
         #endif
      #else
         handlerPath = googleFilePath(exePath.getParent().completeChildPath("crashpad_handler.exe").getAbsolutePath());
#endif
   }

   // open the crashpad database
   std::unique_ptr<crashpad::CrashReportDatabase> pDatabase =
         crashpad::CrashReportDatabase::Initialize(databasePath);

   // in server mode, attempt to give full access to the entire database for all users
   // this is necessary so unprivileged processes can write crash dumps properly
   // again, we swallow the errors here because unprivileged processes cannot change permissions
#ifdef RSTUDIO_SERVER
   if (s_programMode == ProgramMode::Server)
   {
      std::vector<FilePath> dbFolders;
      FilePath(databasePathStr).getChildren(dbFolders);
      for (const FilePath& subPath : dbFolders)
         subPath.changeFileMode(core::FileMode::ALL_READ_WRITE_EXECUTE);
   }
#endif

   // ensure database is properly initialized
   if (pDatabase != nullptr && pDatabase->GetSettings() != nullptr)
      pDatabase->GetSettings()->SetUploadsEnabled(uploadDumps);
   else
      return systemError(boost::system::errc::no_such_file_or_directory, ERROR_LOCATION);

   logClientCreation(handlerPath, databasePath, uploadUrl);

   // initialize and start crashpad client
   s_crashpadClient.reset(new crashpad::CrashpadClient());
   std::map<std::string, std::string> annotations;
   annotations["sentry[release]"] = RSTUDIO_VERSION;
   std::vector<std::string> args {"--no-rate-limit"};

#ifdef __linux__
   // export proxy environment variable if set
   // if not set, the default (system-wide setting) or any existing proxy env var is used instead
   // see https://curl.haxx.se/libcurl/c/CURLOPT_PROXY.html for more info
   std::string proxy = s_settings->get(kUploadProxy, kUploadProxyDefault);
   if (!proxy.empty())
      core::system::setenv("ALL_PROXY", proxy);

   bool success = s_crashpadClient->StartHandlerAtCrash(handlerPath,
                                                        databasePath,
                                                        base::FilePath(),
                                                        uploadUrl,
                                                        annotations,
                                                        args);
#else
   bool success = s_crashpadClient->StartHandler(handlerPath,
                                                 databasePath,
                                                 base::FilePath(),
                                                 uploadUrl,
                                                 annotations,
                                                 args,
                                                 true,
                                                 false);

#endif

   return success ? Success() : systemError(boost::system::errc::invalid_argument, ERROR_LOCATION);
}

ConfigSource configSource()
{
   FilePath settingsPath = s_settings->filePath();
   if (settingsPath.isEmpty())
      return ConfigSource::Default;

   if (settingsPath == adminConfFile())
      return ConfigSource::Admin;
   else if (settingsPath == userConfFile())
      return ConfigSource::User;
   else
      return ConfigSource::Default;
}

bool isHandlerEnabled()
{
   return s_settings->getBool(kCrashHandlingEnabled, kCrashHandlingEnabledDefault);
}

Error setUserHandlerEnabled(bool handlerEnabled)
{
   ConfigSource source = configSource();

   if (source == ConfigSource::Admin)
   {
      // the admin setting is in effect so there's nothing to change
      return Success();
   }

   if (source == ConfigSource::Default)
   {
      FilePath userFile = userConfFile();
      Error error = userFile.ensureFile();
      if (error)
         return error;

      Settings settings;
      error = settings.initialize(userFile);
      if (error)
         return error;

      settings.set(kCrashHandlingEnabled, handlerEnabled);
   }
   else
   {
      // we already have the user file open - simply update the settings directly
      s_settings->set(kCrashHandlingEnabled, handlerEnabled);
   }

   return Success();
}

bool hasUserBeenPromptedForPermission()
{
   if (!permissionFile().exists())
   {
      // check for the old (pre RStudio 1.4) permission file
      FilePath oldPermissionFile = core::system::userSettingsPath(
         core::system::userHomePath(),
         "R",
         false).completePath(kCrashPermissionFile);
      if (oldPermissionFile.exists())
          return true;

      // if for some reason the parent directory is not writeable
      // we will just treat the user as if they have been prompted
      // to prevent indefinite repeated promptings
      if (!file_utils::isDirectoryWriteable(permissionFile().getParent()))
         return true;
      else
         return false;
   }
   else
      return true;
}

Error setUserHasBeenPromptedForPermission()
{
   return permissionFile().ensureFile();
}

} // namespace crash_handler
} // namespace core
} // namespace rstudio

#endif
