/*
 * Assert.cpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <core/Error.hpp>
#include <core/Log.hpp>

using namespace core ;

namespace boost
{
   
void assertion_failed(char const * expr, 
                      char const * function, 
                      char const * file, 
                      long line)
{
   std::string message = std::string("ASSERTION FAILED: ") + expr;
   ErrorLocation errorLocation(function, file, line);
   core::log::logErrorMessage(message, errorLocation);
}
   
}


