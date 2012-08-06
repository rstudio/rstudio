/*
 * SessionBuildErrors.hpp
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

#ifndef SESSION_BUILD_ERRORS_HPP
#define SESSION_BUILD_ERRORS_HPP

#include <string>
#include <vector>

#include <boost/function.hpp>

#include <core/FilePath.hpp>
#include <core/json/Json.hpp>

namespace session {
namespace modules {
namespace build {

struct CompileError
{
   // NOTE: error types are shared accross all client code that uses
   // the CompileError type. therefore if we want to add more types
   // we need to do so beyond the 'Box' value
   enum Type {
      Error = 0, Warning = 1  /*, Box = 2 */
   };

   CompileError(Type type,
                const core::FilePath& path,
                int line,
                const std::string& message)
      : type(type), path(path), line(line), message(message)
   {
   }

   Type type;
   core::FilePath path;
   int line;
   std::string message;
};

core::json::Array compileErrorsAsJson(const std::vector<CompileError>& errors);
   
typedef boost::function<std::vector<CompileError>(const std::string&)>
                                                         CompileErrorParser;

CompileErrorParser gccErrorParser();


} // namespace build
} // namespace modules
} // namespace session

#endif // SESSION_BUILD_ERRORS_HPP

