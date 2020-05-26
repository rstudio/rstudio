/*
 * Set.hpp
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
#ifndef CORE_ALGORITHM_SET_HPP
#define CORE_ALGORITHM_SET_HPP

// Convenience algorithms -- not necessarily as fast as just
// using the STL + iterators, but often easier to use.

#include <algorithm>

namespace rstudio {
namespace core {
namespace algorithm {

#define RS_SET_OPERATION(__OPERATION__)                                        \
   template <typename C1, typename C2>                                         \
   C1 __OPERATION__(C1 c1, C2 c2)                                              \
   {                                                                           \
                                                                               \
      std::sort(c1.begin(), c1.end());                                         \
      std::sort(c2.begin(), c2.end());                                         \
                                                                               \
      C1 result;                                                               \
      std::__OPERATION__(c1.begin(), c1.end(), c2.begin(), c2.end(),           \
                         std::back_inserter(result));                          \
                                                                               \
      return result;                                                           \
   }

RS_SET_OPERATION(set_union)
RS_SET_OPERATION(set_difference)
RS_SET_OPERATION(set_intersection)
RS_SET_OPERATION(set_symmetric_difference)

#undef RS_SET_OPERATION

} // namespace algorithm
} // namespace core
} // namespace rstudio

#endif
