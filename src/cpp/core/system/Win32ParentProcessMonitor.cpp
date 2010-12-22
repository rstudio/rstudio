/*
 * Win32ParentProcessMonitor.cpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
#include <core/system/ParentProcessMonitor.hpp>

#include <windows.h>
#include <stdio.h>

#include <core/Log.hpp>
#include <core/SafeConvert.hpp>
#include <core/Error.hpp>

namespace core {
namespace parent_process_monitor {

#ifndef JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE
#define JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE 0x2000
#endif

Error wrapFork(boost::function<void()> func)
{
   static bool s_initialized = false;

   if (!s_initialized)
   {
      /*
       * Create a Job object and assign this process to it. This will
       * cause all child processes to be assigned to the same job.
       * With JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE set, all the child
       * processes will be killed when this process terminates (since
       * it is the only one holding a handle to the job).
       */

      HANDLE hJob = ::CreateJobObject(NULL, NULL);

      JOBOBJECT_EXTENDED_LIMIT_INFORMATION jeli = { 0 };
      jeli.BasicLimitInformation.LimitFlags = JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE;
      ::SetInformationJobObject(hJob,
                                JobObjectExtendedLimitInformation,
                                &jeli,
                                sizeof(jeli));

      if (!::AssignProcessToJobObject(hJob, ::GetCurrentProcess()))
         return systemError(::GetLastError(), ERROR_LOCATION);

      s_initialized = true;
   }

   func();

   return Success();
}

ParentTermination waitForParentTermination()
{
   return ParentTerminationNoParent;
}

} // namespace parent_process_monitor
} // namespace core
