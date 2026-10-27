/*
 * Utility.hpp
 *
 * Copyright (C) 2025 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant to the terms of a commercial license agreement
 * with Posit, then this program is licensed to you under the following terms:
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

#ifndef RSTUDIO_UTILITY_HPP
#define RSTUDIO_UTILITY_HPP

#include <algorithm>
#include <cctype>
#include <cwctype>
#include <string>

#include <boost/type_traits.hpp>

namespace rstudio {

// Like boost::noncopyable, but allows a type to remain movable.
struct MoveOnly
{
    MoveOnly() = default;
    MoveOnly(MoveOnly&&) = default;
    MoveOnly& operator=(MoveOnly&&) = default;
};

namespace core {

// wrappers for functions in <cctype>, as those functions normally
// require one to cast arguments from char to unsigned char, otherwise
// one risks bumping into undefined behavior (which MSVC is strict about)
// see e.g. http://en.cppreference.com/w/cpp/string/byte/toupper for
// motivation
//
// we also use some C++ template magic to avoid misuse; that is, we don't want
// to allow callers to accidentally pass wchar_t or other non-char types into
// these functions. then wrap it into a macro to generate code with less pain

inline namespace cctype {

namespace internal {

template <typename T>
using IsCharacter = boost::disjunction<
    boost::is_same<T, char>,
    boost::is_same<T, signed char>,
    boost::is_same<T, unsigned char>
>;

}

#define RS_GENERATE_CCTYPE_ALIAS(__NAME__, __WNAME__)          \
                                                               \
template <typename T>                                          \
inline bool __NAME__(T ch)                                     \
{                                                              \
   static_assert(internal::IsCharacter<T>::value, "");         \
   return std::__NAME__(static_cast<unsigned char>(ch)) != 0;  \
}                                                              \
                                                               \
template <typename T>                                          \
inline bool __WNAME__(T ch)                                    \
{                                                              \
   static_assert(std::is_same<T, wchar_t>::value, "");         \
   return std::__WNAME__(static_cast<wchar_t>(ch)) != 0;       \
}                                                              \

RS_GENERATE_CCTYPE_ALIAS(isalpha,  iswalpha)
RS_GENERATE_CCTYPE_ALIAS(isalnum,  iswalnum)
RS_GENERATE_CCTYPE_ALIAS(islower,  iswlower)
RS_GENERATE_CCTYPE_ALIAS(isupper,  iswupper)
RS_GENERATE_CCTYPE_ALIAS(isdigit,  iswdigit)
RS_GENERATE_CCTYPE_ALIAS(isxdigit, iswxdigit)
RS_GENERATE_CCTYPE_ALIAS(iscntrl,  iswcntrl)
RS_GENERATE_CCTYPE_ALIAS(isgraph,  iswgraph)
RS_GENERATE_CCTYPE_ALIAS(isspace,  iswspace)
RS_GENERATE_CCTYPE_ALIAS(isblank,  iswblank)
RS_GENERATE_CCTYPE_ALIAS(isprint,  iswprint)
RS_GENERATE_CCTYPE_ALIAS(ispunct,  iswpunct)

#undef RS_GENERATE_CCTYPE_ALIAS

} // end inline namespace cctype

} // end namespace core

inline bool isNumeric(const std::string& str)
{
  return !str.empty() && std::all_of(str.begin(), str.end(), core::cctype::isdigit<char>);
}

} // end namespace rstudio

// force our safe wrappers to be used
using namespace rstudio::core::cctype;

#endif /* RSTUDIO_UTILITY_HPP */
