/*
 * Thread.hpp
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

#ifndef CORE_THREAD_HPP
#define CORE_THREAD_HPP

#include <boost/utility.hpp>
#include <boost/function.hpp>

#include <core/BoostErrors.hpp>
#include <core/BoostThread.hpp>
#include <core/Error.hpp>
#include <core/Log.hpp>



#define LOCK_MUTEX(m) try { \
   boost::lock_guard<boost::mutex> lock(m); 

#define END_LOCK_MUTEX } \
   catch(const boost::thread_resource_error& e) \
   { \
      Error threadError(boost::thread_error::ec_from_exception(e), \
                        ERROR_LOCATION) ; \
      LOG_ERROR(threadError); \
   }

namespace core {
namespace thread {
      
template <typename T>
class ThreadsafeValue : boost::noncopyable
{
public:
   ThreadsafeValue(const T& value) : value_(value) {}
   
   T get()
   {
      LOCK_MUTEX(mutex_)
      {
         return value_;
      }
      END_LOCK_MUTEX
      
      // keep compiler happy
      return T();
   }
   
   void set(const T& value)
   {
      LOCK_MUTEX(mutex_)
      {
         value_ = value;
      }
      END_LOCK_MUTEX
   }
   
private:
   boost::mutex mutex_;
   T value_;
};

void safeLaunchThread(boost::function<void()> threadMain);
      
} // namespace thread
} // namespace core

#endif // CORE_THREAD_HPP

