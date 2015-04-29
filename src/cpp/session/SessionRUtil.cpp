/*
 * SessionRUtil.cpp
 *
 * Copyright (C) 2009-2015 by RStudio, Inc.
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

#include <session/SessionRUtil.hpp>

#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/Exec.hpp>
#include <core/Macros.hpp>

#include <core/FileSerializer.hpp>

#include <r/RExec.hpp>

#include <session/SessionModuleContext.hpp>

#include <boost/regex.hpp>

#include "modules/shiny/SessionShiny.hpp"

namespace rstudio {

using namespace core;

namespace session {
namespace r_utils {

namespace {

boost::regex& reYaml()
{
   static boost::regex instance("^[\\s\\n]*---\\s*(.*?)---\\s*(?:$|\\n)");
   return instance;
}

} // anonymous namespace

bool hasYamlHeader(const FilePath& filePath)
{
   std::string contents;
   Error error = readStringFromFile(filePath, &contents);
   if (error)
      LOG_ERROR(error);
   
   return hasYamlHeader(contents);
}

bool hasYamlHeader(const std::string& content)
{
   return boost::regex_search(content.begin(), content.end(), reYaml());
}

std::string extractYamlHeader(const FilePath& filePath)
{
   std::string contents;
   Error error = readStringFromFile(filePath, &contents);
   if (error)
      LOG_ERROR(error);
   
   return extractYamlHeader(contents);
}

std::string extractYamlHeader(const std::string& content)
{
   std::string result;
   boost::smatch match;
   
   if (boost::regex_search(content.begin(), content.end(), match, reYaml()))
      if (match.size() >= 1)
         result = match[1];
   
   return result;
}

namespace {

Error extractRCode(const std::string& contents,
                   const std::string& reOpen,
                   const std::string& reClose,
                   std::string* pContent)
{
   using namespace r::exec;
   RFunction extract(".rs.extractRCode");
   extract.addParam(contents);
   extract.addParam(reOpen);
   extract.addParam(reClose);
   Error error = extract.call(pContent);
   return error;
}

} // anonymous namespace

Error extractRCode(const std::string& fileContents,
                   const std::string& documentType,
                   std::string* pCode)
{
   using namespace source_database;
   Error error = Success();
   
   if (documentType == SourceDocument::SourceDocumentTypeRSource)
      *pCode = fileContents;
   else if (documentType == SourceDocument::SourceDocumentTypeRMarkdown)
      error = extractRCode(fileContents,
                           "^\\s*[`]{3}{\\s*[Rr](?:}|[\\s,].*})\\s*$",
                           "^\\s*[`]{3}\\s*$",
                           pCode);
   else if (documentType == SourceDocument::SourceDocumentTypeSweave)
      error = extractRCode(fileContents,
                           "^\\s*<<.*>>=\\s*$",
                           "^\\s*@\\s*$",
                           pCode);
   else if (documentType == SourceDocument::SourceDocumentTypeCpp)
      error = extractRCode(fileContents,
                           "^\\s*/[*]{3,}\\s*[rR]\\s*$",
                           "^\\s*[*]+/",
                           pCode);
   
   return error;
}

std::set<std::string> implicitlyAvailablePackages(const FilePath& filePath,
                                                  const std::string& contents)
{
   std::set<std::string> dependencies;
   
   if (modules::shiny::isShinyDocument(filePath, contents))
      dependencies.insert("shiny");
   
   return dependencies;
}

std::set<std::string> implicitlyAvailablePackages(const FilePath& filePath)
{
   std::string contents;
   Error error = readStringFromFile(filePath, &contents);
   if (error)
      LOG_ERROR(error);
   
   return implicitlyAvailablePackages(filePath, contents);
}

Error initialize()
{
   return Success();
}

} // namespace r_utils
} // namespace session
} // namespace rstudio
