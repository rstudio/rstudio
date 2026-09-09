/*
 * ForkAwareRegistry.hpp
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

#ifndef CORE_FILE_LOCK_FORK_AWARE_REGISTRY_HPP
#define CORE_FILE_LOCK_FORK_AWARE_REGISTRY_HPP

#include <vector>

#include <boost/noncopyable.hpp>

#ifdef _WIN32
# include <boost/thread/condition_variable.hpp>
# include <boost/thread/mutex.hpp>
#else
# include <pthread.h>
#endif

namespace rstudio {
namespace core {
namespace file_lock {

// Base for the per-process lock registries.
//
// fork() copies a registry's mutex in whatever state another thread left it,
// so a child could block forever on its first registry call. Every registry
// therefore holds its mutex across fork() (via pthread_atfork, as
// EnvironmentLock does) and resets itself in the child, which inherits
// neither fcntl locks nor the parent's link-based leases. Plain pthread
// primitives are used so the mutex can be taken in the prepare handler and
// released by the forking thread on both sides, and so the condition variable
// can simply be re-initialized in the child. Registries are created once and
// never destroyed.
class ForkAwareRegistry : boost::noncopyable
{
protected:
   ForkAwareRegistry();
   virtual ~ForkAwareRegistry() {}

   // Called in the child, with the mutex held: drop all inherited state.
   virtual void resetInChild() = 0;

   // Holds the registry mutex for its lifetime.
   class Guard : boost::noncopyable
   {
   public:
      explicit Guard(ForkAwareRegistry& registry);
      ~Guard();

      // Blocks until notifyAll() is called, releasing the mutex meanwhile.
      void wait();

   private:
      ForkAwareRegistry& registry_;
#ifdef _WIN32
      boost::unique_lock<boost::mutex> lock_;
#endif
   };

   // Wakes every Guard::wait(); call with the mutex held.
   void notifyAll();

private:
#ifdef _WIN32
   boost::mutex mutex_;
   boost::condition_variable condition_;
#else
   static void prepareFork();
   static void parentAfterFork();
   static void childAfterFork();

   pthread_mutex_t mutex_;
   pthread_cond_t condition_;
#endif
};

#ifdef _WIN32

inline ForkAwareRegistry::ForkAwareRegistry()
{
}

inline ForkAwareRegistry::Guard::Guard(ForkAwareRegistry& registry)
   : registry_(registry),
     lock_(registry.mutex_)
{
}

inline ForkAwareRegistry::Guard::~Guard()
{
}

inline void ForkAwareRegistry::Guard::wait()
{
   registry_.condition_.wait(lock_);
}

inline void ForkAwareRegistry::notifyAll()
{
   condition_.notify_all();
}

#else

namespace detail {

// The list of registries and the mutex guarding it; the latter is also held
// across fork() so the handlers see a consistent list.
inline pthread_mutex_t& forkAwareRegistriesMutex()
{
   static pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;
   return mutex;
}

inline std::vector<ForkAwareRegistry*>& forkAwareRegistries()
{
   static std::vector<ForkAwareRegistry*>* pInstances =
      new std::vector<ForkAwareRegistry*>();
   return *pInstances;
}

} // namespace detail

inline ForkAwareRegistry::ForkAwareRegistry()
{
   ::pthread_mutex_init(&mutex_, nullptr);
   ::pthread_cond_init(&condition_, nullptr);

   // register the fork handlers once, before the first registry is listed
   struct AtForkRegistration
   {
      AtForkRegistration()
      {
         ::pthread_atfork(prepareFork, parentAfterFork, childAfterFork);
      }
   };
   static AtForkRegistration registration;

   ::pthread_mutex_lock(&detail::forkAwareRegistriesMutex());
   detail::forkAwareRegistries().push_back(this);
   ::pthread_mutex_unlock(&detail::forkAwareRegistriesMutex());
}

inline void ForkAwareRegistry::prepareFork()
{
   ::pthread_mutex_lock(&detail::forkAwareRegistriesMutex());
   for (ForkAwareRegistry* pRegistry : detail::forkAwareRegistries())
      ::pthread_mutex_lock(&pRegistry->mutex_);
}

inline void ForkAwareRegistry::parentAfterFork()
{
   std::vector<ForkAwareRegistry*>& registries = detail::forkAwareRegistries();
   for (auto it = registries.rbegin(); it != registries.rend(); ++it)
      ::pthread_mutex_unlock(&(*it)->mutex_);
   ::pthread_mutex_unlock(&detail::forkAwareRegistriesMutex());
}

inline void ForkAwareRegistry::childAfterFork()
{
   std::vector<ForkAwareRegistry*>& registries = detail::forkAwareRegistries();
   for (auto it = registries.rbegin(); it != registries.rend(); ++it)
   {
      ForkAwareRegistry* pRegistry = *it;
      pRegistry->resetInChild();

      // any waiter belonged to a thread that does not exist in the child
      ::pthread_cond_init(&pRegistry->condition_, nullptr);
      ::pthread_mutex_unlock(&pRegistry->mutex_);
   }
   ::pthread_mutex_unlock(&detail::forkAwareRegistriesMutex());
}

inline ForkAwareRegistry::Guard::Guard(ForkAwareRegistry& registry)
   : registry_(registry)
{
   ::pthread_mutex_lock(&registry_.mutex_);
}

inline ForkAwareRegistry::Guard::~Guard()
{
   ::pthread_mutex_unlock(&registry_.mutex_);
}

inline void ForkAwareRegistry::Guard::wait()
{
   ::pthread_cond_wait(&registry_.condition_, &registry_.mutex_);
}

inline void ForkAwareRegistry::notifyAll()
{
   ::pthread_cond_broadcast(&condition_);
}

#endif

} // namespace file_lock
} // namespace core
} // namespace rstudio

#endif // CORE_FILE_LOCK_FORK_AWARE_REGISTRY_HPP
