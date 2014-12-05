/*
 * RFunctionHook.hpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#ifndef R_FUNCTION_HOOK_HPP
#define R_FUNCTION_HOOK_HPP

#include <string>

namespace rscore {
   class Error;
}

namespace r {
namespace function_hook {

rscore::Error registerUnsupported(const std::string& name,
                                const std::string& package);


rscore::Error registerUnsupportedWithAlternative(const std::string& name,
                                               const std::string& package,
                                               const std::string& alternative);

} // namespace function_hook   
} // namespace r


#endif // R_FUNCTION_HOOK_HPP 

