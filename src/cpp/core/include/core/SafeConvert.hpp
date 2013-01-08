/*
 * SafeConvert.hpp
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

#ifndef CORE_SAFE_CONVERT_HPP
#define CORE_SAFE_CONVERT_HPP

#include <string>
#include <ios>
#include <iostream>
#include <locale>

#include <boost/lexical_cast.hpp>
#include <boost/numeric/conversion/cast.hpp>

#include <core/Log.hpp>
#include <core/Error.hpp>

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

template <typename T>
std::string numberToString(T input, bool localeIndependent = true)
{
   try
   {
      std::ostringstream stream;
      if (localeIndependent)
         stream.imbue(std::locale::classic()); // force locale-independence
      stream << input;
      return stream.str();
   }
   CATCH_UNEXPECTED_EXCEPTION

   // return empty string for unexpected error
   return std::string();
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

