/*
 * Assert.cpp
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


#include <core/Error.hpp>
#include <core/Log.hpp>

#ifdef _WIN32
#include <windows.h>
#else
#include <signal.h>
#endif

using namespace rstudio;
using namespace core;

namespace RSTUDIO_BOOST_NAMESPACE {

void assertion_failed(char const * expr,
                      char const * function,
                      char const * file,
                      long line)
{
   // derive location
   ErrorLocation location(function, file, line);

   // always log the failure
   std::string msg = "ASSERTION FAILED: " + std::string(expr);
   log::logWarningMessage(msg, location);

#ifndef NDEBUG
#ifdef _WIN32
   DebugBreak();
#else
   ::raise(SIGTRAP);
#endif
#endif

}

void assertion_failed_msg(char const * expr,
                          char const * msg,
                          char const * function,
                          char const * file,
                          long line)
{
   // derive location
   ErrorLocation location(function, file, line);

   // always log the failure
   std::string message = "ASSERTION FAILED: " + std::string(expr) +
                         " - " + std::string(msg);
   log::logWarningMessage(message, location);

#ifndef NDEBUG
#ifdef _WIN32
   DebugBreak();
#else
   ::raise(SIGTRAP);
#endif
#endif
}

} // namespace boost




