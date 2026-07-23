/*
 * Memory.hpp
 *
 * Copyright (C) 2026 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant to the terms of a commercial license agreement
 * with Posit, then this program is licensed to you under the following terms:
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

#ifndef SHARED_CORE_MEMORY_HPP
#define SHARED_CORE_MEMORY_HPP

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

#endif // SHARED_CORE_MEMORY_HPP
