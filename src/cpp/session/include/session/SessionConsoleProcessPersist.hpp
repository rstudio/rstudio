/*
 * SessionConsoleProcessPersist.hpp
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
#ifndef SESSION_CONSOLE_PROCESS_PERSIST_HPP
#define SESSION_CONSOLE_PROCESS_PERSIST_HPP

#include <string>

namespace rstudio {
namespace core {
   class Error;
}
}

namespace rstudio {
namespace session {
namespace console_process {

// Helpers for reading/writing ConsoleProcess metadata and buffers.
namespace console_persist {

// Load the raw console process metadata for all known processes
std::string loadConsoleProcessMetadata();

// Save the raw console process metadata for all known processes
void saveConsoleProcesses(const std::string& metadata);

// Get the saved buffer for the given ConsoleProcess. If maxLines > 0,
// trims the saved buffer to the given number of lines and persists it,
// then returns the trimmed buffer.
std::string getSavedBuffer(const std::string& handle, int maxLines);

// Add to the saved buffer for the given ConsoleProcess
void appendToOutputBuffer(const std::string& handle, const std::string& buffer);

// Delete the persisted saved buffer for the given ConsoleProcess
void deleteLogFile(const std::string& handle);

// Clean up ConsoleProcess buffer cache
// Takes a function to see if a given handle represents a known process.
void deleteOrphanedLogs(bool (*validHandle)(const std::string&));

} // namespace console_persist
} // namespace console_process
} // namespace session
} // namespace rstudio

#endif // SESSION_CONSOLE_PROCESS_PERSIST_HPP
