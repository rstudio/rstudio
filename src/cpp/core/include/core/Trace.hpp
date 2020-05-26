/*
 * Trace.hpp
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

#ifndef CORE_TRACE_HPP
#define CORE_TRACE_HPP

#include <iosfwd>
#include <string>

#include <boost/current_function.hpp>

namespace rstudio {
namespace core { 
namespace trace {

void add(void* key, const std::string& functionName);

} // namespace trace
} // namespace core 
} // namespace rstudio

#define TRACE_CURRENT_METHOD \
   core::trace::add(this, BOOST_CURRENT_FUNCTION);

#endif // CORE_PERFORMANCE_TIMER_HPP

