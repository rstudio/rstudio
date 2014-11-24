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
#define kViewerCacheDir "viewer-cache"
#define kGridResourceLocation "/" kGridResource "/"

using namespace core;

namespace session {
namespace modules {
namespace data {
namespace viewer {

namespace {   

std::string viewerCacheDir() 
{
   return module_context::userScratchPath().childPath(kViewerCacheDir)
      .absolutePath();
}

SEXP rs_viewData(SEXP dataSEXP, SEXP captionSEXP, SEXP nameSEXP, SEXP envSEXP, 
                 SEXP cacheKeySEXP)
{    
   // attempt to reverse engineer the location of the data
   std::string envName, dataName, cacheKey;
   r::sexp::Protect protect;
   
   r::exec::RFunction("environmentName", envSEXP).call(&envName);
   if (envName == "R_GlobalEnv")
   {
      // the global environment doesn't need to be named
      envName.clear();
   }
   else if (envName == "R_EmptyEnv" || envName == "") 
   {
      envName = "_rs_no_env";
   }
   dataName = r::sexp::asString(nameSEXP);
   cacheKey = r::sexp::asString(cacheKeySEXP);

   try
   {
      // validate title
      if (!Rf_isString(captionSEXP) || Rf_length(captionSEXP) != 1)
         throw r::exec::RErrorException("invalid caption argument");
      
      // attempt to cast to a data frame
      SEXP dataFrameSEXP = NULL;
      r::exec::RFunction("as.data.frame", dataSEXP).call(
            &dataFrameSEXP, &protect);
      if (dataFrameSEXP != NULL)
         dataSEXP = dataFrameSEXP;
           
      int nrow = 0, ncol = 0;
      r::exec::RFunction("nrow", dataSEXP).call(&nrow);
      r::exec::RFunction("ncol", dataSEXP).call(&ncol);

      // fire show data event
      json::Object dataItem;
      dataItem["caption"] = r::sexp::asString(captionSEXP);
      dataItem["totalObservations"] = nrow;
      dataItem["displayedObservations"] = nrow;
      dataItem["variables"] = ncol;
      dataItem["cacheKey"] = cacheKey;
      dataItem["contentUrl"] = kGridResource "/gridviewer.html?env=" +
         http::util::urlEncode(envName, true) + "&obj=" + 
         http::util::urlEncode(dataName, true) + "&cache_key=" +
         http::util::urlEncode(cacheKey, true);
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

json::Value getCols(SEXP dataSEXP)
{
   std::vector<std::string> cols;

   SEXP namesSEXP = Rf_getAttrib(dataSEXP, R_NamesSymbol);
   if (TYPEOF(namesSEXP) != STRSXP || 
       Rf_length(namesSEXP) != Rf_length(dataSEXP))
   {
      // fake them if we don't have them
      for (int i = 0; i < Rf_length(dataSEXP); i++)
      {
         cols.push_back(boost::lexical_cast<std::string>(i + 1));
      }
   }
   else
   {
      // extract column names 
      r::sexp::extract(namesSEXP, &cols);
   }

   // add row ID column
   cols.insert(cols.begin(), "");
   return json::toJsonArray(cols);
}

json::Value getData(SEXP dataSEXP, const http::Fields& fields)
{
   r::sexp::Protect protect;

   // read draw parameters from DataTables
   int draw = http::util::fieldValue<int>(fields, "draw", 0);
   int start = http::util::fieldValue<int>(fields, "start", 0);
   int length = http::util::fieldValue<int>(fields, "length", 0);
   int ordercol = http::util::fieldValue<int>(fields, "order[0][column]", -1);
   std::string orderdir = http::util::fieldValue<std::string>(fields, "order[0][dir]", "asc");

   // apply sort if needed
   if (ordercol > 0) 
   {
      r::exec::RFunction(".rs.applySort", dataSEXP, ordercol, orderdir)
         .call(&dataSEXP, &protect);
   }

   // unfortunately Rf_nrow and Rf_ncol aren't applicable here
   int nrow = 0, ncol = 0;
   r::exec::RFunction("nrow", dataSEXP).call(&nrow);
   r::exec::RFunction("ncol", dataSEXP).call(&ncol);
   length = std::min(length, nrow - start);

   // DataTables uses 0-based indexing, but R uses 1-based indexing
   start ++;

   // extract the portion of the column vector requested by the client
   SEXP formattedDataSEXP = Rf_allocVector(VECSXP, ncol);
   protect.add(formattedDataSEXP);
   for (unsigned i = 0; i < static_cast<unsigned>(ncol); i++)
   {
      SEXP columnSEXP = VECTOR_ELT(dataSEXP, i);
      SEXP formattedColumnSEXP;
      r::exec::RFunction formatFx(".rs.formatDataColumn");
      formatFx.addParam(columnSEXP);
      formatFx.addParam(static_cast<int>(start));
      formatFx.addParam(static_cast<int>(length));
      Error error = formatFx.call(&formattedColumnSEXP, &protect);
      if (error)
         throw r::exec::RErrorException(error.summary());
      SET_VECTOR_ELT(formattedDataSEXP, i, formattedColumnSEXP);
    }

   // format the row names 
   SEXP rownamesSEXP;
   r::exec::RFunction(".rs.formatRowNames", dataSEXP, start, length)
      .call(&rownamesSEXP, &protect);
   
   // create the result grid as JSON
   json::Array data;
   for (int row = 0; row < length; row++)
   {
      json::Array rowData;
      if (rownamesSEXP != NULL &&
          TYPEOF(rownamesSEXP) != NILSXP &&
          !Rf_isNull(rownamesSEXP) )
      {
         SEXP nameSEXP = STRING_ELT(rownamesSEXP, row);
         if (nameSEXP != NULL &&
             nameSEXP != NA_STRING &&
             r::sexp::length(nameSEXP) > 0)
         {
            rowData.push_back(Rf_translateChar(nameSEXP));
         }
         else
         {
            rowData.push_back(row + start);
         }
      }
      else
      {
         rowData.push_back(row + start);
      }

      for (int col = 0; col<Rf_length(formattedDataSEXP); col++)
      {
         SEXP columnSEXP = VECTOR_ELT(formattedDataSEXP, col);
         if (columnSEXP != NULL && 
             TYPEOF(columnSEXP) != NILSXP &&
             !Rf_isNull(columnSEXP))
         {
            SEXP stringSEXP = STRING_ELT(columnSEXP, row);
            if (stringSEXP != NULL &&
                stringSEXP != NA_STRING &&
                r::sexp::length(stringSEXP) > 0)
            {
               rowData.push_back(Rf_translateChar(stringSEXP));
            }
            else
            {
               rowData.push_back("");
            }
         }
         else
         {
            rowData.push_back("");
         }
      }
      data.push_back(rowData);
   }

   json::Object result;
   result["draw"] = draw;
   result["recordsTotal"] = nrow;
   result["recordsFiltered"] = nrow;
   result["data"] = data;
   return result;
}

Error getGridData(const http::Request& request,
                  http::Response* pResponse)
{
   try
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
      std::string envName = http::util::urlDecode(
            http::util::fieldValue<std::string>(fields, "env", ""), true);
      std::string objName = http::util::urlDecode(
            http::util::fieldValue<std::string>(fields, "obj", ""), true);
      std::string cacheKey = http::util::urlDecode(
            http::util::fieldValue<std::string>(fields, "cache_key", ""), 
            true);
      std::string show = http::util::fieldValue<std::string>(
            fields, "show", "data");
      if (objName.empty() && cacheKey.empty()) 
      {
         return Success();
      }

      r::sexp::Protect protect;

      // attempt to find the original copy of the object
      SEXP dataSEXP = NULL;
      Error error = r::exec::RFunction(".rs.findDataFrame", envName, objName, 
            cacheKey, viewerCacheDir()).call(&dataSEXP, &protect);
      if (error) 
      {
         LOG_ERROR(error);
      }
      
      // couldn't find the original object--look it up from the cache
      if (dataSEXP == NULL || Rf_isNull(dataSEXP) || 
          TYPEOF(dataSEXP) == NILSXP)
      {
         // handle missing data here
         return Success();
      }

      // if the data is a promise (happens for built-in data), the value is
      // what we're looking for
      if (TYPEOF(dataSEXP) == PROMSXP) 
      {
         dataSEXP = PRVALUE(dataSEXP);
      }

      // TODO: handle missing data

      json::Value result;
      if (show == "cols")
      {
         result = getCols(dataSEXP);
      }
      else if (show == "data")
      {
         result = getData(dataSEXP, fields);
      }

      std::ostringstream ostr;
      json::write(result, ostr);
      pResponse->setStatusCode(http::status::Ok);
      pResponse->setBody(ostr.str());
   }

   CATCH_UNEXPECTED_EXCEPTION

   return Success();
}

// called by the client to expire data cached by an associated viewer tab
Error removeCachedData(const json::JsonRpcRequest& request,
                       json::JsonRpcResponse*)
{
   std::string cacheKey;
   Error error = json::readParam(request.params, 0, &cacheKey);
   if (error)
      return error;
   
   error = r::exec::RFunction(".rs.removeCachedData", cacheKey, 
         viewerCacheDir()).call();
   if (error)
      return error;

   return Success();
}

void onShutdown(bool terminatedNormally)
{
   // when R suspends or shuts down, write out the contents of the cache
   // environment to disk so we can load them again if we need to
   Error error = r::exec::RFunction(".rs.saveCachedData", viewerCacheDir())
      .call();
   if (error)
      LOG_ERROR(error);
}

void onSuspend(const r::session::RSuspendOptions&, core::Settings*)
{
   onShutdown(true);
}

void onResume(const Settings&)
{
}

} // anonymous namespace
   
Error initialize()
{
   using namespace module_context;

   // register viewData method
   R_CallMethodDef methodDef ;
   methodDef.name = "rs_viewData" ;
   methodDef.fun = (DL_FUNC) rs_viewData ;
   methodDef.numArgs = 5;
   r::routines::addCallMethod(methodDef);

   module_context::events().onShutdown.connect(onShutdown);
   addSuspendHandler(SuspendHandler(onSuspend, onResume));

   using boost::bind;
   using namespace r::function_hook ;
   using namespace session::module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(sourceModuleRFile, "SessionDataViewer.R"))
      (bind(registerRpcMethod, "remove_cached_data", removeCachedData))
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

