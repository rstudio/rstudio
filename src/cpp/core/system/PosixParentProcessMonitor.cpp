/*
 * PosixParentProcessMonitor.cpp
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

#include <core/system/ParentProcessMonitor.hpp>

#include <errno.h>
#include <unistd.h>

#include <boost/algorithm/string/join.hpp>
#include <boost/assert.hpp>

#include <shared_core/SafeConvert.hpp>
#include <shared_core/system/EnvironmentLock.hpp>

#include <core/Log.hpp>

namespace rstudio {
namespace core {
namespace parent_process_monitor {

namespace {

std::vector<int> s_writeOnExit;

int setFdEnv(std::string name, int val)
{
   core::system::EnvironmentLock lock;

   std::string strVal = safe_convert::numberToString(val);
   return ::setenv(name.c_str(), strVal.c_str(), 1);
}

int getFdEnv(std::string name, int defaultVal)
{
   core::system::EnvironmentLock lock;

   char* result = ::getenv(name.c_str());
   if (!result)
      return defaultVal;
   return core::safe_convert::stringTo(result, defaultVal);
}

void exitHandler()
{
   // Signal normal termination to all child processes
   // that may be waiting
   for (size_t i = 0; i < s_writeOnExit.size(); i++)
   {
      // write to child (don't bother with checking error as there may
      // be one in the case that the child is already gone)
      (void)::write(s_writeOnExit.at(i), "done", 4);
   }
}

} // anonymous namespace

Error wrapFork(boost::function<void()> func)
{
   int fds[2];
   int result = ::pipe(fds);
   if (result != 0)
      return systemError(errno, ERROR_LOCATION);

   result = setFdEnv("RS_PPM_FD_READ", fds[0]);
   if (result != 0)
      return systemError(errno, ERROR_LOCATION);
   result = setFdEnv("RS_PPM_FD_WRITE", fds[1]);
   if (result != 0)
      return systemError(errno, ERROR_LOCATION);

   func();

   ::close(fds[0]);

   ::atexit(exitHandler);
   s_writeOnExit.push_back(fds[1]);

   return Success();
}

std::pair<int, int> parentTerminationFds()
{
   return std::make_pair(getFdEnv("RS_PPM_FD_READ", -1),
                         getFdEnv("RS_PPM_FD_WRITE", -1));
}

ParentTermination waitForParentTermination()
{
   std::pair<int, int> fds = parentTerminationFds();
   return waitForParentTermination(fds.first, fds.second);
}

ParentTermination waitForParentTermination(int readFd, int writeFd)
{
   if (readFd < 0 || writeFd < 0)
      return ParentTerminationNoParent;

   ::close(writeFd);

   char buf[256];
   int result = ::read(readFd, buf, 256);

   if (result == 0)
      return ParentTerminationAbnormal;
   else if (result > 0)
      return ParentTerminationNormal;
   else
      return ParentTerminationWaitFailure;
}

} // namespace parent_process_monitor
} // namespace core
} // namespace rstudio
