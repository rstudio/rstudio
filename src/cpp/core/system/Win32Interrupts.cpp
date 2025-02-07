/*
 * Win32Interrupts.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
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

#include <core/system/Interrupts.hpp>

#include <Windows.h>

namespace rstudio {
namespace core {
namespace system {

// NOTE: On Windows, the process group associated with a process is not
// made available via any publicly-available Windows APIs. However, for
// any process created with the CREATE_NEW_PROCESS_GROUP flag, the
// process group id is the same as the process id of that new process.
//
// To wit, this API should only be used to terminate processes that are
// known to be process group leaders. This normally implies that the
// process was created with the 'terminateChildren' flag set, but
// _NOT_ with 'detachProcess'.
void interrupt(int processGroupId)
{
   ::GenerateConsoleCtrlEvent(CTRL_BREAK_EVENT, processGroupId);
}

} // end namespace system
} // end namespace core
} // end namespace rstudio
