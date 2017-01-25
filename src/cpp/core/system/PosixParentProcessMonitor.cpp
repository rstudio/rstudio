/*
 * PosixParentProcessMonitor.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#include <core/system/ParentProcessMonitor.hpp>

#include <errno.h>
#include <unistd.h>

#include <boost/assert.hpp>

#include <core/SafeConvert.hpp>
#include <core/Log.hpp>

namespace rstudio {
namespace core {
namespace parent_process_monitor {

namespace {

std::vector<int> s_writeOnExit;

int setFdEnv(std::string name, int val)
{
   std::string strVal = safe_convert::numberToString(val);
   return ::setenv(name.c_str(), strVal.c_str(), strVal.size());
}

int getFdEnv(std::string name, int defaultVal)
{
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

ParentTermination waitForParentTermination()
{
   int fds[2];
   fds[0] = getFdEnv("RS_PPM_FD_READ", -1);
   fds[1] = getFdEnv("RS_PPM_FD_WRITE", -1);

   if (fds[0] < 0 || fds[1] < 0)
      return ParentTerminationNoParent;

   ::close(fds[1]);

   char buf[256];
   int result = ::read(fds[0], buf, 256);

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
