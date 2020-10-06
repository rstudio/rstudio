/*
 * Position.hpp
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
#ifndef CORE_COLLECTION_POSITION_HPP
#define CORE_COLLECTION_POSITION_HPP

#include <iostream>

namespace rstudio {
namespace core {
namespace collection {

struct Position
{
   Position(std::size_t row = 0, std::size_t column = 0)
      : row(row), column(column) {}
   
   friend bool operator <(const Position& lhs,
                          const Position& rhs)
   {
      if (lhs.row == rhs.row)
         return lhs.column < rhs.column;
      else
         return lhs.row < rhs.row;
   }
   
   friend bool operator <=(const Position& lhs,
                           const Position& rhs)
   {
      if (lhs.row == rhs.row)
         return lhs.column <= rhs.column;
      return lhs.row < rhs.row;
   }
   
   friend bool operator ==(const Position& lhs,
                           const Position& rhs)
   {
      return lhs.row == rhs.row && lhs.column == rhs.column;
   }
   
   friend bool operator >(const Position& lhs,
                          const Position& rhs)
   {
      return rhs < lhs;
   }
   
   friend bool operator >=(const Position& lhs,
                           const Position& rhs)
   {
      return rhs <= lhs;
   }
   
   std::string toString() const
   {
      std::stringstream ss;
      ss << "(" << row << ", " << column << ")";
      return ss.str();
   }
   
   friend std::ostream& operator <<(std::ostream& os, const Position& position)
   {
      return os << position.toString();
   }
   
   std::size_t row;
   std::size_t column;
};

class Range
{
public:
   
   Range(const Position& begin, const Position& end)
      : begin_(begin), end_(end)
   {}
   
   const Position& begin() const
   {
      return begin_;
   }
   
   const Position& end() const
   {
      return end_;
   }
   
   friend bool operator <(const Range& lhs,
                          const Range& rhs)
   {
      return lhs.begin() < rhs.begin() &&
             lhs.end() < rhs.end();
   }
   
   bool isWithin(const Range& range) const
   {
      return begin() >= range.begin() && end() <= range.end();
   }
   
   bool contains(const Position& position) const
   {
      return begin() <= position && end() >= position;
   }
   
   bool contains(const Range& range) const
   {
      return contains(range.begin()) && contains(range.end());
   }
   
   bool overlaps(const Range& range) const
   {
      return contains(range.begin()) || contains(range.end());
   }
   
private:
   
   Position begin_;
   Position end_;
};



} // namespace collection
} // namespace core
} // namespace rstudio

#endif // CORE_COLLECTION_POSITION_HPP
