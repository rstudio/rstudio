/*
 * PImpl.hpp
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

#ifndef SHARED_CORE_P_IMPL_HPP
#define SHARED_CORE_P_IMPL_HPP

#include <memory>

/**
 * @brief Shared start of the macro to define a private implementation for a class.
 */
#define PRIVATE_IMPL_START                            \
   struct Impl;

/**
 * @brief Macro to define a private implementation for a non-copyable class.
 *
 * Class which use this macro must either be non-copyable or define a custom deep-copy constructor and operator which
 * performs a deep copy of the impl.
 *
 * This macro should be included in the private or protected section of a classes declaration.
 * The PRIVATE_IMPL_DELETER_IMPL macro must be used in the definition file, after the definition of the Impl struct.
 * struct OwningClass::Impl should be defined in the definition file before defining OwningClass.
 *
 * @param in_memberName    The name of the private implementation member variable (e.g. m_impl).
 */
#define PRIVATE_IMPL(in_memberName)                      \
   PRIVATE_IMPL_START                                    \
   struct ImplDeleter { void operator()(Impl*); };       \
   std::unique_ptr<Impl, ImplDeleter> in_memberName;

/**
 * @brief Macro to define a private implementation which would be shared with copied instances.
 *
 * This macro should be included in the private or protected section of a classes declaration.
 * struct OwningClass::Impl should be defined in the definition file before defining OwningClass.
 *
 * @param in_memeberName    The name of the private implementation member variable (e.g. m_impl).
 */
#define PRIVATE_IMPL_SHARED(in_memberName)   \
   PRIVATE_IMPL_START                        \
   std::shared_ptr<Impl> in_memberName;

/**
 * @brief Macro which implements the deleter for the class's private implementation. This macro must be used after the
 *        implementation of the Impl struct in the definition file.
 *
 * @param in_owningClass    The name of the class which owns the private implementation (e.g. Error).
 */
#define PRIVATE_IMPL_DELETER_IMPL(in_owningClass)                                   \
void in_owningClass::ImplDeleter::operator()(in_owningClass::Impl* io_toDelete)     \
{                                                                                   \
   delete io_toDelete;                                                              \
}

#endif
