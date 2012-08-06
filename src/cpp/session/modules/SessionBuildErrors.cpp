/*
 * SessionBuildErrors.cpp
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

#include "SessionBuildErrors.hpp"

#include <algorithm>

#include <core/Error.hpp>

#include <session/SessionModuleContext.hpp>

using namespace core ;

namespace session {  
namespace modules {
namespace build {

namespace {

std::vector<CompileError> parseGccErrors(const std::string& output)
{
   std::vector<CompileError> errors;


   return errors;
}

// NOTE: sync changes with SessionCompilePdf.cpp logEntryJson
json::Value compileErrorJson(const CompileError& compileError)
{
   json::Object obj;
   obj["type"] = static_cast<int>(compileError.type);
   obj["path"] = module_context::createAliasedPath(compileError.path);
   obj["line"] = compileError.line;
   obj["message"] = compileError.message;
   obj["log_path"] = "";
   obj["log_line"] = -1;
   return obj;
}


} // anonymous namespace

json::Array compileErrorsAsJson(const std::vector<CompileError>& errors)
{
   json::Array errorsJson;
   std::transform(errors.begin(),
                  errors.end(),
                  std::back_inserter(errorsJson),
                  compileErrorJson);
   return errorsJson;
}



CompileErrorParser gccErrorParser()
{
   return parseGccErrors;
}




} // namespace build
} // namespace modules
} // namespace session
