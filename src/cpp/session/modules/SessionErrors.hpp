/*
 * SessionErrors.hpp
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

#ifndef SESSIONERRORS_HPP
#define SESSIONERRORS_HPP

#include <core/json/Json.hpp>

namespace core {
   class Error;
}

namespace session {
namespace modules {
namespace errors {

// Error handler types understood by the client
const int ERRORS_MESSAGE = 0;
const int ERRORS_TRACEBACK = 1;
const int ERRORS_BREAK = 2;
const int ERRORS_CUSTOM = 3;

core::Error initialize();
core::json::Value errorStateAsJson();

} // namespace errors
} // namepace modules
} // namesapce session

#endif // SESSIONERRORS_HPP
