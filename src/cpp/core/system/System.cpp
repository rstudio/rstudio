/*
 * System.cpp
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

#include <core/system/System.hpp>

#include <core/Log.hpp>

namespace core {
namespace system {
     
int exitFailure(const Error& error, const ErrorLocation& loggedFromLocation)
{
   core::log::logError(error, loggedFromLocation);
   return EXIT_FAILURE;
}

int exitFailure(const std::string& errMsg,
                const ErrorLocation& loggedFromLocation)
{
   core::log::logErrorMessage(errMsg, loggedFromLocation);
   return EXIT_FAILURE;
}
   
int exitSuccess()
{
   return EXIT_SUCCESS;
}

} // namespace system
} // namespace core

