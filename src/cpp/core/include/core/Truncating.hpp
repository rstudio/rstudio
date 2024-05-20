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
   static_assert(std::is_integral<T>::value || std::is_floating_point<T>::value, "");
   static const T min = std::numeric_limits<T>::min();
   static const T max = std::numeric_limits<T>::max();

   template <typename U>
   void static_assert_compatible_types()
   {
      static_assert(sizeof(T) >= sizeof(U), "");
      static_assert(std::is_floating_point<T>::value == std::is_floating_point<U>::value, "");
      static_assert(std::is_integral<T>::value == std::is_integral<U>::value, "");
      static_assert(std::is_signed<T>::value == std::is_signed<U>::value, "");
   }

public:

   Truncating(T value)
      : value_(value)
   {
   }

   operator T() const
   {
      return value_;
   }

   // Addition ----
   static T add(const T& lhs, const T& rhs)
   {
      if (rhs > 0 && lhs > (max - rhs))
      {
         return max;
      }
      else if (rhs < 0 && lhs < (min - rhs))
      {
         return min;
      }
      else
      {
         return lhs + rhs;
      }
   }

   Truncating<T> operator+(const T& rhs)
   {
      return add(value_, rhs);
   }

   Truncating<T> operator+(const Truncating<T>& rhs)
   {
      return add(value_, rhs);
   }

   template <typename U>
   Truncating<T> operator+(const U& rhs)
   {
      static_assert_compatible_types<U>();
      return add(value_, static_cast<T>(rhs));
   }

   template <typename U>
   Truncating<T> operator+(const Truncating<U>& rhs)
   {
      static_assert_compatible_types<U>();
      return add(value_, static_cast<T>(rhs));
   }


   // Subtraction ----
   static T sub(const T& lhs, const T& rhs)
   {
      if (rhs > 0 && lhs < min + rhs)
      {
         return min;
      }
      else if (rhs < 0 && lhs > max + rhs)
      {
         return max;
      }
      else
      {
         return lhs - rhs;
      }
   }

   Truncating<T> operator-(const T& rhs)
   {
      return sub(value_, rhs);
   }

   Truncating<T> operator-(const Truncating<T>& rhs)
   {
      return sub(value_, rhs);
   }

   template <typename U>
   Truncating<T> operator-(const U& rhs)
   {
      static_assert_compatible_types<U>();
      return sub(value_, static_cast<T>(rhs));
   }

   template <typename U>
   Truncating<T> operator-(const Truncating<U>& rhs)
   {
      static_assert_compatible_types<U>();
      return sub(value_, static_cast<T>(rhs));
   }


   // Multiplication ----
   static T mul(const T& lhs, const T& rhs)
   {
      if (lhs == 0 || rhs == 0)
      {
         return 0;
      }

      T overflow = ((lhs > 0) != (rhs > 0)) ? min : max;

      if (lhs > 0)
      {
         if (rhs > 0)
         {
            if (lhs > (max / rhs))
            {
               return overflow;
            }
         }
         else
         {
            if (rhs < (min / lhs))
            {
               return overflow;
            }
         }
      }
      else
      {
         if (rhs > 0)
         {
            if (lhs < (min / rhs))
            {
               return overflow;
            }
         }
         else
         {
            if (rhs < (max / lhs))
            {
               return overflow;
            }
         }
      }

      return lhs * rhs;
   }

   Truncating<T> operator*(const T& rhs)
   {
      return mul(value_, rhs);
   }

   Truncating<T> operator*(const Truncating<T>& rhs)
   {
      return mul(value_, rhs);
   }

   template <typename U>
   Truncating<T> operator*(const U& rhs)
   {
      static_assert_compatible_types<U>();
      return mul(value_, static_cast<U>(rhs));
   }

   template <typename U>
   Truncating<T> operator*(const Truncating<U>& rhs)
   {
      static_assert_compatible_types<U>();
      return mul(value_, static_cast<U>(rhs));
   }


private:
   T value_;
};

} // namespace core
} // namespace rstudio

#endif // CORE_TRUNACTING_HPP

