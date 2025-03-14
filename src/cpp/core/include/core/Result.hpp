/*
 * Result.hpp
 *
 * Copyright (C) 2023 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef CORE_RESULT_HPP
#define CORE_RESULT_HPP

#include <tl/expected.hpp>

#include <shared_core/Error.hpp>

namespace rstudio {
namespace core {

// Type alias "wrapper" around tl::expected to enforce usage of core::Error as the unexpected type.
// API reference for expected: https://tl.tartanllama.xyz/en/latest/api/expected.html and
// https://en.cppreference.com/w/cpp/utility/expected (which we eventually want to switch to once we upgrade to C++23).
// When using this type prefer the C++23 standard API since we want to switch to that eventually.
// See also core::Unexpected for the unexpected (error) type of core::Result.
//
// Example usage:
// core::Result<float> sqrt(float x)
// {
//    if (x < 0) {return core::Unexpected(core::Error("x must be non-negative"));}
//    return std::sqrt(x);
// }
template <typename T> using Result = tl::expected<T, Error>;

// Type alias for the unexpected (error) type of core::Result.
using Unexpected = tl::unexpected<Error>;

} // end namespace core
} // end namespace rstudio

#endif
