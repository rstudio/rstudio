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

SEXP rs_viewData(SEXP dataSEXP, SEXP captionSEXP)
{    
   // attempt to reverse engineer the location of the data
   // TODO: dataSEXP seems to be a copy for non-global environments; fall back
   // on deparse for non-global?
   std::string dataName, envName;
   SEXP env = R_GlobalEnv;
   r::sexp::Protect protect;
   while (dataName.empty() && env != R_EmptyEnv) 
   {
      std::vector<r::sexp::Variable> variables;
      r::sexp::listEnvironment(env, false, &protect, &variables);
      for (std::vector<r::sexp::Variable>::iterator var = variables.begin();
           var != variables.end();
           var++)
      {
         if (var->second == dataSEXP) 
         {
            dataName = var->first;
            // don't record the name of the global environment--the string
            // "R_GlobalEnv" doesn't translate back to an environment 
            if (env != R_GlobalEnv)
               r::exec::RFunction("environmentName", env).call(&envName);
            break;
         }
      }
      env = ENCLOS(env);
   }

   try
   {
      // validate title
      if (!Rf_isString(captionSEXP) || Rf_length(captionSEXP) != 1)
         throw r::exec::RErrorException("invalid caption argument");
           
      // validate data
      if (TYPEOF(dataSEXP) != VECSXP)
         throw r::exec::RErrorException("invalid data argument (not a list)");

      // fire show data event
      json::Object dataItem;
      dataItem["caption"] = r::sexp::asString(captionSEXP);
      dataItem["contentUrl"] = kGridResource "/gridviewer.html?env=" +
         http::util::urlEncode(envName, true) + "&obj=" + 
         http::util::urlEncode(dataName, true);
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
   std::string envName = http::util::fieldValue<std::string>(fields, "env", "");
   std::string objName = http::util::fieldValue<std::string>(fields, "obj", "");
   if (objName.empty())
   { 
      return Success();
   }
   r::sexp::Protect protect;
   std::vector<std::string> cols;
   SEXP dataSEXP = r::sexp::findVar(objName, envName);

   SEXP namesSEXP = Rf_getAttrib(dataSEXP, R_NamesSymbol);
   if (TYPEOF(namesSEXP) != STRSXP || 
       Rf_length(namesSEXP) != Rf_length(dataSEXP))
   {
      throw r::exec::RErrorException(
                           "invalid data argument (names not specified)");
   }
   Error error = r::sexp::extract(namesSEXP, &cols);

   // add row ID column
   cols.insert(cols.begin(), "");

   std::ostringstream ostr;
   json::write(json::toJsonArray(cols), ostr);
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
   std::string envName = http::util::fieldValue<std::string>(fields, "env", "");
   std::string objName = http::util::fieldValue<std::string>(fields, "obj", "");
   if (objName.empty()) 
   {
      return Success();
   }

   // get the draw parameters
   int draw = http::util::fieldValue<int>(fields, "draw", 0);
   int start = http::util::fieldValue<int>(fields, "start", 0);
   int length = http::util::fieldValue<int>(fields, "length", 0);
   int nrow = 0, ncol = 0;
   r::sexp::Protect protect;
   std::vector<std::string> cols;
   SEXP dataSEXP = r::sexp::findVar(objName, envName);

   // TODO: internal?
   r::exec::RFunction("nrow", dataSEXP).call(&nrow);
   r::exec::RFunction("ncol", dataSEXP).call(&ncol);
   length = std::min(length, nrow - start);

   // truncate and format columns 
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
   
   // add results
   json::Array data;
   for (int row = 0; row < length; row++)
   {
      json::Array rowData;
      // add the row number/id
      rowData.insert(rowData.begin(), 
            boost::lexical_cast<std::string>(row + 1 + start));

      for (int col = 0; col<Rf_length(formattedDataSEXP); col++)
      {
         SEXP columnSEXP = VECTOR_ELT(formattedDataSEXP, col);
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
      data.push_back(rowData);
   }

   json::Object result;
   result["draw"] = draw;
   result["recordsTotal"] = nrow;
   result["recordsFiltered"] = nrow;
   result["data"] = data;

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

