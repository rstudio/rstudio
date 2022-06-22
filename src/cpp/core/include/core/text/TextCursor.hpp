/*
 * TextCursor.hpp
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

#ifndef RSTUDIO_CORE_TEXT_TEXTCURSOR_HPP
#define RSTUDIO_CORE_TEXT_TEXTCURSOR_HPP

#include <cstddef>

#include <string>

namespace rstudio {
namespace core {
namespace text {

class TextCursor
{
public:
   
   // NOTE: Stores a reference (pointer) to 'text', so it's
   // important that the associated text lives as long as
   // the cursor does.
   explicit TextCursor(const std::string& text)
      : offset_(0),
        text_(text.data()),
        size_(text.size())
   {
   }
   
   bool advance(std::size_t count = 1);
   
   bool consume(char ch);
   bool consume(const std::string& text);
   
   bool consumeUntil(char ch);
   bool consumeUntil(const std::string& text);
   
   char peek(std::size_t count = 0) const;
   char operator*() const { return text_[offset_]; }
   
   std::size_t offset() const { return offset_; }
   
private:
   std::size_t offset_;
   const char* text_;
   std::size_t size_;
};

} // end namespace text
} // end namespace core
} // end namespace rstudio

#endif /* RSTUDIO_CORE_TEXT_TEXTCURSOR_HPP */
