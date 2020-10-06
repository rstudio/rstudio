/*
 * SessionBuildErrors.cpp
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

#include "SessionBuildErrors.hpp"

#include <algorithm>

#include <boost/bind.hpp>
#include <boost/regex.hpp>
#include <boost/format.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/SafeConvert.hpp>
#include <core/FileSerializer.hpp>

#include <r/RExec.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/projects/SessionProjects.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {  
namespace modules {
namespace build {

namespace {


bool isRSourceFile(const FilePath& filePath)
{
   return (filePath.getExtensionLowerCase() == ".q" ||
           filePath.getExtensionLowerCase() == ".s" ||
           filePath.getExtensionLowerCase() == ".r");
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
   Error error = basePath.getChildren(children);
   if (error)
   {
      LOG_ERROR(error);
      return FilePath();
   }

   for (const FilePath& child : children)
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

std::vector<module_context::SourceMarker> parseRErrors(
                                       const FilePath& basePath,
                                       const std::string& output)
{
   using namespace module_context;
   std::vector<SourceMarker> errors;

   boost::regex re("^Error in parse\\(outFile\\) : ([0-9]+?):([0-9]+?): (.+?)\\n"
                   "([0-9]+?): (.*?)\\n([0-9]+?): (.+?)$");
   try
   {
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
            if (!rSrcFile.isEmpty())
            {
               // create error and add it
               SourceMarker err(SourceMarker::Error,
                                rSrcFile,
                                core::safe_convert::stringTo<int>(line, 1),
                                core::safe_convert::stringTo<int>(column, 1),
                                core::html_utils::HTML(message),
                                false);
               errors.push_back(err);
            }
         }

      }
   }
   CATCH_UNEXPECTED_EXCEPTION;

   return errors;

}


std::vector<module_context::SourceMarker> parseGccErrors(
                                           const FilePath& basePath,
                                           const std::string& output)
{
   // check to see if we are in a package
   std::string pkgInclude;
   using namespace projects;
   if (projectContext().hasProject() &&
       (projectContext().config().buildType == r_util::kBuildTypePackage))
   {
      pkgInclude = "/" + projectContext().packageInfo().name() + "/include/";
   }

   using namespace module_context;
   std::vector<SourceMarker> errors;

   // parse standard gcc errors and warning lines but also pickup "from"
   // prefixed errors and substitute the from file for the error/warning file
   boost::regex re("(?:from (.+?):([0-9]+?).+?\\n)?"
                   "^(.+?):([0-9]+?):(?:([0-9]+?):)? (error|warning): (.+)$");
   try
   {
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
            filePath = basePath.completeChildPath(file);

         // skip if the file doesn't exist
         if (!filePath.exists())
            continue;

         FilePath realPath;
         Error error = core::system::realPath(filePath, &realPath);
         if (error)
            LOG_ERROR(error);
         else
            filePath = realPath;

         // if we are in a package and the file where the error occurred
         // has /<package-name>/include/ in it then it might be a template
         // instantiation error. in that case re-map it to the appropriate
         // source file within the package
         if (!pkgInclude.empty())
         {
            std::string path = filePath.getAbsolutePath();
            size_t pos = path.find(pkgInclude);
            if (pos != std::string::npos)
            {
               // advance to end and calculate relative path
               pos += pkgInclude.length();
               std::string relativePath = path.substr(pos);

               // does this file exist? if so substitute it
               FilePath includePath = projectContext().buildTargetPath()
                                                      .completeChildPath("inst/include/" + relativePath);
               if (includePath.exists())
                  filePath = includePath;
            }
         }

         // don't show warnings from Makeconf
         if (filePath.getFilename() == "Makeconf")
            continue;

         // create marker and add it
         SourceMarker err(module_context::sourceMarkerTypeFromString(type),
                          filePath,
                          core::safe_convert::stringTo<int>(line, 1),
                          core::safe_convert::stringTo<int>(column, 1),
                          core::html_utils::HTML(message),
                          true);
         errors.push_back(err);
      }
   }
   CATCH_UNEXPECTED_EXCEPTION;

   return errors;
}

std::vector<module_context::SourceMarker> parseTestThatErrors(
                                           const FilePath& basePath,
                                           const std::string& output)
{
   using namespace module_context;
   std::vector<SourceMarker> errors;

   try
   {
      FilePath basePathResolved = module_context::resolveAliasedPath(basePath.getAbsolutePath());

      boost::regex re("\\[[0-9]+m([^:\\n]+):([0-9]+): ?([^:\\n]+): ([^\\n]*)\\[[0-9]+m");

      boost::sregex_iterator iter(output.begin(), output.end(), re);
      boost::sregex_iterator end;
      for (; iter != end; iter++)
      {
         boost::smatch match = *iter;
         BOOST_ASSERT(match.size() == 5);

         std::string file, line, type, message, marker;
         
         file = match[1];
         line = match[2];
         type = match[3];

         if (type.find("error") != std::string::npos) {
            marker = "error";
         } else if (type.find("failure") != std::string::npos) {
            marker = "error";
         } else if (type.find("warning") != std::string::npos) {
            marker = "warning";
         } else {
            marker = "info";
         }

         message = match[4];
         FilePath testFilePath = basePathResolved.completePath(file);

         std::string column = "0";
         SourceMarker err(module_context::sourceMarkerTypeFromString(marker),
                          testFilePath,
                          core::safe_convert::stringTo<int>(line, 1),
                          core::safe_convert::stringTo<int>(column, 1),
                          core::html_utils::HTML(message),
                          true);
         errors.push_back(err);
      }
   }
   CATCH_UNEXPECTED_EXCEPTION;

   return errors;
}

std::vector<module_context::SourceMarker> parseShinyTestErrors(
                                           const FilePath& basePath,
                                           const FilePath& rdsPath,
                                           const std::string& output)
{
   using namespace module_context;
   std::vector<SourceMarker> errors;

   try
   {
      FilePath basePathResolved = module_context::resolveAliasedPath(basePath.getAbsolutePath());

      std::vector<std::string> failed;
      r::exec::RFunction rFunc(".rs.readShinytestResultRds", rdsPath.getAbsolutePath());
      Error error = rFunc.call(&failed);
      if (error) 
         LOG_ERROR(error);

      for (size_t idxFailed = 0; idxFailed < failed.size(); idxFailed++)
      {
         std::string file, line, type, message;
         
         file = failed.at(idxFailed);
         line = "0";
         std::string column = "0";
         type = "failure";
         message = std::string("Differences detected in " + file + ".");

         // ask the shinytest package where the tests live (this location varies between versions of
         // the shinytest package
         std::string testsDir;
         r::exec::RFunction findTests(".rs.findShinyTestsDir", 
               basePathResolved.getAbsolutePath());
         error = findTests.call(&testsDir);
         if (error)
            LOG_ERROR(error);

         SourceMarker err(module_context::sourceMarkerTypeFromString(type),
                          FilePath(testsDir).completePath(file + ".R"),
                          core::safe_convert::stringTo<int>(line, 1),
                          core::safe_convert::stringTo<int>(column, 1),
                          core::html_utils::HTML(message),
                          true);
         errors.push_back(err);
      }
   }
   CATCH_UNEXPECTED_EXCEPTION;

   return errors;
}

} // anonymous namespace

CompileErrorParser gccErrorParser(const FilePath& basePath)
{
   return boost::bind(parseGccErrors, basePath, _1);
}

CompileErrorParser rErrorParser(const FilePath& basePath)
{
   return boost::bind(parseRErrors, basePath, _1);
}

CompileErrorParser testthatErrorParser(const FilePath& basePath)
{
   return boost::bind(parseTestThatErrors, basePath, _1);
}

CompileErrorParser shinytestErrorParser(const FilePath& basePath, const FilePath& rdsPath)
{
   return boost::bind(parseShinyTestErrors, basePath, rdsPath, _1);
}

} // namespace build
} // namespace modules
} // namespace session
} // namespace rstudio
