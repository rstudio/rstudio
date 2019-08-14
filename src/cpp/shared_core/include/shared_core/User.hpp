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

#include "PImpl.hpp"

namespace rstudio {
namespace core {

/**
 * @brief Class which represents a system user.
 */
class User
{
public:
   /**
    * @brief Default Constructor.
    *
    * Creates a user object which represents all users. It should be used for Launcher requests that apply to all users
    * (e.g. get jobs for all users). Username == "*".
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
    * @param in_username    The name of the user.
    */
   explicit User(std::string in_username);

   /**
    * @brief Returns whether this object represents all users or not. See the default constructor for more details.
    *
    * @return True if this object represents all users; false otherwise.
    */
   bool isAllUsers() const;

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
};

} // namespace core
} // namespace rstudio

#endif
