/*
 * Scope.hpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef CORE_SCOPE_HPP
#define CORE_SCOPE_HPP

#include <boost/function.hpp>
#include <boost/noncopyable.hpp>

namespace core {
namespace scope {

template <class T>
class SetOnExit : boost::noncopyable
{
public:
   SetOnExit(T* pLocation, const T& value)
   {
      pLocation_ = pLocation;
      value_ = value;
   }

   virtual ~SetOnExit()
   {
      try
      {
         *pLocation_ = value_;
      }
      catch(...)
      {
      }
   }

 private:
   T* pLocation_;
   T value_;
};

class CallOnExit : boost::noncopyable
{
public:
   CallOnExit(const boost::function<void()>& func)
   {
      func_ = func;
   }

   ~CallOnExit()
   {
      func_();
   }

private:
   boost::function<void()> func_;
};

} // namespace scope
} // namespace core 


#endif // CORE_SCOPE_HPP

