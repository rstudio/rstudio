/*
 * PosixEnvironment.cpp
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

#include <core/system/Environment.hpp>

#include <pthread.h>
#include <stdlib.h>

#include <boost/algorithm/string/predicate.hpp>

extern char **environ;

namespace rstudio {
namespace core {
namespace system {

namespace {

// guards access to the process environment: glibc's setenv can reallocate
// (and free) the environ array while a concurrent getenv walks it without
// locking, so unsynchronized cross-thread access can dereference a freed
// array (see rstudio-pro#4628, #10756). only callers of these wrappers are
// protected; direct ::getenv calls (libc internals, R) are not.
//
// a plain pthread mutex (rather than boost::mutex) so it can participate in
// pthread_atfork below: forked children may read the environment before
// exec, so a fork must never snapshot this mutex in the locked state
pthread_mutex_t s_environmentMutex = PTHREAD_MUTEX_INITIALIZER;

class EnvironmentLock
{
public:
   EnvironmentLock()
   {
      ::pthread_mutex_lock(&s_environmentMutex);
   }

   ~EnvironmentLock()
   {
      ::pthread_mutex_unlock(&s_environmentMutex);
   }

private:
   EnvironmentLock(const EnvironmentLock&);
   EnvironmentLock& operator=(const EnvironmentLock&);
};

void lockEnvironmentMutex()
{
   ::pthread_mutex_lock(&s_environmentMutex);
}

void unlockEnvironmentMutex()
{
   ::pthread_mutex_unlock(&s_environmentMutex);
}

// hold the mutex across fork so both the parent and the child resume with
// it unlocked (the child's sole thread is the forking thread, which owns it)
struct AtForkRegistration
{
   AtForkRegistration()
   {
      ::pthread_atfork(lockEnvironmentMutex,
                       unlockEnvironmentMutex,
                       unlockEnvironmentMutex);
   }
};

AtForkRegistration s_atForkRegistration;

} // anonymous namespace

namespace impl {

bool optionIsNamed(const Option& option, const std::string& name)
{
   return boost::algorithm::equals(option.first, name);
}

} // namespace impl

void environment(Options* pEnvironment)
{
   EnvironmentLock lock;

   for (char **env = environ; *env; ++env)
   {
      Option envVar;
      if (parseEnvVar(std::string(*env), &envVar))
         pEnvironment->push_back(envVar);
   }
}

std::string getenv(const std::string& name)
{
   EnvironmentLock lock;

   char * value = ::getenv(name.c_str());
   if (value)
      return std::string(value);
   else
      return std::string();
}

void setenv(const std::string& name, const std::string& value)
{
   EnvironmentLock lock;

   ::setenv(name.c_str(), value.c_str(), 1);
}

void unsetenv(const std::string& name)
{
   EnvironmentLock lock;

   ::unsetenv(name.c_str());
}


} // namespace system
} // namespace core
} // namespace rstudio
