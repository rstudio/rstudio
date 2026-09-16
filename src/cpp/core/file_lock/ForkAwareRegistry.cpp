/*
 * ForkAwareRegistry.cpp
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

#include "ForkAwareRegistry.hpp"

#include <vector>

#ifdef _WIN32
# include <windows.h>
#endif

namespace rstudio {
namespace core {
namespace file_lock {

namespace {

#ifdef _WIN32
SRWLOCK s_initializationMutex = SRWLOCK_INIT;
#else
// Constant initialization avoids C++ guards, and neither the mutex nor the
// list is destroyed: file-lock destructors may still run at static shutdown.
pthread_mutex_t s_initializationMutex = PTHREAD_MUTEX_INITIALIZER;
std::vector<ForkAwareRegistry*>* s_pRegistries = nullptr;
#endif

} // anonymous namespace

#ifdef _WIN32

ForkAwareRegistry::InitializationGuard::InitializationGuard()
{
   ::AcquireSRWLockExclusive(&s_initializationMutex);
}

ForkAwareRegistry::InitializationGuard::~InitializationGuard()
{
   ::ReleaseSRWLockExclusive(&s_initializationMutex);
}

void ForkAwareRegistry::publishRegistry(ForkAwareRegistry*)
{
}

#else

ForkAwareRegistry::AtForkRegistration::AtForkRegistration()
{
   ::pthread_atfork(prepareFork, parentAfterFork, childAfterFork);
}

// Register before runtime initialization can take the mutex. The out-of-line
// InitializationGuard methods ensure this translation unit, including this
// startup constructor, is linked wherever instance() is used.
ForkAwareRegistry::AtForkRegistration ForkAwareRegistry::s_atForkRegistration;

ForkAwareRegistry::InitializationGuard::InitializationGuard()
{
   ::pthread_mutex_lock(&s_initializationMutex);
}

ForkAwareRegistry::InitializationGuard::~InitializationGuard()
{
   ::pthread_mutex_unlock(&s_initializationMutex);
}

void ForkAwareRegistry::publishRegistry(ForkAwareRegistry* pRegistry)
{
   // Called under InitializationGuard, after construction and before the
   // caller publishes its singleton pointer. fork() sees all three or none.
   if (!s_pRegistries)
      s_pRegistries = new std::vector<ForkAwareRegistry*>();
   s_pRegistries->push_back(pRegistry);
}

void ForkAwareRegistry::prepareFork()
{
   ::pthread_mutex_lock(&s_initializationMutex);
   if (s_pRegistries)
   {
      for (ForkAwareRegistry* pRegistry : *s_pRegistries)
         ::pthread_mutex_lock(&pRegistry->mutex_);
   }
}

void ForkAwareRegistry::parentAfterFork()
{
   if (s_pRegistries)
   {
      for (auto it = s_pRegistries->rbegin(); it != s_pRegistries->rend(); ++it)
         ::pthread_mutex_unlock(&(*it)->mutex_);
   }
   ::pthread_mutex_unlock(&s_initializationMutex);
}

void ForkAwareRegistry::childAfterFork()
{
   if (s_pRegistries)
   {
      for (auto it = s_pRegistries->rbegin(); it != s_pRegistries->rend(); ++it)
      {
         ForkAwareRegistry* pRegistry = *it;
         pRegistry->resetInChild();

         // Any waiter belonged to a thread that does not exist in the child.
         ::pthread_cond_init(&pRegistry->condition_, nullptr);
         ::pthread_mutex_unlock(&pRegistry->mutex_);
      }
   }
   ::pthread_mutex_unlock(&s_initializationMutex);
}

#endif

} // namespace file_lock
} // namespace core
} // namespace rstudio
