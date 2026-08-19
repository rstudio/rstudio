/*
 * EnvironmentLock.cpp
 *
 * Copyright (C) 2026 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant to the
 * terms of a commercial license agreement with Posit Software, then this program is
 * licensed to you under the following terms:
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

#include <shared_core/system/EnvironmentLock.hpp>

#ifdef _WIN32
# include <windows.h>
#else
# include <pthread.h>
#endif

namespace rstudio {
namespace core {
namespace system {

namespace {

#ifdef _WIN32

SRWLOCK s_environmentLock = SRWLOCK_INIT;

#else

// a plain pthread mutex (rather than boost::mutex) so it can participate in
// pthread_atfork below: forked children may read the environment before
// exec, so a fork must never snapshot this mutex in the locked state
pthread_mutex_t s_environmentMutex = PTHREAD_MUTEX_INITIALIZER;

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

#endif

} // anonymous namespace

#ifdef _WIN32

EnvironmentLock::EnvironmentLock()
{
   ::AcquireSRWLockExclusive(&s_environmentLock);
}

EnvironmentLock::~EnvironmentLock()
{
   ::ReleaseSRWLockExclusive(&s_environmentLock);
}

#else

EnvironmentLock::EnvironmentLock()
{
   ::pthread_mutex_lock(&s_environmentMutex);
}

EnvironmentLock::~EnvironmentLock()
{
   ::pthread_mutex_unlock(&s_environmentMutex);
}

#endif

} // namespace system
} // namespace core
} // namespace rstudio
