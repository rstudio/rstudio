/*
 * NotebookHtmlWidgets.cpp
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

#include "SessionRmdNotebook.hpp"
#include "NotebookHtmlWidgets.hpp"
#include "NotebookOutput.hpp"

#include <iostream>

#include <boost/foreach.hpp>
#include <boost/format.hpp>

#include <r/RExec.hpp>
#include <r/RRoutines.hpp>
#include <r/RJson.hpp>

#include <core/Exec.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {

namespace {

SEXP rs_recordHtmlWidget(SEXP htmlFileSEXP, SEXP depFileSEXP, SEXP metadata)
{
   json::Value meta;
   Error error = r::json::jsonValueFromObject(metadata, &meta);
   if (error)
      LOG_ERROR(error);
   events().onHtmlOutput(FilePath(r::sexp::safeAsString(htmlFileSEXP)), 
                         FilePath(r::sexp::safeAsString(depFileSEXP)), meta);
   return R_NilValue;
}

bool moveLibFile(const FilePath& from, const FilePath& to, 
      const FilePath& path)
{
   std::string relativePath = path.relativePath(from);
   FilePath target = to.complete(relativePath);

   Error error = path.isDirectory() ?
                     target.ensureDirectory() :
                     path.move(target);
   if (error)
      LOG_ERROR(error);
   return true;
}


} // anonymous namespace

// provide default constructor/destructor
HtmlCapture::HtmlCapture()
{
}

HtmlCapture::~HtmlCapture()
{
}

void HtmlCapture::disconnect()
{
   // stop capturing HTML widgets when the prompt returns
   Error error = r::exec::RFunction(".rs.releaseHtmlCapture").call();
   if (error)
      LOG_ERROR(error);
   
   NotebookCapture::disconnect();
}

core::Error HtmlCapture::connectHtmlCapture(
              const core::FilePath& outputFolder,
              const core::FilePath& libraryFolder,
              const json::Object& chunkOptions)
{
   return r::exec::RFunction(".rs.initHtmlCapture", 
         outputFolder.absolutePath(),
         outputFolder.complete(kChunkLibDir).absolutePath(),
         chunkOptions).call();
}

core::Error initHtmlWidgets()
{
   RS_REGISTER_CALL_METHOD(rs_recordHtmlWidget, 3);

   ExecBlock initBlock;
   initBlock.addFunctions()
      (boost::bind(module_context::sourceModuleRFile, "NotebookHtmlWidgets.R"));

   return initBlock.execute();
}

core::Error mergeLib(const core::FilePath& source, 
                     const core::FilePath& target)
{
   return source.childrenRecursive(
         boost::bind(moveLibFile, source, target, _2));
}

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

