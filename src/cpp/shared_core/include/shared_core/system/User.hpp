/*
 * User.hpp
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

#ifndef SHARED_CORE_USER_HPP
#define SHARED_CORE_USER_HPP

#include <string>

#ifndef _WIN32
#include <unistd.h>
#endif

#include <shared_core/PImpl.hpp>

namespace rstudio {
namespace core {

class Error;
class FilePath;

} // namespace core
} // namespace rstudio


namespace rstudio {
namespace core {
namespace system {

#ifndef _WIN32
typedef uid_t  UidType;
typedef gid_t  GidType;
#else
namespace detail {

/**
 * @brief Gets an environment variable with the specified name.
 *
 * @param in_name   The name of the environment variable to retrieve.
 *
 * @return The value of the environment variable.
 */
std::string getenv(const std::string& name);

} // namespace detail

#endif

/**
 * @brief Class which represents a system user.
 */
class User
{
public:

   /**
    * @brief Gets the user home path, as set in the environment.
    *
    * @param in_envOverride     If set, overrides the name of the environment variable to use as the user's home path.
    *                           Multiple overrides may be specified by delimiting them with '|' in order of precedence.
    *
    * @return The user home path, as set in the environment.
    */
   static FilePath getUserHomePath(const std::string& in_envOverride = std::string());

#ifndef _WIN32

   /**
    * @brief Creates a user by user ID.
    *
    * @param in_userId      The ID of the user to create.
    * @param out_user       The created user.
    *
    * @return Success if the user could be retrieved; Error otherwise.
    */
   static Error createUser(UidType in_userId, User& out_user);

   /**
    * @brief Gets the current user.
    *
    * @param out_currentUser    The user this process is currently executing on behalf of. This object will be the empty
    *                           user if this function returns an error.
    *
    * @return Success if the user could be retrieved; Error otherwise.
    */
   static Error getCurrentUser(User& out_currentUser);

   /**
    * @brief Gets the ID of this user's primary group.
    *
    * @return The ID of this user's primary group.
    */
   GidType getGroupId() const;

   /**
    * @brief Gets the ID of this user.
    *
    * @return The ID of this user.
    */
   UidType getUserId() const;

   /**
    * @brief Default constructor.
    *
    * Creates a user object which represents all users.
    */
    User();

   /**
    * @brief Copy constructor.
    *
    * @param in_other   The user to copy.
    */
   User(const User& in_other);

   /**
    * @brief Creates a user by username.
    *
    * @param in_username    The name of the user to create.
    * @param out_user       The created user.
    *
    * @return Success if the user could be retrieved; Error otherwise.
    */
   static Error createUser(const std::string& in_username, User& out_user);

   /**
    * @brief Checks whether the user represented by this object exists.
    *
    * If this is an empty user, or is a user object which represents all users, this method will return false as it does
    * not represent a user which exists on the system.
    *
    * @return True if this user exists; false otherwise.
    */
   bool exists() const;

   /**
    * @brief Returns whether this object represents all users or not. See the default constructor for more details.
    *
    * @return True if this object represents all users; false otherwise.
    */
   bool isAllUsers() const;

   /**
    * @brief Checks whether this user is empty or not.
    * @return
    */
   bool isEmpty() const;

   /**
    * @brief Gets the user home path, if it exists.
    *
    * @return The user's home path, if it exists; empty path otherwise.
    */
   const FilePath& getHomePath() const;

   /**
    * @brief Returns the name of this user.
    *
    * @return The name of this user ("*" for all users).
    */
   const std::string& getUsername() const;

   /**
    * @brief Overloaded assignment operator.
    *
    * @param in_other   The user to copy to this one.
    *
    * @return This user.
    */
   User& operator=(const User& in_other);

private:
   // The private implementation of User.
   PRIVATE_IMPL(m_impl);

#else
   // No construction on windows.
   User() = delete;

#endif
};

} // namesapce system
} // namespace core
} // namespace rstudio

#endif
