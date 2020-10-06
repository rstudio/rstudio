/*
 * RFunctionHook.cpp
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

#include <r/RFunctionHook.hpp>

#include <shared_core/Error.hpp>
#include <core/Log.hpp>

#include <r/RExec.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace r {
namespace function_hook {
   
namespace {
   
} // anonymous namespace
   
   
Error registerUnsupported(const std::string& name, const std::string& package)
{
   return r::exec::RFunction(".rs.registerUnsupported", name, package).call();
}

Error registerUnsupportedWithAlternative(const std::string& name,
                                         const std::string& package,
                                         const std::string& alternative)
{
   return r::exec::RFunction(".rs.registerUnsupported", 
                                 name, package, alternative).call();
}

} // namespace function_hook   
} // namespace r
} // namespace rstudio



