/*
 * ReaderWriterMutex.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant to the terms of a commercial license agreement
 * with RStudio, then this program is licensed to you under the following terms:
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

#include <shared_core/ReaderWriterMutex.hpp>

#include <boost/thread/condition_variable.hpp>
#include <boost/thread/recursive_mutex.hpp>

namespace rstudio {
namespace core {
namespace thread {

typedef boost::unique_lock<boost::recursive_mutex> Lock;

// ReaderWriterMutex ===================================================================================================
struct ReaderWriterMutex::Impl
{
   bool IsWriting;
   unsigned int ReaderCount;
   boost::recursive_mutex Mutex;
   // This mutex is used to allow re-entrant lock behaviour on write. On read it's basically already re-entrant.
   boost::recursive_mutex WriteMutex;
   boost::condition_variable_any Condition;
};

PRIVATE_IMPL_DELETER_IMPL(ReaderWriterMutex);

ReaderWriterMutex::ReaderWriterMutex() :
   m_impl(new Impl())
{
   m_impl->IsWriting = false;
   m_impl->ReaderCount = 0;
}

ReaderWriterMutex::ReaderWriterMutex(ReaderWriterMutex&& in_other) noexcept :
   m_impl(std::move(in_other.m_impl))
{
}

void ReaderWriterMutex::lockRead()
{
   // Acquire the lock.
   Lock lock(m_impl->Mutex);

   // Wait until writers are finished, unless we can acquire the write lock, because that means the writer is in this
   // thread.
   bool haveWriteLock = false;
   if (m_impl->IsWriting)
   {
      haveWriteLock = m_impl->WriteMutex.try_lock();
      while (!haveWriteLock && m_impl->IsWriting)
      {
         m_impl->Condition.wait(lock);
         haveWriteLock = m_impl->WriteMutex.try_lock();
      }
   }

   // Increment the number of readers.
   ++m_impl->ReaderCount;

   // Safely read. Also unlock the write lock if we locked it before.
   if (haveWriteLock)
      m_impl->WriteMutex.unlock();
}

// This implementation is write-preferring because no more readers can start reading while there is a thread that
// wishes to obtain the lock for write. Such an implementation is desirable when there would be many read operations and
// few write operations. In such a case, a read-preferring implementation would result in write operations getting
// effectively locked out.
void ReaderWriterMutex::lockWrite()
{
   // Acquire the lock.
   Lock lock(m_impl->Mutex);

   // Wait until we can get access to the write lock.
   while (!m_impl->WriteMutex.try_lock())
      m_impl->Condition.wait(lock);

   // Notify other threads that you're waiting to write.
   m_impl->IsWriting = true;

   // Wait until current readers are finished.
   while (m_impl->ReaderCount > 0)
      m_impl->Condition.wait(lock);

   // Safely write.
}

void ReaderWriterMutex::unlockRead()
{
   // Acquire the lock.
   Lock lock(m_impl->Mutex);

   // If this wasn't erroneously called, decrement the reader count.
   if (m_impl->ReaderCount > 0)
   {
      --m_impl->ReaderCount;

      // If there are no more readers, notify any waiters.
      if (m_impl->ReaderCount == 0)
         m_impl->Condition.notify_all();
   }
}

void ReaderWriterMutex::unlockWrite()
{
   Lock lock(m_impl->Mutex);

   // If this wasn't erroneously called, reset IsWriting and notify any waiters.
   if (m_impl->IsWriting)
   {
      m_impl->WriteMutex.unlock();
      m_impl->IsWriting = false;
      m_impl->Condition.notify_all();
   }
}

// ReaderLock ==========================================================================================================
ReaderLock::ReaderLock(ReaderWriterMutex& in_mutex) :
   m_mutex(in_mutex)
{
   m_mutex.lockRead();
}

ReaderLock::~ReaderLock()
{
   m_mutex.unlockRead();
}

// WriterLock ==========================================================================================================
WriterLock::WriterLock(ReaderWriterMutex& in_mutex) :
   m_mutex(in_mutex)
{
   m_mutex.lockWrite();
}

WriterLock::~WriterLock()
{
   m_mutex.unlockWrite();
}

} // namespace thread
} // namespace rstudio
} // namespace core
