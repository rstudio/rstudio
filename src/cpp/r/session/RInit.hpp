/*
 * RInit.hpp
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
#ifndef R_SESSION_INIT_HPP
#define R_SESSION_INIT_HPP

namespace rstudio {
namespace core {
   class Error;
   class FilePath;
} 
}

namespace rstudio {
namespace r {
namespace session {

core::Error initialize();

// Sets aside saved session state (from a suspend or a restart) that an earlier
// start never finished restoring, so that a crash while restoring it doesn't
// repeat on every start. Must run before deciding how to start R, since the
// state is no longer there afterwards.
void setAsideUnfinishedRestores();

void reportHistoryAccessError(const std::string& context,
                              const core::FilePath& historyFilePath,
                              const core::Error& error);

core::FilePath rHistoryFilePath();

   
} // namespace session
} // namespace r
} // namespace rstudio

#endif // R_SESSION_INIT_HPP 
