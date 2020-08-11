/*
 * User.cpp
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

#include <shared_core/system/User.hpp>

#include <pwd.h>

#include <boost/algorithm/string.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>
#include <shared_core/SafeConvert.hpp>
#include <shared_core/system/PosixSystem.hpp>

namespace rstudio {
namespace core {
namespace system {

struct User::Impl
{
   template<class T>
   using GetPasswdFunc = std::function<int(T, struct passwd*, char*, size_t, struct passwd**)>;

   Impl() : UserId(-1), GroupId(-1)
   { };

   template<typename T>
   Error populateUser(const GetPasswdFunc<T>& in_getPasswdFunc, T in_value)
   {
      struct passwd pwd;
      struct passwd* tempPtrPwd;

      // Get the maximum size of a passwd for this system.
      long buffSize = ::sysconf(_SC_GETPW_R_SIZE_MAX);
      if (buffSize < 0)
         buffSize = 4096; // some systems return -1, be conservative!

      std::vector<char> buffer(buffSize);
      int result = in_getPasswdFunc(in_value, &pwd, &(buffer[0]), buffSize, &tempPtrPwd);
      if (tempPtrPwd == nullptr)
      {
         Error error;
         if (result == 0)
         {
            // A successful result code but no user details means that we couldn't find the user.
            // This could stem from a permissions issue but is more likely just an incorrectly
            // formed username.
            error = systemError(ENOENT, "User not found.", ERROR_LOCATION);
         }
         else
         {
            error = systemError(result, "Failed to get user details.", ERROR_LOCATION);
         }

         error.addProperty("user-value", safe_convert::numberToString(in_value));
         return error;
      }
      else
      {
         UserId = pwd.pw_uid;
         GroupId = pwd.pw_gid;
         Name = pwd.pw_name;
         HomeDirectory = FilePath(pwd.pw_dir);
         Shell = pwd.pw_shell;
      }

      return Success();
   }

   UidType UserId;
   GidType GroupId;
   std::string Name;
   FilePath HomeDirectory;
   std::string Shell;
};

PRIVATE_IMPL_DELETER_IMPL(User)

User::User(bool in_isEmpty) :
   m_impl(new Impl())
{
   m_impl->Name = in_isEmpty ? "" : "*";
}

User::User(const User& in_other) :
   m_impl(new Impl(*in_other.m_impl))
{
}

Error User::getCurrentUser(User& out_currentUser)
{
   return getUserFromIdentifier(::geteuid(), out_currentUser);
}

Error User::getUserFromIdentifier(const std::string& in_username, User& out_user)
{
   User user;

   Error error = user.m_impl->populateUser<const char*>(::getpwnam_r, in_username.c_str());
   if (!error)
      out_user = user;

   return error;
}

Error User::getUserFromIdentifier(UidType in_userId, User& out_user)
{
   User user;
   Error error = user.m_impl->populateUser<UidType>(::getpwuid_r, in_userId);
   if (!error)
      out_user = user;

   return error;
}

FilePath User::getUserHomePath(const std::string& in_envOverride)
{
   // use environment override if specified
   if (!in_envOverride.empty())
   {
      using namespace boost::algorithm;
      for (split_iterator<std::string::const_iterator> it =
         make_split_iterator(in_envOverride, first_finder("|", is_iequal()));
           it != split_iterator<std::string::const_iterator>();
           ++it)
      {
         std::string envHomePath = posix::getEnvironmentVariable(boost::copy_range<std::string>(*it));
         if (!envHomePath.empty())
         {
            FilePath userHomePath(envHomePath);
            if (userHomePath.exists())
               return userHomePath;
         }
      }
   }

   // otherwise use standard unix HOME
   return FilePath(posix::getEnvironmentVariable("HOME"));
}

User& User::operator=(const User& in_other)
{
   if (this == &in_other)
      return *this;

   if ((m_impl == nullptr) && (in_other.m_impl == nullptr))
      return *this;

   if (in_other.m_impl == nullptr)
   {
      m_impl.reset();
      return *this;
   }

   if (m_impl == nullptr)
      m_impl.reset(new Impl());

   *m_impl = *in_other.m_impl;

   return *this;
}

bool User::operator==(const User& in_other) const
{
   // If one or the other is empty but not both, these objects aren't equal.
   if (isEmpty() != in_other.isEmpty())
      return false;

   // Otherwise they're both empty or they're both not, so just return true if this user is empty.
   if (isEmpty())
      return true;

   // If one or the other is all users but not both, these aren't the same user.
   if (isAllUsers() != in_other.isAllUsers())
      return false;

   // Otherwise they're both all users or they're both not, so just return true if this user is all users.
   if (isAllUsers())
      return true;

   return getUserId() == in_other.getUserId();
}

bool User::operator!=(const User &in_other) const
{
   return !(*this == in_other);
}

bool User::exists() const
{
   return !isEmpty() && !isAllUsers();
}

bool User::isAllUsers() const
{
   return m_impl->Name == "*";
}

bool User::isEmpty() const
{
   return m_impl->Name.empty();
}

const FilePath& User::getHomePath() const
{
   return m_impl->HomeDirectory;
}

GidType User::getGroupId() const
{
   return m_impl->GroupId;
}

UidType User::getUserId() const
{
   return m_impl->UserId;
}

const std::string& User::getUsername() const
{
   return m_impl->Name;
}

const std::string& User::getShell() const
{
   return m_impl->Shell;
}

} // namespace system
} // namespace core
} // namespace rstudio

