/*
 * Stack.hpp
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

#ifndef CORE_COLLECTION_STACK_HPP
#define CORE_COLLECTION_STACK_HPP

#include <vector>

namespace rstudio {
namespace core {
namespace collection {

template <typename T>
class Stack
{
public:
   
   // default ctors: copyable members
   
   explicit Stack(std::size_t initialCapacity = 8)
   {
      data_.reserve(initialCapacity);
   }

   void push(const T& data)
   {
      data_.push_back(data);
   }
   
   void pop()
   {
      data_.pop_back();
   }
   
   const T& peek() const
   {
      return data_[data_.size() - 1];
   }
   
   bool empty() const
   {
      return data_.empty();
   }
   
   const T& operator[](std::size_t i) const
   {
      return data_[i];
   }
   
   std::size_t size() const
   {
      return data_.size();
   }
   
   typedef typename std::vector<T>::reverse_iterator iterator;
   typedef typename std::vector<T>::const_reverse_iterator const_iterator;
   
   const_iterator begin() const { return data_.rbegin(); }
   const_iterator end() const { return data_.rend(); }
   
private:
   std::vector<T> data_;
};

} // end namespace collection
} // end namespace core
} // end namespace rstudio

#endif
