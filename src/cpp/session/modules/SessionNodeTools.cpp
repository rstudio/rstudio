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

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

#include <core/system/System.hpp>

#include <r/ROptions.hpp>
#include <r/RSexp.hpp>

#include <session/SessionOptions.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace node_tools {

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
               return Success();
         }
      }
   }

   // Check admin-configured path
   if (!session::options().nodePath().isEmpty())
   {
      FilePath nodePath = session::options().nodePath();
      FilePath arm64NodePath;

      // on arm64 Mac, substitute the arm64-specific node binary
      if (findNodeMacArm64(nodePath, &arm64NodePath))
      {
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
               *pNodePath = nodeExePath;
               return Success();
            }
         }

         return Error(fileNotFoundError(nodePath, ERROR_LOCATION));
      }
      else if (nodePath.isRegularFile())
      {
         *pNodePath = nodePath;
         return Success();
      }
      else
      {
         return Error(fileNotFoundError(nodePath, ERROR_LOCATION));
      }
   }

   // Search PATH
   Error error = system::findProgramOnPath(kNodeExe, pNodePath);
   if (error)
      return error;

   return Success();
}

} // namespace node_tools
} // namespace modules
} // namespace session
} // namespace rstudio
