/*
 * SessionRMarkdown.cpp
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

#include "SessionRMarkdown.hpp"

#include <boost/algorithm/string/predicate.hpp>

#include <core/FileSerializer.hpp>

#include <r/RExec.hpp>

#include <session/SessionModuleContext.hpp>

using namespace core ;

namespace session {
namespace modules { 
namespace rmarkdown {

namespace {

void initPandocPath()
{
   r::exec::RFunction sysSetenv("Sys.setenv");
   sysSetenv.addParam("RSTUDIO_PANDOC", 
                      session::options().pandocPath().absolutePath());
   Error error = sysSetenv.call();
   if (error)
      LOG_ERROR(error);
}

// when the RMarkdown package is installed, give .Rmd files the extended type
// "rmarkdown", unless they contain a special marker that indicates we should
// use the previous rendering strategy
std::string onDetectRmdSourceType(
      boost::shared_ptr<source_database::SourceDocument> pDoc)
{
   if (!pDoc->path().empty())
   {
      FilePath filePath = module_context::resolveAliasedPath(pDoc->path());
      if (filePath.extensionLowerCase() == ".rmd" &&
          !boost::algorithm::icontains(pDoc->contents(),
                                       "<!-- rmarkdown v1 -->"))
      {
         return "rmarkdown";
      }
   }
   return std::string();
}

} // anonymous namespace

Error initialize()
{
   initPandocPath();

   if (module_context::isPackageVersionInstalled("rmarkdown", "0.1"))
      module_context::events().onDetectSourceExtendedType
                              .connect(onDetectRmdSourceType);

   return Success();
}
   
} // namepsace rmarkdown
} // namespace modules
} // namesapce session

