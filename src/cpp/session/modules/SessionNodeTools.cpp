/*
 * SessionNodeTools.cpp
 *
 * Copyright (C) 2025 by Posit Software, PBC
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

#include "SessionNodeTools.hpp"
#include "SessionLogging.hpp"

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

#include <core/system/Environment.hpp>
#include <core/system/System.hpp>
#include <core/system/Process.hpp>

#include <session/prefs/UserPrefs.hpp>

#include <cctype>
#include <cstdio>
#include <cstdlib>

#include <boost/algorithm/string/trim.hpp>

#include <r/ROptions.hpp>
#include <r/RSexp.hpp>

#include <session/SessionOptions.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace node_tools {

namespace {

#ifndef _WIN32
# define kNodeExe "node"
#else
# define kNodeExe "node.exe"
#endif

/**
 * If running on arm64 Mac, substitute the arm64-specific node binary; returns true if this was
 * done, false otherwise.
 */
bool findNodeMacArm64(const FilePath& inputPath, FilePath* pOutputNodePath)
{
#if defined(__APPLE__)
   if (system::isAppleSilicon())
   {
      FilePath nodeExePath;
      if (inputPath.isRegularFile())
      {
         // change /node/bin/node to /node-arm64/bin/node
         nodeExePath = inputPath.getParent().getParent().completeChildPath("node-arm64/bin/" kNodeExe);
      }
      else if (inputPath.isDirectory())
      {
         // change /node to /node-arm64
         nodeExePath = inputPath.getParent().completeChildPath("node-arm64");
      }
      else
         return false;

      if (nodeExePath.exists())
      {
         *pOutputNodePath = nodeExePath;
         return true;
      }
   }
#endif
   return false;
}

} // end anonymous namespace

Error findNode(FilePath* pNodePath, const std::string& rOptionName)
{
   // Check R option override if provided
   if (!rOptionName.empty())
   {
      SEXP nodePathSEXP = r::options::getOption(rOptionName);
      if (nodePathSEXP != R_NilValue)
      {
         std::string nodePath = r::sexp::asString(nodePathSEXP);
         if (!nodePath.empty())
         {
            *pNodePath = FilePath(nodePath);
            if (pNodePath->exists())
            {
               DLOG("Found node via R option '{}': '{}'.",
                    rOptionName, pNodePath->getAbsolutePath());
               return Success();
            }

            WLOG("R option '{}' set to '{}', but path does not exist.",
                 rOptionName, nodePath);
         }
      }
   }

   // Check admin-configured path
   if (!session::options().nodePath().isEmpty())
   {
      FilePath nodePath = session::options().nodePath();
      DLOG("Checking admin-configured node path: '{}'.",
           nodePath.getAbsolutePath());

      FilePath arm64NodePath;

      // on arm64 Mac, substitute the arm64-specific node binary
      if (findNodeMacArm64(nodePath, &arm64NodePath))
      {
         DLOG("Substituted arm64 node path: '{}'.",
              arm64NodePath.getAbsolutePath());
         nodePath = arm64NodePath;
      }

      // Allow both directories containing a 'node' binary, and the path
      // to a 'node' binary directly.
      if (nodePath.isDirectory())
      {
         for (auto&& suffix : { "bin/" kNodeExe, kNodeExe })
         {
            FilePath nodeExePath = nodePath.completeChildPath(suffix);
            if (nodeExePath.exists())
            {
               DLOG("Found node in admin-configured directory: '{}'.",
                    nodeExePath.getAbsolutePath());
               *pNodePath = nodeExePath;
               return Success();
            }
         }

         WLOG("Admin-configured node directory '{}' does not contain a '{}' binary.",
              nodePath.getAbsolutePath(), kNodeExe);
         return Error(fileNotFoundError(nodePath, ERROR_LOCATION));
      }
      else if (nodePath.isRegularFile())
      {
         DLOG("Found node at admin-configured path: '{}'.",
              nodePath.getAbsolutePath());
         *pNodePath = nodePath;
         return Success();
      }
      else
      {
         WLOG("Admin-configured node path '{}' does not exist.",
              nodePath.getAbsolutePath());
         return Error(fileNotFoundError(nodePath, ERROR_LOCATION));
      }
   }

   // Search PATH
   DLOG("No admin-configured node path; searching PATH for '{}'.", kNodeExe);
   Error error = system::findProgramOnPath(kNodeExe, pNodePath);
   if (error)
   {
      WLOG("'{}' not found on PATH.", kNodeExe);
      return error;
   }

   DLOG("Found node on PATH: '{}'.", pNodePath->getAbsolutePath());
   return Success();
}

