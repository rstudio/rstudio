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
#include <r/RRoutines.hpp>

#include <session/SessionModuleContext.hpp>

#include <boost/regex.hpp>

#include <core/YamlUtil.hpp>

#include "shiny/SessionShiny.hpp"

namespace rstudio {

using namespace core;
using namespace core::yaml;

namespace session {
namespace r_utils {


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
   
   if (modules::shiny::getShinyFileType(filePath, contents) != 
       modules::shiny::ShinyNone)
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

namespace {

SEXP rs_fromJSON(SEXP objectSEXP)
{
   std::string contents = r::sexp::asString(objectSEXP);
   
   json::Value jsonValue;
   if (!json::parse(contents, &jsonValue))
      return R_NilValue;
   
   r::sexp::Protect protect;
   return r::sexp::create(jsonValue, &protect);
}

SEXP rs_isNullExternalPointer(SEXP objectSEXP)
{
   using namespace r::sexp;
   
   Protect protect;
   return create(isNullExternalPointer(objectSEXP), &protect);
}

} // anonymous namespace

Error initialize()
{
   RS_REGISTER_CALL_METHOD(rs_fromJSON, 1);
   RS_REGISTER_CALL_METHOD(rs_isNullExternalPointer, 1);
   
   using boost::bind;
   using namespace module_context;
   
   ExecBlock initBlock;
   initBlock.addFunctions()
         (bind(sourceModuleRFile, "SessionRUtil.R"));
   return initBlock.execute();
}

} // namespace r_utils
} // namespace session
} // namespace rstudio
