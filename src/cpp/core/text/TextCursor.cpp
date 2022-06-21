/*
 * TextCursor.cpp
 *
 * Copyright (C) 2022 by RStudio, PBC
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

#include <core/text/TextCursor.hpp>

#include <core/Macros.hpp>

namespace rstudio {
namespace core {
namespace text {

bool TextCursor::advance(std::size_t count)
{
   std::size_t offset = offset_ + count;
   if (UNLIKELY(offset >= size_))
      return false;
   
   offset_ = offset;
   return true;
}

bool TextCursor::consume(char ch)
{
   if (text_[offset_] != ch)
      return false;
   
   ++offset_;
   return true;
}

bool TextCursor::consume(const std::string& text)
{
   for (std::size_t i = 0, n = text.size(); i < n; i++)
      if (text_[offset_ + i] != text[i])
         return false;
   
   offset_ += text.size();
   return true;
}

bool TextCursor::consumeUntil(char ch)
{
   auto it = std::find(text_ + offset_, text_ + size_, ch);
   if (it == text_ + size_)
      return false;
   
   offset_ = (it - text_);
   return true;
}

bool TextCursor::consumeUntil(const std::string& text)
{
   auto it = std::search(text_ + offset_, text_ + size_, text.begin(), text.end());
   if (it == text_ + size_)
      return false;
   
   offset_ = (it - text_);
   return true;
}

char TextCursor::peek(std::size_t count) const
{
   auto offset = offset_ + count;
   if (UNLIKELY(offset >= size_))
      return 0;
   
   return text_[offset];
}

} // end namespace text
} // end namespace core
} // end namespace rstudio
