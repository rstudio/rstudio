/*
 * User.cpp
 * 
 * Copyright (C) 2019 by RStudio, Inc.
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

#include <shared_core/User.hpp>

#include <pwd.h>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>
#include <shared_core/SafeConvert.hpp>

namespace rstudio {
namespace core {

struct User::Impl
{
   template <class T>
   using GetPasswdFunc = std::function<int(T, struct passwd*, char*, size_t, struct passwd**)>;

   Impl() : UserId(0), GroupId(0) {};

   template <typename T>
   void populateUser(const GetPasswdFunc<T>& in_getPasswdFunc, T in_value)
   {
      struct passwd pwd;
      struct passwd* tempPtrPwd;

      // Get the maximum size of a passwd for this system.
      long buffSize = ::sysconf(_SC_GETPW_R_SIZE_MAX);
      if (buffSize == 1)
         buffSize = 4096; // some systems return -1, be conservative!

      std::vector<char> buffer(buffSize);
      int result = in_getPasswdFunc(in_value, &pwd, &(buffer[0]), buffSize, &tempPtrPwd);
      if (tempPtrPwd == nullptr)
      {
         if (result == 0) // will happen if user is simply not found. Return EACCES (not found error).
            result = EACCES;

         UserRetrievalError = systemError(result, "Failed to get user details.", ERROR_LOCATION);
         UserRetrievalError.addProperty("user-value", safe_convert::numberToString(in_value));
      }
      else
      {
         UserId = pwd.pw_uid;
         GroupId = pwd.pw_gid;
         Name = pwd.pw_name;
         HomeDirectory = FilePath(pwd.pw_dir);
      }
   }

   UidType UserId;
   GidType GroupId;
   std::string Name;
   FilePath HomeDirectory;
   Error UserRetrievalError;
};

PRIVATE_IMPL_DELETER_IMPL(User)

User::User(const User& in_other) :
   m_impl(new Impl(*in_other.m_impl))
{
}

User::User(bool in_isAllUsers) :
   m_impl(new Impl())
{
   if (in_isAllUsers)
      m_impl->Name = "*";
}

User::User(const std::string& in_username) :
   User()
{
   m_impl->populateUser<const char*>(::getpwnam_r, in_username.c_str());
   if (m_impl->UserRetrievalError)
   {
      // Log the error an ensure that the username is set.
      logError(m_impl->UserRetrievalError);
      m_impl->Name = in_username;
   }
}

User::User(UidType in_userId) :
   User()
{
   m_impl->populateUser<UidType>(::getpwuid_r, in_userId);
   if (m_impl->UserRetrievalError)
   {
      // Log the error an ensure that the user ID is set.
      logError(m_impl->UserRetrievalError);
      m_impl->UserId = in_userId;
   }
}

Error User::getCurrentUser(User& out_currentUser)
{
   out_currentUser = User(::geteuid());
   return out_currentUser.m_impl->UserRetrievalError;
}

bool User::exists() const
{
   return !m_impl->UserRetrievalError && !isEmpty() && !isAllUsers();
}

bool User::isAllUsers() const
{
   return m_impl->Name == "*";
}

bool User::isEmpty() const
{
   return m_impl->Name.empty();
}

GidType User::getGroupId() const
{
   return m_impl->GroupId;
}

const FilePath& User::getHomePath() const
{
   return m_impl->HomeDirectory;
}

const std::string& User::getUsername() const
{
   return m_impl->Name;
}

UidType User::getUserId() const
{
   return m_impl->UserId;
}

User& User::operator=(const User& in_other)
{
   m_impl->Name = in_other.m_impl->Name;
   m_impl->UserId = in_other.m_impl->UserId;
   m_impl->GroupId = in_other.m_impl->GroupId;
   m_impl->HomeDirectory = in_other.m_impl->HomeDirectory;
   return *this;
}

} // namespace core
} // namespace rstudio

