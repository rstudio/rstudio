/*
 * NotebookData.cpp
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

#include "SessionRmdNotebook.hpp"
#include "NotebookData.hpp"
#include "NotebookOutput.hpp"

#include <iostream>

#include <boost/format.hpp>

#include <r/RExec.hpp>
#include <r/RRoutines.hpp>
#include <r/RJson.hpp>

#include <core/Exec.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;


#define kNotebookDataResource "rmd_data"
#define kNotebookDataResourceLocation "/" kNotebookDataResource "/"

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {

namespace {

SEXP rs_recordData(SEXP dataFileSEXP, SEXP metadata)
{
   json::Value meta;
   Error error = r::json::jsonValueFromObject(metadata, &meta);
   if (error)
      LOG_ERROR(error);
   events().onDataOutput(FilePath(r::sexp::safeAsString(dataFileSEXP)), 
         FilePath(), meta);
   return R_NilValue;
}

void handleNotebookDataResReq(const http::Request& request,
                              http::Response* pResponse)
{
   std::string resourceName = http::util::pathAfterPrefix(request, kNotebookDataResourceLocation);

   std::string resourcePath("pagedtable/");
   resourcePath.append(http::util::pathAfterPrefix(request, kNotebookDataResourceLocation));

   core::FilePath pagedTableResource = options().rResourcesPath().completeChildPath(resourcePath);

   pResponse->setCacheableFile(pagedTableResource, request);
}

} // anonymous namespace

// provide default constructor/destructor
DataCapture::DataCapture()
{
}

DataCapture::~DataCapture()
{
}

void DataCapture::disconnect()
{
   // stop capturing data when the prompt returns
   Error error = r::exec::RFunction(".rs.releaseDataCapture").call();
   if (error)
      LOG_ERROR(error);
   
   NotebookCapture::disconnect();
}

core::Error DataCapture::connectDataCapture(
              const core::FilePath& outputFolder,
              const json::Object& chunkOptions)
{
   return r::exec::RFunction(".rs.initDataCapture",
         string_utils::utf8ToSystem(outputFolder.getAbsolutePath()),
         chunkOptions).call();
}

core::Error initData()
{
   RS_REGISTER_CALL_METHOD(rs_recordData, 2);

   ExecBlock initBlock;
   initBlock.addFunctions()
      (boost::bind(module_context::sourceModuleRFile, "NotebookData.R"))
      (boost::bind(module_context::registerUriHandler, kNotebookDataResourceLocation, handleNotebookDataResReq));

   return initBlock.execute();
}

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

