/*
 * Algorithm.hpp
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


#ifndef CORE_ALGORITHM_HPP
#define CORE_ALGORITHM_HPP

#include <vector>
#include <algorithm>

#include <boost/type_traits.hpp>
#include "../type_traits/TypeTraits.hpp"

namespace rstudio {
namespace core {
namespace algorithm {

template<typename InputIterator, typename OutputIterator, typename Predicate>
OutputIterator copy_if(InputIterator begin,
                       InputIterator end,
                       OutputIterator destBegin,
                       Predicate p)
{
   while (begin != end)
   {
      if (p(*begin))
        *destBegin++ = *begin;
      ++begin;
   }
   return destBegin;
}

template<typename InputIterator,
         typename OutputIterator,
         typename Predicate,
         typename UnaryOperator>
OutputIterator copy_transformed_if(InputIterator begin,
                                   InputIterator end,
                                   OutputIterator destBegin,
                                   Predicate p,
                                   UnaryOperator op)
{
   while (begin != end)
   {
      if (p(*begin))
        *destBegin++ = op(*begin);
      ++begin;
   }
   return destBegin;
}

namespace detail {

template <typename Container, typename ValueType>
typename boost::enable_if_c< type_traits::has_key_type<Container>::value, bool >::type
contains(const Container& container, const ValueType& value, boost::true_type)
{
   return container.count(value);
}

template <typename Container, typename ValueType>
typename boost::disable_if_c< type_traits::has_key_type<Container>::value, bool >::type
contains(const Container& container, const ValueType& value, boost::false_type)
{
   return std::find(container.begin(), container.end(), value) != container.end();
}

} // namespace detail

template <typename Container, typename ValueType>
bool contains(const Container& container, const ValueType& value)
{
   return detail::contains(
            container,
            value,
            type_traits::has_key_type_t<Container>());
}

/* Wrappers for the erase-remove idiom */
template <typename Container, typename ValueType>
void discard(Container& container, const ValueType& value)
{
   container.erase(std::remove(container.begin(), container.end(), value), container.end());
}

template <typename Container, typename Predicate>
void discard_if(Container& container, Predicate predicate)
{
   container.erase(std::remove_if(container.begin(), container.end(), predicate), container.end());
}

template <typename Container, typename ValueType>
Container without(Container& container, const ValueType& value)
{
   container.erase(std::remove(container.begin(), container.end(), value), container.end());
}

template <typename Container, typename Predicate>
Container without_if(Container& container, Predicate predicate)
{
   container.erase(std::remove_if(container.begin(), container.end(), predicate), container.end());
}

template <typename T>
std::vector<T> seq(T length)
{
   std::vector<T> result;
   result.reserve(length);
   for (std::size_t i = 0; i < length; ++i)
      result.push_back(i);
   return result;
}

} // namespace algorithm
} // namespace core
} // namespace rstudio


#endif // CORE_ALGORITHM_HPP
