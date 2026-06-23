/*
 * ServerCheckConfig.cpp
 *
 * Copyright (C) 2026 by Posit Software, PBC
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

#include "ServerCheckConfig.hpp"

#include <iostream>

namespace rstudio {
namespace server {

bool checkConfigFilePath(const std::string& optionName,
                         const core::FilePath& path,
                         std::ostream& out,
                         bool informational,
                         bool fileOnly)
{
   if (path.isEmpty())
      return true;

   if (path.exists())
   {
      if (fileOnly && path.isDirectory())
      {
         out << "[FAIL] " << optionName << ": " << path.getAbsolutePath()
             << " exists but is a directory, not a file" << std::endl;
         return false;
      }
      out << "[PASS] " << optionName << ": " << path.getAbsolutePath() << " exists" << std::endl;
      return true;
   }

   if (informational)
   {
      out << "[PASS] " << optionName << ": " << path.getAbsolutePath()
          << " not found (will be created on startup)" << std::endl;
      return true;
   }

   out << "[FAIL] " << optionName << ": " << path.getAbsolutePath() << " not found" << std::endl;
   return false;
}

bool checkConfigFilePath(const std::string& optionName,
                         const std::string& path,
                         std::ostream& out,
                         bool informational,
                         bool fileOnly)
{
   if (path.empty())
      return true;
   return checkConfigFilePath(optionName, core::FilePath(path), out, informational, fileOnly);
}

} // namespace server
} // namespace rstudio
