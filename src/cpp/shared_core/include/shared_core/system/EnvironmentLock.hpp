/*
 * EnvironmentLock.hpp
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

#ifndef SHARED_CORE_SYSTEM_ENVIRONMENT_LOCK_HPP
#define SHARED_CORE_SYSTEM_ENVIRONMENT_LOCK_HPP

namespace rstudio {
namespace core {
namespace system {

/**
 * @brief Scope guard serializing access to the process environment.
 *
 * On POSIX, glibc's setenv can reallocate (and free) the environ array while
 * a concurrent getenv walks it without locking, so unsynchronized cross-thread
 * access can dereference a freed array (see rstudio-pro#4628, #10756). Take
 * this lock around any direct ::getenv / ::setenv / ::unsetenv / environ
 * access; the core::system environment accessors take it internally. Direct
 * environment reads made by third-party code (libc internals, R) cannot be
 * covered.
 *
 * On Windows the Get/SetEnvironmentVariable APIs already serialize
 * internally; the lock is provided there for uniform cross-platform semantics
 * (and cross-call atomicity), so callers need not special-case platforms.
 *
 * The lock is not recursive: do not call the core::system environment
 * accessors (or otherwise re-acquire it) while holding it. On POSIX it
 * participates in pthread_atfork, so a fork never snapshots it in the locked
 * state and forked children may safely read the environment before exec.
 */
class EnvironmentLock
{
public:
   EnvironmentLock();
   ~EnvironmentLock();

private:
   EnvironmentLock(const EnvironmentLock&);
   EnvironmentLock& operator=(const EnvironmentLock&);
};

} // namespace system
} // namespace core
} // namespace rstudio

#endif // SHARED_CORE_SYSTEM_ENVIRONMENT_LOCK_HPP
