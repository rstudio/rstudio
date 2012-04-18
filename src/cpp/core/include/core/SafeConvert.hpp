/*
 * SafeConvert.hpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef CORE_SAFE_CONVERT_HPP
#define CORE_SAFE_CONVERT_HPP

#include <string>
#include <ios>
#include <iostream>

#include <boost/lexical_cast.hpp>
#include <boost/numeric/conversion/cast.hpp>


namespace core {
namespace safe_convert {
   
template <typename T>
T stringTo(const std::string& str, T defaultValue)
{
   try
   {
      return boost::lexical_cast<T>(str);
   }
   catch(boost::bad_lexical_cast&)
   {
      return defaultValue;
   }
}

template <typename T>
T stringTo(const std::string& str,
           T defaultValue,
           std::ios_base& (*f)(std::ios_base&))
{
   std::istringstream iss(str);
   T result;
   if ((iss >> f >> result).fail())
      return defaultValue;
   return result;
}

template <typename TInput, typename TOutput>
TOutput numberTo(TInput input, TOutput defaultValue)
{
   try
   {
      return boost::numeric_cast<TOutput>(input);
   }
   catch(...)
   {
      return defaultValue;
   }
}

} // namespace safe_convert
} // namespace core 


#endif // CORE_SAFE_CONVERT_HPP

