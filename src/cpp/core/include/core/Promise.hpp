/*
 * Promise.hpp
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


#ifndef PROMISE_HPP
#define PROMISE_HPP

#include <boost/function.hpp>

/*
 This class is NOT threadsafe.

 Convenient way to delay and memoize an expensive computation.
 */
template <typename T>
class Promise
{
public:
   Promise(boost::function<T ()> func) :
      resolved_(false),
      func_(func)
   {
   }

   T& value()
   {
      if (!resolved_)
      {
         value_ = func_();
         resolved_ = true;
      }
      return value_;
   }

   operator T&()
   {
      return value();
   }

   bool isResolved()
   {
      return resolved_;
   }

private:
   bool resolved_;
   T value_;
   boost::function<T ()> func_;
};

#endif // PROMISE_HPP
