/*
 * SafeConvert.hpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#ifndef SHARED_CORE_SAFE_CONVERT_HPP
#define SHARED_CORE_SAFE_CONVERT_HPP

#include <string>
#include <ios>
#include <iostream>
#include <locale>

#include <boost/lexical_cast.hpp>
#include <boost/numeric/conversion/cast.hpp>
#include <boost/optional.hpp>

#include <shared_core/Logger.hpp>
#include <shared_core/Error.hpp>

namespace rstudio {
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
boost::optional<T> stringTo(const std::string& str)
{
   boost::optional<T> result;

   try
   {
      result = boost::lexical_cast<T>(str);
   }
   catch(boost::bad_lexical_cast&)
   {
   }

   return result;
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

inline std::string numberToString(double input, bool localeIndependent = true)
{
   try
   {
      std::ostringstream stream;
      if (localeIndependent)
         stream.imbue(std::locale::classic()); // force locale-independence
      stream << std::fixed;
      stream << input;
      return stream.str();
   }
   CATCH_UNEXPECTED_EXCEPTION

   // return empty string for unexpected error
   return std::string();
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

template <typename TInput, typename TOutput>
boost::optional<TOutput> numberTo(TInput input)
{
   boost::optional<TOutput> result;

   try
   {
      result = boost::numeric_cast<TOutput>(input);
   }
   catch(...)
   {
   }

   return result;
}

} // namespace safe_convert
} // namespace core 
} // namespace rstudio

#endif // CORE_SAFE_CONVERT_HPP
