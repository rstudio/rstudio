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
#include <boost/foreach.hpp>
#include <boost/format.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <core/Error.hpp>
#include <core/SafeConvert.hpp>
#include <core/FileSerializer.hpp>

#include <session/SessionModuleContext.hpp>

using namespace core ;

namespace session {  
namespace modules {
namespace build {

namespace {


bool isRSourceFile(const FilePath& filePath)
{
   return (filePath.extensionLowerCase() == ".q" ||
           filePath.extensionLowerCase() == ".s" ||
           filePath.extensionLowerCase() == ".r");
}

bool isMatchingFile(const std::vector<std::string>& lines,
                    std::size_t diagLine,
                    const std::string& lineContents,
                    const std::string& nextLineContents)
{
   // first verify the file has enough lines to match
   if (lines.size() < (diagLine+1))
      return false;

   return boost::algorithm::equals(lines[diagLine-1],lineContents) &&
          boost::algorithm::starts_with(lines[diagLine], nextLineContents);
}

FilePath scanForRSourceFile(const FilePath& basePath,
                            std::size_t diagLine,
                            const std::string& lineContents,
                            const std::string& nextLineContents)
{
   std::vector<FilePath> children;
   Error error = basePath.children(&children);
   if (error)
   {
      LOG_ERROR(error);
      return FilePath();
   }

   BOOST_FOREACH(const FilePath& child, children)
   {
      if (isRSourceFile(child))
      {
         std::vector<std::string> lines;
         Error error = core::readStringVectorFromFile(child, &lines, false);
         if (error)
         {
            LOG_ERROR(error);
            continue;
         }

         if (isMatchingFile(lines, diagLine, lineContents, nextLineContents))
            return child;
      }
   }

   return FilePath();
}

std::vector<CompileError> parseRErrors(const FilePath& basePath,
                                       const std::string& output)
{
   std::vector<CompileError> errors;

   boost::regex re("^Error in parse\\(outFile\\) : ([0-9]+?):([0-9]+?): (.+?)\\n"
                   "([0-9]+?): (.*?)\\n([0-9]+?): (.+?)$");
   boost::sregex_iterator iter(output.begin(), output.end(), re,
                               boost::regex_constants::match_not_dot_newline);
   boost::sregex_iterator end;
   for (; iter != end; iter++)
   {
      boost::smatch match = *iter;
      BOOST_ASSERT(match.size() == 8);

      // first part is straightforward
      std::string line = match[1];
      std::string column = match[2];
      std::string message = match[3];

      // we need to guess the file based on the contextual information
      // provided in the error message
      int diagLine = core::safe_convert::stringTo<int>(match[4], -1);
      if (diagLine != -1)
      {
         FilePath rSrcFile = scanForRSourceFile(basePath,
                                                diagLine,
                                                match[5],
                                                match[7]);
         if (!rSrcFile.empty())
         {
            // create error and add it
            CompileError err(CompileError::Error,
                             rSrcFile,
                             core::safe_convert::stringTo<int>(line, 1),
                             core::safe_convert::stringTo<int>(column, 1),
                             message,
                             false);
            errors.push_back(err);
         }
      }

   }

   return errors;

}


std::vector<CompileError> parseGccErrors(const FilePath& basePath,
                                         const std::string& output)
{
   std::vector<CompileError> errors;

   // parse standard gcc errors and warning lines but also pickup "from"
   // prefixed errors and substitute the from file for the error/warning file
   boost::regex re("(?:from (.+?):([0-9]+?).+?\\n)?"
                   "^(.+?):([0-9]+?):(?:([0-9]+?):)? (error|warning): (.+)$");
   boost::sregex_iterator iter(output.begin(), output.end(), re,
                               boost::regex_constants::match_not_dot_newline);
   boost::sregex_iterator end;
   for (; iter != end; iter++)
   {
      boost::smatch match = *iter;
      BOOST_ASSERT(match.size() == 8);

      std::string file, line, column, type, message;
      std::string match1 = match[1];
      if (!match1.empty() && FilePath::isRootPath(match[1]))
      {
         file = match[1];
         line = match[2];
         column = "1";
      }
      else
      {
         file = match[3];
         line = match[4];
         column = match[5];
         if (column.empty())
            column = "1";
      }
      type = match[6];
      message = match[7];

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
                       message,
                       true);
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
   obj["show_error_list"] = compileError.showErrorList;
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

CompileErrorParser rErrorParser(const FilePath& basePath)
{
   return boost::bind(parseRErrors, basePath, _1);
}


} // namespace build
} // namespace modules
} // namespace session
