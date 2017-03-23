/*
 * TypeTraits.hpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */


#ifndef CORE_TYPETRAITS_HPP
#define CORE_TYPETRAITS_HPP

#include <boost/type_traits.hpp>

namespace rstudio {
namespace core {
namespace type_traits {

#define RS_GENERATE_HAS_TYPE_TRAIT(__NAME__)                                   \
   template <typename T> struct has_##__NAME__##_impl                          \
   {                                                                           \
      template <typename U, typename V> struct SFINAE                          \
      {                                                                        \
      };                                                                       \
                                                                               \
      template <typename U>                                                    \
      static char test(SFINAE<U, typename U::__NAME__>*);                      \
      template <typename U> static int test(...);                              \
                                                                               \
      static const bool value = sizeof(test<T>(0)) == sizeof(char);            \
   };                                                                          \
                                                                               \
   template <typename T>                                                       \
   struct has_##__NAME__                                                       \
       : public boost::integral_constant<bool,                                 \
                                         has_##__NAME__##_impl<T>::value>      \
   {                                                                           \
   }

RS_GENERATE_HAS_TYPE_TRAIT(key_type);

} // namespace type_traits
} // namespace core
} // namespace rstudio

#endif // CORE_TYPETRAITS_HPP

