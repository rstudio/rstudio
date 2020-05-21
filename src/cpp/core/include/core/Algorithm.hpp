/*
 * Algorithm.hpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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
#include <boost/algorithm/string.hpp>

#include <core/Macros.hpp>
#include <core/StringUtils.hpp>
#include <core/type_traits/TypeTraits.hpp>

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
            type_traits::has_key_type<Container>());
}

namespace detail {

template <typename Container, typename ValueType>
typename boost::enable_if_c<type_traits::has_key_type<Container>::value, void>::type
insert(Container& container, const ValueType& value, boost::true_type)
{
   container.insert(value);
}

template <typename Container, typename ValueType>
typename boost::disable_if_c<type_traits::has_key_type<Container>::value, void>::type
insert(Container& container, const ValueType& value, boost::false_type)
{
   container.push_back(value);
}

} // namespace detail

template <typename Container, typename ValueType>
void insert(Container& container, const ValueType& value)
{
   detail::insert(container, value, type_traits::has_key_type<Container>());
}

namespace detail {

template <typename Container, typename Iterator>
typename boost::enable_if_c<type_traits::has_key_type<Container>::value, void>::type
insert(Container& container, Iterator begin, Iterator end, boost::true_type)
{
   container.insert(begin, end);
}

template <typename Container, typename Iterator>
typename boost::disable_if_c<type_traits::has_key_type<Container>::value, void>::type
insert(Container& container, Iterator begin, Iterator end, boost::false_type)
{
   container.insert(container.end(), begin, end);
}

} // namespace detail

template <typename Container, typename Iterator>
void insert(Container& container, Iterator begin, Iterator end)
{
   detail::insert(container, begin, end,
                  type_traits::has_key_type<Container>());
}

/* Wrappers for the erase-remove idiom */
template <typename Container, typename ValueType>
void expel(Container& container, const ValueType& value)
{
   container.erase(std::remove(container.begin(), container.end(), value), container.end());
}

template <typename Container, typename Predicate>
void expel_if(Container& container, Predicate predicate)
{
   container.erase(std::remove_if(container.begin(), container.end(), predicate), container.end());
}

/* Value-based wrappers for erase-remove idiom */
template <typename Container, typename ValueType>
Container without(Container container, const ValueType& value)
{
   container.erase(std::remove(container.begin(), container.end(), value), container.end());
   return container;
}

template <typename Container, typename Predicate>
Container without_if(Container container, Predicate predicate)
{
   container.erase(std::remove_if(container.begin(), container.end(), predicate), container.end());
   return container;
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

template <typename AssociativeContainer,
          typename KeyType,
          typename MappedType>
bool get(const AssociativeContainer& container,
         const KeyType& key,
         MappedType** ppValue)
{
   if (!container.count(key))
      return false;
   
   *ppValue = &(const_cast<AssociativeContainer&>(container)[key]);
   return true;
}

namespace detail {

template <typename Container, typename Iterator>
typename boost::enable_if_c<type_traits::has_key_type<Container>::value, void>::type
append(Container* pContainer, Iterator begin, Iterator end, boost::true_type)
{
   pContainer->insert(begin, end);
}

template <typename Container, typename Iterator>
typename boost::disable_if_c<type_traits::has_key_type<Container>::value, void>::type
append(Container* pContainer, Iterator begin, Iterator end, boost::false_type)
{
   pContainer->insert(pContainer->end(), begin, end);
}

} // namespace detail

template <typename ContainerType, typename OtherType>
void append(ContainerType* pContainer, const OtherType& other)
{
   detail::append(pContainer,
                  other.begin(),
                  other.end(),
                  type_traits::has_key_type<ContainerType>());
}

inline std::vector<std::string> split(const std::string& string,
                                      const std::string& delim)
{
   std::vector<std::string> result;
   
   if (UNLIKELY(delim.size() == 0))
   {
      std::size_t n = string.size();
      result.reserve(n);
      for (std::string::size_type i = 0; i < n; ++i)
         result.push_back(std::string(string[i], 1));
      return result;
   }
   
   std::string::size_type start = 0;
   std::string::size_type end   = string.find(delim, start);
   
   // Add all of the initial split pieces
   while (end != std::string::npos)
   {
      result.push_back(string_utils::substring(string, start, end));
      
      start = end + delim.size();
      end   = string.find(delim, start);
   }
   
   // Add the final piece
   result.push_back(string_utils::substring(string, start));
   
   // And return!
   return result;
}

template <std::size_t N>
inline std::vector<std::string> split(const std::string& string,
                                      const char (&delim)[N])
{
   return split(string, std::string(delim, N - 1));
}

inline std::string join(const std::vector<std::string>& container, const std::string& delim)
{
   return boost::algorithm::join(container, delim);
}


template <typename Iterator, typename F>
inline std::string join(Iterator begin,
                        Iterator end,
                        const std::string& delim,
                        F&& f)
{
   if (begin >= end)
      return std::string();
   
   std::string result;
   result += f(*begin);
   for (Iterator it = begin + 1; it != end; ++it)
   {
      result += delim;
      result += f(*it);
   }
   return result;
   
}

template <typename Iterator>
inline std::string join(Iterator begin,
                        Iterator end,
                        const std::string& delim)
{
   auto callback = [](const std::string& string) { return string; };
   return join(begin, end, delim, std::move(callback));
}

} // namespace algorithm
} // namespace core
} // namespace rstudio


#endif // CORE_ALGORITHM_HPP
