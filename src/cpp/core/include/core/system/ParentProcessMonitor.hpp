/*
 * ParentProcessMonitor.hpp
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

#ifndef PARENT_PROCESS_MONITOR_HPP
#define PARENT_PROCESS_MONITOR_HPP

#include <utility>

#include <shared_core/Error.hpp>
#include <boost/function.hpp>

namespace rstudio {
namespace core {
namespace parent_process_monitor {

Error wrapFork(boost::function<void()> func);

enum ParentTermination {
   ParentTerminationNormal,
   ParentTerminationAbnormal,
   ParentTerminationNoParent,
   ParentTerminationWaitFailure
};

ParentTermination waitForParentTermination();

#ifndef _WIN32

// reads the RS_PPM_FD_* environment variables established by wrapFork().
// call this on the thread that owns startup (typically the main thread)
// and pass the result to the overload below: reading the environment from
// a freshly launched monitor thread races the setenv calls made during
// startup, which is not thread-safe
std::pair<int, int> parentTerminationFds();

ParentTermination waitForParentTermination(int readFd, int writeFd);

#endif

} // namespace parent_process_monitor
} // namespace core
} // namespace rstudio

#endif // PARENT_PROCESS_MONITOR_HPP
