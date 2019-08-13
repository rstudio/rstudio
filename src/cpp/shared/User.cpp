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

#include <shared/User.hpp>

namespace rstudio {
namespace shared {

struct User::Impl
{
   explicit Impl(std::string in_name) : Name(std::move(in_name)) {};

   std::string Name;
};

PRIVATE_IMPL_DELETER_IMPL(User)

User::User() :
   m_impl(new Impl("*"))
{
}

User::User(const User& in_other) :
   m_impl(new Impl(in_other.m_impl->Name))
{

}

User::User(std::string in_username) :
   m_impl(new Impl(std::move(in_username)))
{
}

bool User::isAllUsers() const
{
   return m_impl->Name == "*";
}

const std::string& User::getUsername() const
{
   return m_impl->Name;
}

User& User::operator=(const User& in_other)
{
   m_impl->Name = in_other.m_impl->Name;
   return *this;
}

} // namespace shared
} // namespace rstudio

