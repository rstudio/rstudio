/*
 * SessionConsoleProcessPersist.cpp
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

#include <session/SessionConsoleProcessPersist.hpp>

#include <boost/foreach.hpp>

#include <core/FileSerializer.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionOptions.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace console_process {
namespace console_persist {

#define kConsoleIndex "INDEX001"

// Change this constant if incompatible/version specific changes need to be
// made to ConsoleProcess/terminal persistence, e.g. "console02". Ideally
// this will be rare to never once we ship.
//
// 2017/02/03 - console -> console01
//                Reset state to eliminate previously saved buffers that
//                haven't had their alt-buffer stripped during save.
// 2017/03/08 - console01 -> console02
//                Added shell type property to allow Windows to track
//                multiple terminal types (cmd, powershell, git bash, etc.)
// 2017/03/21 - console02 -> console03
//                Added channel type and channel ID to allow distinction of
//                using non-RPC-based communication channel back to client
// 2017/05/15 - console03 -> console04
//                Added current-working directory, alt-buffer, cols, rows
// 2017/06/8  - console04 -> console05
//                Added autoClose, zombie
// 2017/06/16 - console05 -> console06
//                Added trackEnv
#define kConsoleDir "console06"

namespace {

FilePath s_consoleProcPath;
FilePath s_consoleProcIndexPath;
bool s_inited = false;
const std::string s_envFileExt = ".env";

void initialize()
{
   if (s_inited) return;

   if (session::options().multiSession() &&
       session::options().programMode() == kSessionProgramModeServer)
   {
      s_consoleProcPath = module_context::sessionScratchPath().complete(kConsoleDir);
   }
   else
   {
      s_consoleProcPath = module_context::scopedScratchPath().complete(kConsoleDir);
   }

   Error error = s_consoleProcPath.ensureDirectory();
   if (error)
   {
      LOG_ERROR(error);
      return;
   }
   s_consoleProcIndexPath = s_consoleProcPath.complete(kConsoleIndex);
   s_inited = true;
}

const FilePath& getConsoleProcPath()
{
   initialize();
   return s_consoleProcPath;
}

const FilePath& getConsoleProcIndexPath()
{
   initialize();
   return s_consoleProcIndexPath;
}

Error getLogFilePath(const std::string& handle, FilePath* pFile)
{
   initialize();
   Error error = getConsoleProcPath().ensureDirectory();
   if (error)
   {
      return error;
   }

   *pFile = getConsoleProcPath().complete(handle);
   return Success();
}

Error getEnvFilePath(const std::string& handle, FilePath* pFile)
{
   initialize();
   Error error = getConsoleProcPath().ensureDirectory();
   if (error)
   {
      return error;
   }

   std::string envHandle = handle;
   envHandle.append(s_envFileExt);
   *pFile = getConsoleProcPath().complete(envHandle);
   return Success();
}

} // anonymous namespace

std::string loadConsoleProcessMetadata()
{
   std::string contents;

   if (!getConsoleProcIndexPath().exists())
      return "";

   Error error = rstudio::core::readStringFromFile(getConsoleProcIndexPath(), &contents);
   if (error)
   {
      LOG_ERROR(error);
      return "";
   }
   return contents;
}

void saveConsoleProcesses(const std::string& metadata)
{
   initialize();

   if (!s_consoleProcPath.exists())
      return;

   Error error = rstudio::core::writeStringToFile(getConsoleProcIndexPath(),
                                                  metadata);
   if (error)
      LOG_ERROR(error);
}

std::string getSavedBuffer(const std::string& handle, int maxLines)
{
   std::string content;
   FilePath log;

   Error error = getLogFilePath(handle, &log);
   if (error)
   {
      LOG_ERROR(error);
      return content;
   }

   if (!log.exists())
   {
      return "";
   }

   error = core::readStringFromFile(log, &content);
   if (error)
   {
      LOG_ERROR(error);
      return content;
   }

   if (maxLines < 1)
      return content;

   // Trim the buffer based on maxLines. Otherwise it can grow without
   // bound until the terminal is closed or cleared.
   std::string trimmedOutput = content;
   if (string_utils::trimLeadingLines(maxLines, &trimmedOutput))
   {
      error = core::writeStringToFile(log, trimmedOutput);
      if (error)
      {
          LOG_ERROR(error);
          return content;
      }
   }
   return trimmedOutput;
}

int getSavedBufferLineCount(const std::string& handle, int maxLines)
{
   std::string buffer = getSavedBuffer(handle, maxLines);
   return string_utils::countNewlines(buffer) + 1;
}

void appendToOutputBuffer(const std::string& handle, const std::string& buffer)
{
   FilePath log;
   Error error = getLogFilePath(handle, &log);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   error = rstudio::core::appendToFile(log, buffer);
   if (error)
   {
      LOG_ERROR(error);
   }
}

void deleteLogFile(const std::string &handle, bool lastLineOnly)
{
   FilePath log;
   Error error = getLogFilePath(handle, &log);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   if (!log.exists())
      return;

   if (!lastLineOnly)
   {
      // blow away the file
      error = log.removeIfExists();
      if (error)
      {
         LOG_ERROR(error);
         return;
      }
   }
   else
   {
      // read the file, remove last line and rewrite buffer
      std::string content;
      error = core::readStringFromFile(log, &content);
      if (error)
      {
         LOG_ERROR(error);
         return;
      }

      size_t lastNewline = content.find_last_of('\n');

      // no complete line in buffer, just blow it away
      if (lastNewline == std::string::npos)
      {
         deleteLogFile(handle, false);
         return;
      }

      // erase everything after the final newline
      content.erase(++lastNewline);
      error = core::writeStringToFile(log, content);
      if (error)
      {
          LOG_ERROR(error);
      }
   }
}

void deleteOrphanedLogs(bool (*validHandle)(const std::string&))
{
   if (!validHandle)
      return;

   // Delete orphaned buffer files
   std::vector<FilePath> children;
   Error error = getConsoleProcPath().children(&children);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }
   BOOST_FOREACH(const FilePath& child, children)
   {
      // Don't erase the INDEXnnn or any subfolders
      if (!child.filename().compare(kConsoleIndex) || child.isDirectory())
      {
         continue;
      }

      if (!validHandle(child.stem()))
      {
         error = child.remove();
         if (error)
            LOG_ERROR(error);
      }
   }
}

void saveConsoleEnvironment(const std::string& handle, const core::system::Options& environment)
{
   FilePath log;
   Error error = getEnvFilePath(handle, &log);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   json::Object envJson = json::toJsonObject(environment);
   std::ostringstream ostr;
   json::writeFormatted(envJson, ostr);
   error = rstudio::core::writeStringToFile(log, ostr.str());
   if (error)
   {
      LOG_ERROR(error);
   }
}

void loadConsoleEnvironment(const std::string& handle, core::system::Options* pEnv)
{
   FilePath log;
   Error error = getEnvFilePath(handle, &log);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   if (!log.exists())
      return;

   std::string jsonStr;
   error = rstudio::core::readStringFromFile(log, &jsonStr);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   json::Value envJson;
   if (!json::parse(jsonStr, &envJson) ||
       !json::isType<json::Object>(envJson))
   {
      LOG_ERROR(systemError(boost::system::errc::protocol_error,
                            "Error parsing terminal environment save file",
                            ERROR_LOCATION));
      return;
   }

   core::system::Options loadedEnvironment = json::optionsFromJson(envJson.get_obj());
   BOOST_FOREACH(const core::system::Option& var, loadedEnvironment)
   {
      core::system::setenv(pEnv, var.first, var.second);
   }
}

void deleteEnvFile(const std::string& handle)
{
   FilePath log;
   Error error = getEnvFilePath(handle, &log);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   if (!log.exists())
      return;

   error = log.removeIfExists();
   if (error)
   {
      LOG_ERROR(error);
      return;
   }
}

} // namespace console_persist
} // namespace console_process
} // namespace session
} // namespace rstudio
