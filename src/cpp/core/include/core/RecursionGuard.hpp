/*
 * RecursionGuard.hpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef CORE_RECURSION_GUARD_HPP
#define CORE_RECURSION_GUARD_HPP

#include <boost/utility.hpp>

#define DROP_RECURSIVE_CALLS \
   static int recursionCount; \
   if (recursionCount > 0) \
      return; \
   RecursionGuard rg(&recursionCount)

namespace rstudio {
namespace core {

// RecursionGuard is a simple class intended to prevent reentrancy for a 
// single function or other scoped block.  Given a reference to an appropriately
// scoped static, it can indicate how many instances of itself are on the 
// stack. 
class RecursionGuard : boost::noncopyable
{
public:
   explicit RecursionGuard(int* pCounter);
   ~RecursionGuard();
private:
   int* pCounter_;
};

}
}

#endif /* CORE_RECURSION_GUARD_HPP */
