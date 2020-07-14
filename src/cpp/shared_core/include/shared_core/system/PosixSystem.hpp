/*
 * PosixSystem.hpp
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

#ifndef SHARED_CORE_POSIX_SYSTEM_HPP
#define SHARED_CORE_POSIX_SYSTEM_HPP

#include <functional>

#include <shared_core/Error.hpp>

namespace rstudio {
namespace core  {
namespace system {

class User;

} // namespace system
} // namespace core
} // namespace rstudio

namespace rstudio {
namespace core {
namespace system {
namespace posix {

/**
 * @file
 * Posix System Utilities.
 */

/**
 * @brief Represents an IP address.
 */
struct IpAddress
{
   /** The name of the IP address. */
   std::string Name;

   /** The address of the IP address. */
   std::string Address;
};

/**
 * @brief Enables core dumps for this process.
 *
 * @return Success if core dumps could be enabled; Error otherwise.
 */
Error enableCoreDumps();

/**
 * @brief Gets an environment variable from the system.
 *
 * @param in_name   The name of the environment variable.
 *
 * @return The value of the environment variable, if it exists; empty string otherwise.
 */
std::string getEnvironmentVariable(const std::string& in_name);

/**
 * @brief Gets the IP addresses of the machine running this process.
 *
 * @param out_addresses         The IP addresses of the machine running this process.
 * @param in_includeIPv6        Whether or not to include IPv6 addresses. Default: false.
 *
 * @return Success if the IP addresses could be retrieved; Error otherwise.
 */
core::Error getIpAddresses(std::vector<IpAddress>& out_addresses, bool in_includeIPv6 = false);

/**
 * @brief Ignores a particular signal for this process.
 *
 * @param in_signal     The signal to ignore.
 *
 * @return Success if the specified signal could be ignored; Error otherwise.
 */
Error ignoreSignal(int in_signal);

/**
 * @brief Makes a posix call and handles EINTR retries.
 *
 * Only for use with functions that return -1 on error and set errno.
 *
 * @tparam T        The return type of the function to be called.
 *
 * @param in_posixFunction      The function to call.
 *
 * @return  The return value of the provided function.
 */
template <typename T>
T posixCall(const std::function<T()>& in_posixFunction)
{
   const T ERR = -1;

   T result;
   while (true)
   {
      result = in_posixFunction();

      if (result == ERR && errno == EINTR)
         continue;
      else
         break;
   }

   return result;
}

/**
 * @brief Makes a posix call and handles EINTR retries.
 *
 * Only for use with functions that return -1 on error and set errno.
 *
 * @tparam T        The return type of the function to be called.
 *
 * @param in_posixFunction      The function to call.
 * @param in_errorLocation      The location at which this function was invoked.
 * @param out_result            Optional output parameter on which the result of in_posixFUnction will be set.
 *
 * @return Success if the posix function was invoked and ran successfully; Error otherwise.
 */
template <typename T>
Error posixCall(const std::function<T()>& in_posixFunction,
                const ErrorLocation& in_errorLocation,
                T* out_result = nullptr)
{
   const T ERR = -1;

   // make the call
   T result = posixCall<T>(in_posixFunction);

   // set out param (if requested)
   if (out_result)
      *out_result = result;

   // return status
   if (result == ERR)
      return systemError(errno, in_errorLocation);
   else
      return Success();
}

/**
 * @brief Checks whether the real user (not the effective user) running this process is root.
 *
 * @return True if the real user is root; false otherwise.
 */
bool realUserIsRoot();

/**
 * @brief Restores root privileges.
 *
 * @return Success if root privileges could be restored; Error otherwise.
 */
Error restoreRoot();

/**
 * @brief Restores privileges of the previous user, whose privileges were dropped by calling temporarilyDropPrivileges.
 *
 * @return Success if privileges could be restored; Error otherwise.
 */
Error restorePrivileges();

/**
 * @brief Temporarily drops privileges from root to the requested user.
 *
 * @param in_user   The user to which to drop privileges.
 *
 * @return Success if privileges could be dropped to the requested user; Error otherwise.
 */
Error temporarilyDropPrivileges(const User& in_user);

} // namespace posix
} // namespace system
} // namespace core
} // namespace rstudio

#endif
