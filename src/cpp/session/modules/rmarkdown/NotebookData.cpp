/*
 * NotebookData.cpp
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
#include "NotebookData.hpp"
#include "NotebookOutput.hpp"

#include <iostream>

#include <boost/foreach.hpp>
#include <boost/format.hpp>

#include <r/RExec.hpp>
#include <r/RRoutines.hpp>

#include <core/Exec.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {

namespace {

SEXP rs_recordData(SEXP dataFileSEXP)
{
   events().onDataOutput(FilePath(r::sexp::safeAsString(dataFileSEXP)), FilePath());
   return R_NilValue;
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
         outputFolder.absolutePath(),
         chunkOptions).call();
}

core::Error initData()
{
   RS_REGISTER_CALL_METHOD(rs_recordData, 1);

   ExecBlock initBlock;
   initBlock.addFunctions()
      (boost::bind(module_context::sourceModuleRFile, "NotebookData.R"));

   return initBlock.execute();
}

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

