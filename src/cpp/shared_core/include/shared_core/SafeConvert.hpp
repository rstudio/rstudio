/*
 * SafeConvert.hpp
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant to the terms of a commercial license agreement
 * with RStudio, then this program is licensed to you under the following terms:
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

/**
 * @brief Converts a string value to the specified type. Returns the specified default value if a conversion error
 *        occurs.
 *
 * @tparam T                The type to convert the string to.
 * @param in_strValue       The value to convert to type T.
 * @param in_defaultValue   The default value to use if conversion fails.
 *
 * @return The converted value, on successful conversion; the default value otherwise.
 */
template <typename T>
T stringTo(const std::string& in_strValue, T in_defaultValue)
{
   try
   {
      return boost::lexical_cast<T>(in_strValue);
   }
   catch(boost::bad_lexical_cast&)
   {
      return in_defaultValue;
   }
}

/**
 * @brief Converts a string value to the specified type.
 *
 * @tparam T                The type to convert the string to.
 * @param in_strValue       The value to convert to type T.
 *
 * @return The converted value, on successful conversion; an empty optional value otherwise.
 */
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

/**
 * @brief Converts a string value to the specified type using the provided stream conversion function. Returns the
 *        specified default value if a conversion error occurs.
 *
 * @tparam T                The type to convert the string to.
 * @param in_strValue       The value to convert to type T.
 * @param in_defaultValue   The default value to use if conversion fails.
 * @param in_f              The conversion function.
 *
 * @return The converted value, on successful conversion; the default value otherwise.
 */
template <typename T>
T stringTo(const std::string& in_strValue,
           T in_defaultValue,
           std::ios_base& (*in_f)(std::ios_base&))
{
   std::istringstream iss(in_strValue);
   T result;
   if ((iss >> in_f >> result).fail())
      return in_defaultValue;
   return result;
}

/**
 * @brief Coverts a number to string value.
 *
 * @param in_input                  The number to convert.
 * @param in_localeIndependent      Whether to perform the conversion independent of locale. Default: true.
 *
 * @return The converted string on successful conversion; empty string otherwise.
 */
inline std::string numberToString(double in_input, bool in_localeIndependent = true)
{
   try
   {
      std::ostringstream stream;
      if (in_localeIndependent)
         stream.imbue(std::locale::classic()); // force locale-independence
      stream << std::fixed;
      stream << in_input;
      return stream.str();
   }
   CATCH_UNEXPECTED_EXCEPTION

   // return empty string for unexpected error
   return std::string();
}

/**
 * @brief Coverts a number to string value.
 *
 * @tparam T                        The type of the number.
 * @param in_input                  The number to convert.
 * @param in_localeIndependent      Whether to perform the conversion independent of locale. Default: true.
 *
 * @return The converted string on successful conversion; empty string otherwise.
 */
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

/**
 * @brief Coverts a number to the specified type.
 *
 * @tparam TInput                   The type of the number.
 * @tparam TOutput                  The type to which to convert the number.
 * @param in_input                  The number to convert.
 * @param in_defaultValue           The default value to return on failed conversion.
 *
 * @return The converted value on successful conversion; the default value otherwise.
 */
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

/**
 * @brief Coverts a number to the specified type.
 *
 * @tparam TInput                   The type of the number.
 * @tparam TOutput                  The type to which to convert the number.
 * @param in_input                  The number to convert.
 *
 * @return The converted value on successful conversion; an empty optional value otherwise.
 */
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

#endif // SHARED_CORE_SAFE_CONVERT_HPP
