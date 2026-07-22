/*
 * Memory.hpp
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

#ifndef CORE_MEMORY_HPP
#define CORE_MEMORY_HPP

#include <utility>

#if defined(__has_feature)
# if __has_feature(address_sanitizer)
#  define RSTUDIO_HAS_LSAN 1
# endif
#elif defined(__SANITIZE_ADDRESS__)
# define RSTUDIO_HAS_LSAN 1
#endif

#ifdef RSTUDIO_HAS_LSAN
# include <sanitizer/lsan_interface.h>
#endif

namespace rstudio {
namespace core {

// construct an object on the heap that is intentionally never destroyed.
//
// use this for objects with static storage duration that background threads
// might touch while the process is exiting: exit() runs static destructors,
// so an ordinary file-scope static can be destroyed while a detached thread
// still holds a reference to it. a leaked object has no destructor to run,
// and the process is exiting anyway, so the leak is unobservable.
//
// typical usage binds the result to a file-scope reference:
//
//    boost::mutex& s_mutex = core::make_leaked<boost::mutex>();
//
// leak checkers do not flag objects created this way: the reference above
// keeps the object reachable, so Valgrind classifies it as "still reachable"
// (not reported by default) and LeakSanitizer does not report it at all. in
// sanitizer builds we additionally mark the allocation as intentionally
// leaked via __lsan_ignore_object.
template <typename T, typename... Args>
T& make_leaked(Args&&... args)
{
   T* pObject = new T(std::forward<Args>(args)...);

#ifdef RSTUDIO_HAS_LSAN
   __lsan_ignore_object(pObject);
#endif

   return *pObject;
}

} // namespace core
} // namespace rstudio

#endif // CORE_MEMORY_HPP
