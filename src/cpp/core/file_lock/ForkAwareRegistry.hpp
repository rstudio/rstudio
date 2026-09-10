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

#include <memory>

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
//
// instance() constructs and publishes a registry while holding the mutex
// used by the fork handlers. Its pointer storage must be constant-initialized:
// a function-local static initializer has a separate C++ guard that a child
// can inherit in the busy state, even after the registry was published.
class ForkAwareRegistry : boost::noncopyable
{
public:
   // Lazily constructs an immortal registry. Pass a pointer initialized to
   // nullptr, and access that pointer only through this function.
   template <typename Registry>
   static Registry& instance(Registry*& pInstance)
   {
      Registry* pRegistry;
#ifdef RSTUDIO_UNIT_TESTS_ENABLED
      bool published = false;
#endif
      {
         InitializationGuard guard;
         if (!pInstance)
         {
            std::unique_ptr<Registry> registry(new Registry());
            publishRegistry(registry.get());
            pInstance = registry.release();
#ifdef RSTUDIO_UNIT_TESTS_ENABLED
            published = true;
#endif
         }
         pRegistry = pInstance;
      }

#ifdef RSTUDIO_UNIT_TESTS_ENABLED
      // Let a test fork after publication, while the initializing call is
      // still in progress. This must run outside the initialization mutex.
      if (published)
         static_cast<ForkAwareRegistry*>(pRegistry)->afterPublishForTesting();
#endif
      return *pRegistry;
   }

protected:
   ForkAwareRegistry();
   virtual ~ForkAwareRegistry() {}

   // Called in the child, with the mutex held: drop all inherited state.
   virtual void resetInChild() = 0;

#ifdef RSTUDIO_UNIT_TESTS_ENABLED
   virtual void afterPublishForTesting() {}
#endif

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
   class InitializationGuard : boost::noncopyable
   {
   public:
      InitializationGuard();
      ~InitializationGuard();
   };

   static void publishRegistry(ForkAwareRegistry* pRegistry);

#ifdef _WIN32
   boost::mutex mutex_;
   boost::condition_variable condition_;
#else
   static void prepareFork();
   static void parentAfterFork();
   static void childAfterFork();

   struct AtForkRegistration
   {
      AtForkRegistration();
   };
   static AtForkRegistration s_atForkRegistration;

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

inline ForkAwareRegistry::ForkAwareRegistry()
{
   ::pthread_mutex_init(&mutex_, nullptr);
   ::pthread_cond_init(&condition_, nullptr);
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