namespace {

// Whether `token` appears in `options` as a whole, whitespace-delimited token
// (so "--use-system-ca" does not match inside "--use-system-cafoo").
bool containsOptionToken(const std::string& options, const std::string& token)
{
   if (token.empty())
      return false;

   std::string::size_type pos = 0;
   while ((pos = options.find(token, pos)) != std::string::npos)
   {
      bool atStart = (pos == 0) ||
         std::isspace(static_cast<unsigned char>(options[pos - 1]));
      std::string::size_type end = pos + token.size();
      bool atEnd = (end == options.size()) ||
         std::isspace(static_cast<unsigned char>(options[end]));
      if (atStart && atEnd)
         return true;
      pos = end;
   }
   return false;
}

} // anonymous namespace

std::string appendNodeOption(const std::string& existingOptions,
                             const std::string& option)
{
   // Preserve the caller's NODE_OPTIONS verbatim (quoted values may contain
   // intentional whitespace); only append our flag when it is not already
   // present as a whole token. No tokenize/rejoin, so nothing is rewritten.
   if (containsOptionToken(existingOptions, option))
      return existingOptions;
   if (existingOptions.empty())
      return option;
   return existingOptions + " " + option;
}

bool parseNodeVersion(const std::string& versionOutput, int* pMajor, int* pMinor)
{
   std::string trimmed = boost::algorithm::trim_copy(versionOutput);
   if (!trimmed.empty() && (trimmed[0] == 'v' || trimmed[0] == 'V'))
      trimmed = trimmed.substr(1);

   int major = 0, minor = 0;
   if (std::sscanf(trimmed.c_str(), "%d.%d", &major, &minor) != 2)
      return false;

   if (major < 0 || minor < 0)
      return false;

   *pMajor = major;
   *pMinor = minor;
   return true;
}

bool versionSupportsSystemCa(int major, int minor)
{
   // Patch is intentionally ignored: the floor (22.17.0) is a .0 release, so
   // major.minor fully determines support.
   return (major > 22) || (major == 22 && minor >= 17);
}

bool nodeSupportsSystemCa(const core::FilePath& nodePath)
{
   DLOG("Probing node version at '{}' for --use-system-ca support.",
        nodePath.getAbsolutePath());

   core::system::ProcessOptions options;
   core::system::ProcessResult result;
   Error error = core::system::runProgram(nodePath.getAbsolutePath(),
                                          { "--version" },
                                          options,
                                          &result);
   if (error)
   {
      WLOG("Could not determine node version at '{}': {}",
           nodePath.getAbsolutePath(), error.getMessage());
      return false;
   }

   if (result.exitStatus != EXIT_SUCCESS)
   {
      WLOG("node --version at '{}' exited with status {}.",
           nodePath.getAbsolutePath(), result.exitStatus);
      return false;
   }

   int major = 0, minor = 0;
   if (!parseNodeVersion(result.stdOut, &major, &minor))
   {
      WLOG("Could not parse node version from output '{}'.", result.stdOut);
      return false;
   }

   bool supported = versionSupportsSystemCa(major, minor);
   if (!supported)
   {
      WLOG("Node {}.{} at '{}' does not support --use-system-ca via NODE_OPTIONS "
           "(requires 22.17.0+); the system certificate store will not be trusted.",
           major, minor, nodePath.getAbsolutePath());
   }

   return supported;
}

void applySystemCaOption(core::system::Options* pEnvironment,
                         const core::FilePath& nodePath)
{
   // Trust the OS certificate store when the user has opted in and the resolved
   // Node supports the flag. Additive to NODE_EXTRA_CA_CERTS; preserves any
   // existing NODE_OPTIONS. Guarded on the Node version because
   // NODE_OPTIONS=--use-system-ca is rejected by Node < 22.17.0 (it would
   // otherwise break agent startup).
   if (!prefs::userPrefs().assistantUseSystemCa())
      return;
   if (!nodeSupportsSystemCa(nodePath))
      return;

   std::string nodeOptions = core::system::getenv(*pEnvironment, "NODE_OPTIONS");
   core::system::setenv(pEnvironment, "NODE_OPTIONS",
                        appendNodeOption(nodeOptions, "--use-system-ca"));
}

} // namespace node_tools
} // namespace modules
} // namespace session
} // namespace rstudio
