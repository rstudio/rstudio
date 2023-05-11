/*
 * Predicate.hpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
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

#ifndef CORE_PREDICATE_HPP
#define CORE_PREDICATE_HPP

#include <boost/function.hpp>
#include <boost/bind/bind.hpp>

using namespace boost::placeholders;

namespace rstudio {
namespace core {
namespace predicate {

template <typename T>
bool isWithinRange(const T& value, const T& min, const T& max)
{
   return (value >= min) && (value <= max);
}

template <typename T>
boost::function<bool(T)> range(const T& min, const T& max)
{
   return boost::bind(isWithinRange<T>, _1, min, max);
}   
   
   
} // namespace predicate
} // namespace core
} // namespace rstudio


#endif // CORE_PREDICATE_HPP

