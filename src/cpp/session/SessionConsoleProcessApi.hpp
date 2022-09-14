/*
 * SessionConsoleProcessApi.hpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef SESSION_CONSOLE_PROCESS_API_HPP
#define SESSION_CONSOLE_PROCESS_API_HPP

#include <session/SessionConsoleProcess.hpp>

#include "SessionConsoleProcessTable.hpp"

namespace rstudio {
namespace core {
   class Error;
}
}

namespace rstudio {
namespace session {
namespace console_process {

core::Error initializeApi();

} // namespace console_process
} // namespace session
} // namespace rstudio

#endif // SESSION_CONSOLE_PROCESS_API_HPP

