/*
 * DataViewer.cpp
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

#include "DataViewer.hpp"

#include <string>
#include <vector>
#include <sstream>

#include <boost/bind.hpp>
#include <boost/format.hpp>

#include <core/Log.hpp>
#include <core/Error.hpp>
#include <core/Exec.hpp>
#include <core/FileSerializer.hpp>
#include <core/StringUtils.hpp>

#define R_INTERNAL_FUNCTIONS
#include <r/RInternal.hpp>
#include <r/RSexp.hpp>
#include <r/RExec.hpp>
#include <r/RJson.hpp>
#include <r/ROptions.hpp>
#include <r/RFunctionHook.hpp>
#include <r/RRoutines.hpp>

#include <session/SessionModuleContext.hpp>

#include <session/SessionContentUrls.hpp>

#define kGridResource "grid_resource"
#define kGridResourceLocation "/" kGridResource "/"

using namespace core;

namespace session {
namespace modules {
namespace data {
namespace viewer {

namespace {   

class CachedFrame 
{
public:
   CachedFrame(SEXP dataSEXP);
   json::Value getColumns();
   json::Value getData(int data, int start, int length);
private:
   std::vector<std::string> cols_;
   std::vector<std::vector<std::string> > data_;
};

CachedFrame::CachedFrame(SEXP dataSEXP)
{
   SEXP namesSEXP = Rf_getAttrib(dataSEXP, R_NamesSymbol);
   if (TYPEOF(namesSEXP) != STRSXP || 
       Rf_length(namesSEXP) != Rf_length(dataSEXP))
   {
      throw r::exec::RErrorException(
                           "invalid data argument (names not specified)");
   }
   Error error = r::sexp::extract(namesSEXP, &cols_);

   unsigned rowCount = 0;
   std::vector<unsigned> columnLengths;
   for (unsigned i = 0; i < cols_.size(); i++)
   {
       SEXP columnSEXP = VECTOR_ELT(dataSEXP, i);
       unsigned columnLength = r::sexp::length(columnSEXP);
       columnLengths.push_back(columnLength);
       rowCount = std::max(columnLength, rowCount);
   }

   // consider: for very large data we should only format/extract the page of
   // the data frame being requested, instead of doing everything up front
   r::sexp::Protect rProtect;
   SEXP formattedDataSEXP = Rf_allocVector(VECSXP, cols_.size());
   rProtect.add(formattedDataSEXP);
   for (unsigned i = 0; i < cols_.size(); i++)
   {
      SEXP columnSEXP = VECTOR_ELT(dataSEXP, i);
      SEXP formattedColumnSEXP;
      r::exec::RFunction formatFx(".rs.formatDataColumn");
      formatFx.addParam(columnSEXP);
      formatFx.addParam(static_cast<int>(rowCount));
      Error error = formatFx.call(&formattedColumnSEXP, &rProtect);
      if (error)
         throw r::exec::RErrorException(error.summary());
      SET_VECTOR_ELT(formattedDataSEXP, i, formattedColumnSEXP);
    }

   for (unsigned row = 0; row < rowCount; row++)
   {
      data_.push_back(std::vector<std::string>());
      for (int col = 0; col<Rf_length(formattedDataSEXP); col++)
      {
         if (columnLengths[col] > row)
         {
            SEXP columnSEXP = VECTOR_ELT(formattedDataSEXP, col);
            SEXP stringSEXP = STRING_ELT(columnSEXP, row);
            if (stringSEXP != NULL &&
                stringSEXP != NA_STRING &&
                r::sexp::length(stringSEXP) > 0)
            {
               data_[row].push_back(Rf_translateChar(stringSEXP));
            }
            else
            {
               data_[row].push_back("");
            }
         }
         else
         {
            data_[row].push_back("");
         }
      }
   }
}

json::Value CachedFrame::getColumns() 
{
   return json::toJsonArray(cols_);
}

json::Value CachedFrame::getData(int draw, int start, int length)
{
   json::Object result;
   // set basic parameters
   result["draw"] = draw;
   result["recordsTotal"] = json::toJsonValue(static_cast<int>(data_.size()));
   result["recordsFiltered"] = json::toJsonValue(static_cast<int>(data_.size()));

   // add results
   json::Array rows;
   int max = std::min(start + length, static_cast<int>(data_.size()));
   for (int i = start; i < max; i++) 
   {
      rows.push_back(json::toJsonArray(data_[i]));
   }
   result["data"] = rows;
   return result;
}

std::vector<boost::shared_ptr<CachedFrame> > s_frames;

SEXP rs_viewData(SEXP dataSEXP, SEXP captionSEXP)
{    
   try
   {
      // validate title
      if (!Rf_isString(captionSEXP) || Rf_length(captionSEXP) != 1)
         throw r::exec::RErrorException("invalid caption argument");
           
      // validate data
      if (TYPEOF(dataSEXP) != VECSXP)
         throw r::exec::RErrorException("invalid data argument (not a list)");

      // create the cached frame
      s_frames.push_back(boost::make_shared<CachedFrame>(dataSEXP));

      // fire show data event
      json::Object dataItem;
      dataItem["caption"] = r::sexp::asString(captionSEXP);
      dataItem["contentUrl"] = kGridResource "/index.html?" +
         boost::lexical_cast<std::string>(s_frames.size() - 1);
      ClientEvent event(client_events::kShowData, dataItem);
      module_context::enqueClientEvent(event);

      // done
      return R_NilValue;
   }
   catch(r::exec::RErrorException& e)
   {
      r::exec::error(e.message());
   }
   CATCH_UNEXPECTED_EXCEPTION
   
   // keep compiler happy
   return R_NilValue;
}
  
void handleGridResReq(const http::Request& request,
                            http::Response* pResponse)
{
   std::string path("grid/");
   path.append(http::util::pathAfterPrefix(request, kGridResourceLocation));

   core::FilePath gridResource = options().rResourcesPath().childPath(path);
   if (gridResource.exists())
   {
      pResponse->setCacheableFile(gridResource, request);
      return;
   }
}

Error getGridShape(const http::Request& request,
                   http::Response* pResponse)
{
   // extract the query string
   std::string::size_type pos = request.uri().find('?');
   if (pos == std::string::npos)
   {
      return Success();
   }

   // find the data frame we're going to be pulling data from
   std::string queryString = request.uri().substr(pos+1);
   http::Fields fields;
   http::util::parseQueryString(queryString, &fields);
   int frameId = http::util::fieldValue<int>(fields, "frame_id", -1);
   if (frameId < 0)
   {
      return Success();
   }

   json::Value result = s_frames[frameId]->getColumns();
   
   std::ostringstream ostr;
   json::write(result, ostr);
   pResponse->setStatusCode(http::status::Ok);
   pResponse->setBody(ostr.str());
   return Success();
}

Error getGridData(const http::Request& request,
                  http::Response* pResponse)
{
   // extract the query string
   std::string::size_type pos = request.uri().find('?');
   if (pos == std::string::npos)
   {
      return Success();
   }

   // find the data frame we're going to be pulling data from
   std::string queryString = request.uri().substr(pos+1);
   http::Fields fields;
   http::util::parseQueryString(queryString, &fields);
   int frameId = http::util::fieldValue<int>(fields, "frame_id", -1);
   if (frameId < 0) 
   {
      return Success();
   }
   
   json::Value result = s_frames[frameId]->getData(
         http::util::fieldValue<int>(fields, "draw", 0),
         http::util::fieldValue<int>(fields, "start", 0),
         http::util::fieldValue<int>(fields, "length", 10));
   
   std::ostringstream ostr;
   json::write(result, ostr);
   pResponse->setStatusCode(http::status::Ok);
   pResponse->setBody(ostr.str());
   return Success();
}


} // anonymous namespace
   
Error initialize()
{
   // register viewData method
   R_CallMethodDef methodDef ;
   methodDef.name = "rs_viewData" ;
   methodDef.fun = (DL_FUNC) rs_viewData ;
   methodDef.numArgs = 2;
   r::routines::addCallMethod(methodDef);

   using boost::bind;
   using namespace r::function_hook ;
   using namespace session::module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(sourceModuleRFile, "SessionDataViewer.R"))
      (bind(registerUriHandler, "/grid_shape", getGridShape))
      (bind(registerUriHandler, "/grid_data", getGridData))
      (bind(registerUriHandler, kGridResourceLocation, handleGridResReq));

   Error error = initBlock.execute();
   if (error)
       return error;

   // initialize data viewer (don't make failure fatal because we are
   // adding this code in a hot patch release)
   bool server = session::options().programMode() == kSessionProgramModeServer;
   error = r::exec::RFunction(".rs.initializeDataViewer", server).call();
   if (error)
       LOG_ERROR(error);

   return Success();
}


} // namespace viewer
} // namespace data
} // namespace modules
} // namesapce session

