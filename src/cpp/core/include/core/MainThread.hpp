/*
 * MainThread.hpp
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

#ifndef CORE_MAIN_THREAD_HPP
#define CORE_MAIN_THREAD_HPP

#include <string>

#include <shared_core/Error.hpp>

// NOTE: this header is deliberately lightweight -- no core/Thread.hpp or
// boost thread dependencies -- so that it can be included from headers
// (e.g. r/RExec.hpp) that cannot tolerate the system headers Thread.hpp
// pulls in

#define ASSERT_MAIN_THREAD(__REASON__)                                         \
   (::rstudio::core::thread::assertMainThread(                                 \
      { __REASON__ },                                                          \
      BOOST_CURRENT_FUNCTION,                                                  \
      ERROR_LOCATION))

// like ASSERT_MAIN_THREAD, but performs no work at all on the main-thread
// fast path: the thread check short-circuits before the ErrorLocation (which
// heap-allocates) or any diagnostic string is built. requireMainThread
// resolves via unqualified lookup, so a class may supply a member with custom
// failure handling (see r::exec::RFunction); elsewhere, bring the default
// below into scope with 'using namespace rstudio::core::thread'.
#define REQUIRE_MAIN_THREAD()                                                  \
   (::rstudio::core::thread::isMainThread() ||                                 \
      requireMainThread(BOOST_CURRENT_FUNCTION, ERROR_LOCATION))

namespace rstudio {
namespace core {
namespace thread {

bool isMainThread();

// logs an error and returns false when invoked off the main thread; the
// reason string is built by the caller, so prefer REQUIRE_MAIN_THREAD()
// on hot paths
bool assertMainThread(
      const std::string& reason,
      const std::string& functionName,
      const core::ErrorLocation& errorLocation);

// logs an error and returns false when invoked off the main thread
bool requireMainThread(
      const char* functionName,
      const core::ErrorLocation& errorLocation);

} // namespace thread
} // namespace core
} // namespace rstudio

#endif // CORE_MAIN_THREAD_HPP
