/*
 * Win32ConPty.hpp
 *
 * Copyright (C) 2021 by RStudio, PBC
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

#include "Win32ConPty.hpp"

namespace rstudio {
namespace core {
namespace system {

WinConPty::~WinConPty()
{
}

Error WinConPty::start(const std::string& exe,
                       const std::vector<std::string> args,
                       const ProcessOptions& options,
                       HANDLE* pStdInWrite,
                       HANDLE* pStdOutRead,
                       HANDLE* pStdErrRead,
                       HANDLE* pProcess)
{
   return Success();
}

bool WinConPty::ptyRunning() const
{
   return false;
}

Error WinConPty::setSize(int cols, int rows)
{
   return Success();
}

Error WinConPty::interrupt()
{
   return Success();
}


} // namespace system
} // namespace core
} // namespace rstudio