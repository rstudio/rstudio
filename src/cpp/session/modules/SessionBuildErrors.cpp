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

#include <boost/bind.hpp>
#include <boost/regex.hpp>

#include <core/Error.hpp>
#include <core/SafeConvert.hpp>

#include <session/SessionModuleContext.hpp>

using namespace core ;

namespace session {  
namespace modules {
namespace build {

namespace {

std::vector<CompileError> parseGccErrors(const FilePath& basePath,
                                         const std::string& output)
{
   std::vector<CompileError> errors;

   boost::regex re("^(.+?):([0-9]+?):(?:([0-9]+?):)? (error|warning): (.+)$");
   boost::sregex_iterator iter(output.begin(), output.end(), re,
                               boost::regex_constants::match_not_dot_newline);
   boost::sregex_iterator end;
   for (; iter != end; iter++)
   {
      boost::smatch match = *iter;
      std::string file = match[1];
      std::string line = match[2];
      std::string column, type, message;
      if (match.size() == 4)
      {
         column = "1";
         type = match[3];
         message = match[4];
      }
      else
      {
         column = match[3];
         type = match[4];
         message = match[5];
      }

      // resolve file path
      FilePath filePath;
      if (FilePath::isRootPath(file))
         filePath = FilePath(file);
      else
         filePath = basePath.complete(file);
      FilePath realPath;
      Error error = core::system::realPath(filePath, &realPath);
      if (error)
         LOG_ERROR(error);
      else
         filePath = realPath;

      // resolve type
      CompileError::Type errType = (type == "warning") ? CompileError::Warning :
                                                         CompileError::Error;

      // create error and add it
      CompileError err(errType,
                       filePath,
                       core::safe_convert::stringTo<int>(line, 1),
                       core::safe_convert::stringTo<int>(column, 1),
                       message);
      errors.push_back(err);
   }

   return errors;
}

// NOTE: sync changes with SessionCompilePdf.cpp logEntryJson
json::Value compileErrorJson(const CompileError& compileError)
{
   json::Object obj;
   obj["type"] = static_cast<int>(compileError.type);
   obj["path"] = module_context::createAliasedPath(compileError.path);
   obj["line"] = compileError.line;
   obj["column"] = compileError.column;
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



CompileErrorParser gccErrorParser(const FilePath& basePath)
{
   return boost::bind(parseGccErrors, basePath, _1);
}




} // namespace build
} // namespace modules
} // namespace session
