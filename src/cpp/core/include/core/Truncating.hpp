/*
 * Truncating.hpp
 *
 * Copyright (C) 2024 by Posit Software, PBC
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

#ifndef CORE_TRUNCATING_HPP
#define CORE_TRUNCATING_HPP

#include <limits>
#include <type_traits>

namespace rstudio {
namespace core {

template <typename T>
class Truncating
{
   static_assert(std::is_integral<T>::value, "");

public:

   Truncating(T value)
      : value_(value)
   {
   }

   operator T() const
   {
      return value_;
   }

   Truncating<T> operator+(const T& rhs)
   {
      T result = 0;
      if (__builtin_add_overflow(value_, rhs, &result))
      {
         result = (rhs >= 0)
               ? std::numeric_limits<T>::max()
               : std::numeric_limits<T>::min();
      }
      return Truncating<T>(result);
   }

   Truncating<T> operator-(const T& rhs)
   {
      T result = 0;
      if (__builtin_sub_overflow(value_, rhs, &result))
      {
         result = (rhs >= 0)
               ? std::numeric_limits<T>::min()
               : std::numeric_limits<T>::max();
      }
      return Truncating<T>(result);
   }

   Truncating<T> operator*(const T& rhs)
   {
      T result = 0;
      if (__builtin_mul_overflow(value_, rhs, &result))
      {
         result = ((value_ >= 0) == (rhs >= 0))
               ? std::numeric_limits<T>::max()
               : std::numeric_limits<T>::min();
      }
      return result;
   }

private:
   T value_;
};

} // namespace core
} // namespace rstudio

#endif // CORE_TRUNACTING_HPP

