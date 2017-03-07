/*
 * SessionTerminalShell.cpp
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

#include "SessionTerminalShell.hpp"

#include <boost/foreach.hpp>

namespace rstudio {
namespace session {
namespace modules { 
namespace workbench {

namespace {

void scanAvailableShells(std::vector<TerminalShell>* pShells)
{
   pShells->push_back(TerminalShell());

#ifdef _WIN32
   // TODO (gary)
#endif
}

} // anonymous namespace

core::json::Object TerminalShell::toJson() const
{
   core::json::Object resultJson;
   resultJson["type"] = type;
   resultJson["name"] = name;
   return resultJson;
}

AvailableTerminalShells::AvailableTerminalShells()
{
   scanAvailableShells(&shells_);
}

void AvailableTerminalShells::toJson(core::json::Array* pArray) const
{
   BOOST_FOREACH(const TerminalShell& shell, shells_)
   {
      pArray->push_back(shell.toJson());
   }
}

bool AvailableTerminalShells::getInfo(TerminalShell::TerminalShellType type,
                                      TerminalShell* pShellInfo) const
{
   BOOST_FOREACH(const TerminalShell& shell, shells_)
   {
      if (shell.type == type)
      {
         *pShellInfo = shell;
         return true;
      }
   }
   return false;
}

} // namespace workbench
} // namespace modules
} // namesapce session
} // namespace rstudio

