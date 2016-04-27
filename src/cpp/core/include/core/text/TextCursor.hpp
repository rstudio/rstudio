/*
 * TextCursor.hpp
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

#include <cstddef>

#include <string>

#include <core/Macros.hpp>

#include <boost/noncopyable.hpp>

namespace rstudio {
namespace core {
namespace text {

template <typename CharType>
class TextCursorBase : boost::noncopyable
{
public:
   
   TextCursorBase(const CharType* begin, const CharType* end)
      : it_(begin), begin_(begin), end_(end)
   {
   }

   TextCursorBase(const CharType* text, std::size_t n)
      : it_(text), begin_(text), end_(text + n)
   {
   }
   
   const CharType* data() const     { return it_; }
   operator const CharType*() const { return it_; }
   CharType* operator->()           { return it_; }
   CharType& operator*()            { return *it_; }
   
   CharType& operator[](std::size_t index)
   {
      return *(it_ + index);
   }
   
   void advance(std::size_t characters = 1)
   {
      it_ += characters;
      if (UNLIKELY(it_ > end_))
         it_ = end_;
   }
   
   void retreat(std::size_t characters = 1)
   {
      it_ -= characters;
      if (UNLIKELY(it_ < begin_))
         it_ = begin_;
   }
   
   CharType peekFwd(std::size_t offset = 1)
   {
      if (UNLIKELY(it_ + offset >= end_))
         return '\0';
      return *(it_ + offset);
   }
   
   CharType peekBwd(std::size_t offset = 1)
   {
      if (UNLIKELY(it_ < begin_ + offset))
         return '\0';
      return *(it_ - offset);
   }

   bool findFwd(CharType ch)
   {
      for (; it_ != end_; ++it_)
         if (*it_ == ch)
            return true;
      return false;
   }
   
   bool findBwd(CharType ch)
   {
      for (; it_ != begin_; --it_)
         if (*it_ == ch)
            return true;
      return *it_ == ch;
   }
   
   std::size_t offset() { return it_ - begin_; }
   
private:
   
   const CharType* it_;
   const CharType* begin_;
   const CharType* end_;
};

class TextCursor : public TextCursorBase<char>
{
public:
   explicit TextCursor(const std::string& string)
      : TextCursorBase(string.c_str(), string.size())
   {
   }
};

} // namespace text
} // namespace core
} // namespace rstudio
