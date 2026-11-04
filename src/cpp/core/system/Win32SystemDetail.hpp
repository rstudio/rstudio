/*
 * Win32SystemDetail.hpp
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

// Internals of Win32System.cpp that its tests reach into; shared here so
// the two translation units cannot disagree on a layout.

#ifndef CORE_SYSTEM_WIN32_SYSTEM_DETAIL_HPP
#define CORE_SYSTEM_WIN32_SYSTEM_DETAIL_HPP

#include <map>
#include <string>
#include <vector>

#include <core/system/System.hpp>

// after windows.h, via System.hpp
#include <tlhelp32.h>

namespace rstudio {
namespace core {
namespace system {
namespace detail {

std::vector<SubprocInfo> getSubprocesses(PidType pid,
                                         const FILETIME* pParentCreationTime);

struct OpenedProcess
{
   PidType pid;
   HANDLE handle;
};

// every process in a snapshot, by parent pid
typedef std::map<PidType, std::vector<PROCESSENTRY32W>> ProcessListing;

bool listProcesses(ProcessListing* pListing);

void openProcessTree(HANDLE hRoot,
                     PidType rootPid,
                     const ProcessListing& listing,
                     std::vector<OpenedProcess>* pProcesses);

bool getProcessParentPid(HANDLE hProcess, PidType* pParentPid);

bool isListedProcess(const std::wstring& listedExe,
                     PidType listedParentPid,
                     const std::wstring& openedExe,
                     const PidType* pOpenedParentPid,
                     const FILETIME* pCreationTime,
                     const FILETIME* pParentCreationTime);

} // namespace detail
} // namespace system
} // namespace core
} // namespace rstudio

#endif // CORE_SYSTEM_WIN32_SYSTEM_DETAIL_HPP
