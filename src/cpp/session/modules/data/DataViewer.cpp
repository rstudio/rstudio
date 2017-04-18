/*
 * DataViewer.cpp
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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
#include <boost/foreach.hpp>
#include <boost/format.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <core/Log.hpp>
#include <core/Error.hpp>
#include <core/Exec.hpp>
#include <core/FileSerializer.hpp>
#include <core/RecursionGuard.hpp>
#include <core/StringUtils.hpp>
#include <core/SafeConvert.hpp>

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
#include <session/SessionSourceDatabase.hpp>

#ifndef _WIN32
#include <core/system/FileMode.hpp>
#endif

#define kGridResource "grid_resource"
#define kViewerCacheDir "viewer-cache"
#define kGridResourceLocation "/" kGridResource "/"
#define kNoBoundEnv "_rs_no_env"

// separates filter type from contents (e.g. "numeric|12-25")
#define kFilterSeparator "|"

// the largest number of factor values we're willing to display (after this
// point the column's text is searched as though it were a character column)
#define MAX_FACTORS 64

// special cell values
#define SPECIAL_CELL_NA 0

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace data {
namespace viewer {

namespace {   
/*
 * Data Viewer caching overview
 * ----------------------------
 *
 * For each object being viewed, there are three copies to consider:
 * 
 * ORIGINAL:
 *    The original object on which the user invoked View(). This object may or
 *    may not exist; for example, View(cars) binds a viewer to the 'cars' 
 *    object in 'package:datasets', but View(rbind(cars,cars)) binds a viewer
 *    to a temporary object that doesn't exist anywhere but in the viewer.
 *
 *    When the original object exists, and no sorting or filtering is applied,
 *    requests for data are met by pulling data from the original object. We
 *    also watch original objects; when they're replaced in their hosting
 *    environments (assuming those environments are named), a client event is
 *    emitted.
 *
 * CACHED:
 *    Because the original object may be temporary (and, even if not, can be
 *    deleted at any time), we always have a cached copy of the object 
 *    available. 
 *
 *    The environment .rs.CachedDataEnv contains the cached objects. These
 *    objects have randomly generated cache keys. 
 *
 *    When the session suspends/resumes, the contents of the cache environment
 *    are written as individual .RData files to the user scratch folder. This
 *    allows us to reload the data for viewing afterwards.
 *
 *    The client is responsible for letting the server know when the viewer has
 *    closed; when this happens, the server removes the in-memory and disk 
 *    cache entries.
 *
 * WORKING:
 *    As the user orders, filters, and searches data, it's typical to follow a
 *    narrowing approach--e.g. first show only "Housewares", then only
 *    housewares between $10-$25, then only housewares between $10-25 and
 *    matching the text "eggs".
 *    
 *    In order to avoid re-ordering and re-filtering the entire dataset every
 *    time a new set of rows is requested, we keep a "working copy" of the
 *    object in a second environment, .rs.WorkingDataEnv, using the same cache
 *    keys.
 *    
 *    When a request for data arrives, we check to see if the data requested is
 *    a subset of the data already in our working copy. If it is, we use the
 *    working copy as a starting postion rather than the original or cached
 *    object.
 *
 *    This allows us to efficiently perform operations on very large datasets
 *    once they've been winnowed down to smaller objects using searches and
 *    filters.
 */    

// indicates whether one filter string is a subset of another; e.g. if a column
// is filtered for "abc" and then "abcd", the new state is a subset of the
// previous state.
bool isFilterSubset(const std::string& outer, const std::string& inner) 
{
   // shortcut for identical filters (the typical case)
   if (inner == outer) 
      return true;

   // find filter separators; if we can't find them, presume no subset since we
   // can't parse filters
   size_t outerPipe = outer.find(kFilterSeparator);
   if (outerPipe == std::string::npos) 
      return false;
   size_t innerPipe = inner.find(kFilterSeparator);
   if (innerPipe == std::string::npos)
      return false;

   std::string outerType(outer.substr(0, outerPipe));
   std::string innerType(inner.substr(0, innerPipe));
   std::string outerValue(outer.substr(outerPipe + 1, 
            outer.length() - outerPipe));
   std::string innerValue(inner.substr(innerPipe + 1, 
            inner.length() - innerPipe));
   
   // only identical types can be subsets
   if (outerType != innerType) 
      return false;

   if (outerType == "numeric")
   {
      // matches a numeric filter (i.e. "2.71_3.14") -- in this case we need to
      // check the components for range inclusion
      boost::regex numFilter("(-?\\d+\\.?\\d*)_(-?\\d+\\.?\\d*)");
      boost::smatch innerMatch, outerMatch;
      if (regex_utils::search(innerValue, innerMatch, numFilter) &&
          regex_utils::search(outerValue, outerMatch, numFilter))
      {
         // for numeric filters, the inner is a subset if its lower bound (1)
         // is larger than the outer lower bound, and the upper bound (2) is
         // smaller than the outer upper bound
         return safe_convert::stringTo<double>(innerMatch[1], 0) >= 
                safe_convert::stringTo<double>(outerMatch[1], 0) &&
                safe_convert::stringTo<double>(innerMatch[2], 0) <= 
                safe_convert::stringTo<double>(outerMatch[2], 0);
      }

      // if not identical and not a range, then not a subset
      return false;
   } 
   else if (outerType == "factor" || outerType == "boolean")
   {
      // factors and boolean values have to be identical for subsetting, and we
      // already checked above
      return false;
   }
   else if (outerType == "character")
   {
      // characters are a subset if the outer string is within the inner one
      // (i.e. a seach for "walnuts" (inner) is within "walnut" (outer))
      return inner.find(outer) != std::string::npos;
   }
   
   // unknown filter type
   return false;
}

typedef enum 
{
  DIM_ROWS,
  DIM_COLS
} DimType;

// returns dimensions of an object safely--assumes dimension to be 0 unless we
// can succesfully obtain dimensions
int safeDim(SEXP data, DimType dimType)
{
   r::sexp::Protect protect;
   SEXP result = R_NilValue;
   Error err = r::exec::RFunction(dimType == DIM_ROWS ? 
         ".rs.nrow" : ".rs.ncol", data).call(&result, &protect);
   // bail if we encountered an error
   if (err)
   {
      LOG_ERROR(err);
      return 0;
   }

   if (TYPEOF(result) == INTSXP && Rf_length(result) > 0)
   {
      return INTEGER(result)[0];
   }

   return 0;
}

// CachedFrame represents an object that's currently active in a data viewer
// window.
struct CachedFrame
{
   CachedFrame(const std::string& env, const std::string& obj, SEXP sexp):
      envName(env),
      objName(obj),
      observedSEXP(sexp)
   {
      if (sexp == NULL)
         return;

      // cache list of column names
      r::sexp::Protect protect;
      SEXP namesSEXP;
      r::exec::RFunction("names", sexp).call(&namesSEXP, &protect);
      if (namesSEXP != NULL && TYPEOF(namesSEXP) != NILSXP 
          && !Rf_isNull(namesSEXP))
      {
         r::sexp::extract(namesSEXP, &colNames);
      }

      // cache number of columns
      ncol = safeDim(sexp, DIM_COLS);
   };

   CachedFrame() {};

   // The location of the frame (if we know it)
   std::string envName;
   std::string objName;

   // The frame's columns; used to determine whether the shape of the frame has
   // changed (necessitating a full reload of any displayed version of the
   // frame)
   int ncol;
   std::vector<std::string> colNames;

   // The current search string and filter set
   std::string workingSearch;
   std::vector<std::string> workingFilters;

   bool isSupersetOf(const std::string& newSearch, 
                     const std::vector<std::string> &newFilters)
   {
      if (!isFilterSubset(workingSearch, newSearch))
         return false;

      for (unsigned i = 0; 
           i < std::min(newFilters.size(), workingFilters.size()); 
           i++)
      {
         if (!isFilterSubset(workingFilters[i], newFilters[i]))
            return false;
      }

      return true;
   };

   // The current order column and direction
   int workingOrderCol;
   std::string workingOrderDir;

   // NB: There's no protection on this SEXP and it may be a stale pointer!
   // Used only to test for changes.
   SEXP observedSEXP;
};

// The set of active frames. Used primarily to check each for changes.
std::map<std::string, CachedFrame> s_cachedFrames;

std::string viewerCacheDir() 
{
   return module_context::sessionScratchPath().childPath(kViewerCacheDir)
      .absolutePath();
}

SEXP findInNamedEnvir(const std::string& envir, const std::string& name)
{
   SEXP env = NULL;
   r::sexp::Protect protect;

   // shortcut for unbound environment
   if (envir == kNoBoundEnv)
      return NULL;

   // use the global environment or resolve environment name
   if (envir.empty() || envir == "R_GlobalEnv")
      env = R_GlobalEnv;
   else 
      r::exec::RFunction(".rs.safeAsEnvironment", envir).call(&env, &protect);

   // if we failed to find an environment by name, return a null SEXP
   if (env == NULL || TYPEOF(env) == NILSXP || Rf_isNull(env))
      return NULL;

   // find the SEXP directly in the environment; return null if unbound
   SEXP obj = r::sexp::findVar(name, env); 
   return obj == R_UnboundValue ? NULL : obj;
}

// data items are used both as the payload for the client event that opens an
// editor viewer tab and as a server response when duplicating that tab's
// contents
json::Value makeDataItem(SEXP dataSEXP, const std::string& caption, 
                         const std::string& objName, const std::string& envName, 
                         const std::string& cacheKey, int preview)
{
   int nrow = safeDim(dataSEXP, DIM_ROWS);
   int ncol = safeDim(dataSEXP, DIM_COLS); 

   // fire show data event
   json::Object dataItem;
   dataItem["caption"] = caption;
   dataItem["totalObservations"] = nrow;
   dataItem["displayedObservations"] = nrow;
   dataItem["variables"] = ncol;
   dataItem["cacheKey"] = cacheKey;
   dataItem["object"] = objName;
   dataItem["environment"] = envName;
   dataItem["contentUrl"] = kGridResource "/gridviewer.html?env=" +
      http::util::urlEncode(envName, true) + "&obj=" + 
      http::util::urlEncode(objName, true) + "&cache_key=" +
      http::util::urlEncode(cacheKey, true);
   dataItem["preview"] = preview;

   return dataItem;
}

SEXP rs_viewData(SEXP dataSEXP, SEXP captionSEXP, SEXP nameSEXP, SEXP envSEXP, 
                 SEXP cacheKeySEXP, SEXP previewSEXP)
{    
   try
   {
      // attempt to reverse engineer the location of the data
      std::string envName, objName, cacheKey;
      r::sexp::Protect protect;
      
      // it's okay if this fails (and it might); we'll just treat the data as
      // unbound to an environment
      r::exec::RFunction("environmentName", envSEXP).call(&envName);
      if (envName == "R_GlobalEnv")
      {
         // the global environment doesn't need to be named
         envName.clear();
      }
      else if (envName == "R_EmptyEnv" || envName == "") 
      {
         envName = kNoBoundEnv;
      }
      objName = r::sexp::asString(nameSEXP);
      cacheKey = r::sexp::asString(cacheKeySEXP);

      // validate title
      if (!Rf_isString(captionSEXP) || Rf_length(captionSEXP) != 1)
         throw r::exec::RErrorException("invalid caption argument");
      
      // attempt to cast to a data frame
      SEXP dataFrameSEXP = R_NilValue;
      r::exec::RFunction asDataFrame("as.data.frame");
      asDataFrame.addParam("x", dataSEXP);
      asDataFrame.addParam("optional", true);  // don't require column names
      Error error = asDataFrame.call(&dataFrameSEXP, &protect);
      if (error) 
      {
         // caught below
         throw r::exec::RErrorException(error.summary());
      }
      if (dataFrameSEXP != NULL && dataFrameSEXP != R_NilValue)
      {
         dataSEXP = dataFrameSEXP;
      }
      else
      {
         // caught below
         throw r::exec::RErrorException("Could not coerce object to data frame.");
      }

      int preview = r::sexp::asLogical(previewSEXP) ? 1 : 0;

      json::Value dataItem = makeDataItem(dataSEXP, 
            r::sexp::asString(captionSEXP), objName, envName, cacheKey, preview);
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

   // setCacheableFile is responsible for emitting a 404 when the file doesn't
   // exist.
   core::FilePath gridResource = options().rResourcesPath().childPath(path);
   pResponse->setCacheableFile(gridResource, request);
}

json::Value getCols(SEXP dataSEXP)
{
   SEXP colsSEXP = R_NilValue;
   r::sexp::Protect protect;
   json::Value result;
   Error error = r::exec::RFunction(".rs.describeCols", dataSEXP, MAX_FACTORS)
      .call(&colsSEXP, &protect);
   if (error || colsSEXP == R_NilValue) 
   {
      json::Object err;
      if (error) 
         err["error"] = error.summary();
      else
         err["error"] = "Failed to retrieve column definitions for data.";
      result = err;
   }
   else 
   {
      r::json::jsonValueFromList(colsSEXP, &result);
   }
   return result;
}

// given an object from which to return data, and a description of the data to
// return via URL-encoded paramters supplied by the DataTables API, returns the
// data requested by the parameters. 
//
// the shape of the API is described here:
// http://datatables.net/manual/server-side
// 
// NB: may throw exceptions! these are expected to be handled by the handlers
// in getGridData, where they will be marshaled to JSON and displayed on the
// client.
json::Value getData(SEXP dataSEXP, const http::Fields& fields)
{
   Error error;
   r::sexp::Protect protect;

   // read draw parameters from DataTables
   int draw = http::util::fieldValue<int>(fields, "draw", 0);
   int start = http::util::fieldValue<int>(fields, "start", 0);
   int length = http::util::fieldValue<int>(fields, "length", 0);
   int ordercol = http::util::fieldValue<int>(fields, "order[0][column]", 
         -1);
   std::string orderdir = http::util::fieldValue<std::string>(fields, 
         "order[0][dir]", "asc");
   std::string search = http::util::urlDecode(
         http::util::fieldValue<std::string>(fields, "search[value]", ""), 
         true);
   std::string cacheKey = http::util::urlDecode(
         http::util::fieldValue<std::string>(fields, "cache_key", ""), 
         true);

   int nrow = safeDim(dataSEXP, DIM_ROWS);
   int ncol = safeDim(dataSEXP, DIM_COLS);
   int filteredNRow = 0;

   // extract filters
   std::vector<std::string> filters;
   bool hasFilter = false;
   for (int i = 1; i <= ncol; i++) 
   {
      std::string filterVal = http::util::urlDecode( 
            http::util::fieldValue<std::string>(fields,
                  "columns[" + boost::lexical_cast<std::string>(i) + "]" 
                  "[search][value]", ""), true);
      if (!filterVal.empty()) 
      {
         hasFilter = true;
      }
      filters.push_back(filterVal);
   }

   bool needsTransform = ordercol > 0 || hasFilter || !search.empty();
   bool hasTransform = false;

   // check to see if we have an ordered/filtered view we can build from
   std::map<std::string, CachedFrame>::iterator cachedFrame = 
      s_cachedFrames.find(cacheKey);
   if (needsTransform)
   {
      if (cachedFrame != s_cachedFrames.end())
      {
         // do we have a previously ordered/filtered view?
         SEXP workingDataSEXP = NULL;
         r::exec::RFunction(".rs.findWorkingData", cacheKey)
            .call(&workingDataSEXP, &protect);
         if (workingDataSEXP != NULL && TYPEOF(workingDataSEXP) != NILSXP &&
             !Rf_isNull(workingDataSEXP))
         {
            if (cachedFrame->second.workingSearch == search &&
                cachedFrame->second.workingFilters == filters && 
                cachedFrame->second.workingOrderDir == orderdir &&
                cachedFrame->second.workingOrderCol == ordercol)
            {
               // we have one with exactly the same parameters as requested;
               // use it exactly as is
               dataSEXP = workingDataSEXP;
               needsTransform = false;
               hasTransform = true;
            } 
            else if (cachedFrame->second.isSupersetOf(search, filters))
            {
               // we have one that is a strict superset of the parameters
               // requested; transform the filtered set instead of starting
               // from scratch
               dataSEXP = workingDataSEXP;
            }
         }
      }
   }

   // apply transformations if needed.    
   if (needsTransform) 
   {
      // can we use a working copy? 
      r::exec::RFunction transform(".rs.applyTransform");
      transform.addParam("x", dataSEXP);       // data to transform
      transform.addParam("filtered", filters); // which columns are filtered
      transform.addParam("search", search);    // global search (across cols)
      transform.addParam("col", ordercol);     // which column to order on
      transform.addParam("dir", orderdir);     // order direction ("asc"/"desc")
      transform.call(&dataSEXP, &protect);
      if (error)
         throw r::exec::RErrorException(error.summary());

      // check to see if we've accidentally transformed ourselves into nothing
      // (this shouldn't generally happen without a specific error)
      if (dataSEXP == NULL || TYPEOF(dataSEXP) == NILSXP || 
          Rf_isNull(dataSEXP)) 
      {
         throw r::exec::RErrorException("Failure to sort or filter data");
      }

      // save the working data state (it's okay if this fails; it's a
      // performance optimization)
      r::exec::RFunction(".rs.assignWorkingData", cacheKey, dataSEXP).call();
      if (cachedFrame != s_cachedFrames.end())
      {
         cachedFrame->second.workingSearch = search;
         cachedFrame->second.workingFilters = filters;
         cachedFrame->second.workingOrderDir = orderdir;
         cachedFrame->second.workingOrderCol = ordercol;
      }
   }

   // apply new row count if we've tansformed the data (or need to)
   filteredNRow = needsTransform || hasTransform ?
      safeDim(dataSEXP, DIM_ROWS) : 
      nrow;

   // return the lesser of the rows available and rows requested
   length = std::min(length, filteredNRow - start);

   // DataTables uses 0-based indexing, but R uses 1-based indexing
   start ++;

   // extract the portion of the column vector requested by the client
   SEXP formattedDataSEXP = Rf_allocVector(VECSXP, ncol);
   protect.add(formattedDataSEXP);
   for (unsigned i = 0; i < static_cast<unsigned>(ncol); i++)
   {
      SEXP columnSEXP = VECTOR_ELT(dataSEXP, i);
      if (columnSEXP == NULL || TYPEOF(columnSEXP) == NILSXP || 
          Rf_isNull(columnSEXP))
      {
         throw r::exec::RErrorException("No data in column " + 
               boost::lexical_cast<std::string>(i));
      }
      SEXP formattedColumnSEXP;
      r::exec::RFunction formatFx(".rs.formatDataColumn");
      formatFx.addParam(columnSEXP);
      formatFx.addParam(static_cast<int>(start));
      formatFx.addParam(static_cast<int>(length));
      error = formatFx.call(&formattedColumnSEXP, &protect);
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
            rowData.push_back(Rf_translateCharUTF8(nameSEXP));
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
               rowData.push_back(Rf_translateCharUTF8(stringSEXP));
            }
            else if (stringSEXP == NA_STRING) 
            {
               rowData.push_back(SPECIAL_CELL_NA);
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
   result["recordsFiltered"] = filteredNRow;
   result["data"] = data;
   return result;
}

Error getGridData(const http::Request& request,
                  http::Response* pResponse)
{
   json::Value result;
   http::status::Code status = http::status::Ok;

   try
   {
      // find the data frame we're going to be pulling data from
      http::Fields fields;
      http::util::parseForm(request.body(), &fields);
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

      // begin observing if we aren't already
      if (envName != kNoBoundEnv) 
      {
         SEXP objSEXP = findInNamedEnvir(envName, objName);
         std::map<std::string, CachedFrame>::iterator it = 
            s_cachedFrames.find(cacheKey);
         if (it == s_cachedFrames.end())
            s_cachedFrames[cacheKey] = CachedFrame(envName, objName, objSEXP);
      }

      // attempt to find the original copy of the object (loads from cache key
      // if necessary)
      SEXP dataSEXP = R_NilValue;
      Error error = r::exec::RFunction(".rs.findDataFrame", envName, objName, 
            cacheKey, viewerCacheDir()).call(&dataSEXP, &protect);
      if (error) 
      {
         LOG_ERROR(error);
      }
      
      // couldn't find the original object
      if (dataSEXP == NULL || dataSEXP == R_UnboundValue || 
          Rf_isNull(dataSEXP) || TYPEOF(dataSEXP) == NILSXP)
      {
         json::Object err;
         err["error"] = "The object no longer exists.";
         status = http::status::NotFound;
         result = err;
      }
      else 
      {
         // if the data is a promise (happens for built-in data), the value is
         // what we're looking for
         if (TYPEOF(dataSEXP) == PROMSXP) 
         {
            dataSEXP = PRVALUE(dataSEXP);
         }
      
         if (show == "cols")
         {
            result = getCols(dataSEXP);
         }
         else if (show == "data")
         {
            result = getData(dataSEXP, fields);
         }
      }

   }
   catch(r::exec::RErrorException& e)
   {
      // marshal R errors to the client in the format DataTables (and our own
      // error handling code) expects
      json::Object err;
      err["error"] = e.message();
      result = err;
      status = http::status::InternalServerError;
   }
   CATCH_UNEXPECTED_EXCEPTION

   std::ostringstream ostr;
   json::write(result, ostr);

   // There are some unprintable ASCII control characters that are written
   // verbatim by json::write, but that won't parse in most Javascript JSON
   // parsing implementations, even if contained in a string literal. Scan the
   // output data for these characters and replace them with spaces. Escaping
   // is another option here for some character ranges but since (a) these are
   // unprintable and (b) some characters are invalid *even if escaped* e.g.
   // \v, there's little to be gained here in trying to marshal them to the
   // viewer.
   std::string output = ostr.str();
   for (size_t i = 0; i < output.size(); i++) 
   {
      char c = output[i];
      // These ranges for control character values come from empirical testing
      if ((c >= 1 && c <= 7) || c == 11 || (c >= 14 && c <= 31))
      {
         output[i] = ' ';
      }
   }
 
   pResponse->setNoCacheHeaders();    // don't cache data/grid shape
   pResponse->setStatusCode(status);
   pResponse->setBody(output);

   return Success();
}

Error removeCacheKey(const std::string& cacheKey)
{
   // remove from watchlist
   std::map<std::string, CachedFrame>::iterator pos = 
      s_cachedFrames.find(cacheKey);
   if (pos != s_cachedFrames.end())
      s_cachedFrames.erase(pos);
   
   // remove cache env object and backing file
   return r::exec::RFunction(".rs.removeCachedData", cacheKey, 
         viewerCacheDir()).call();
}

// called by the client to expire data cached by an associated viewer tab
Error removeCachedData(const json::JsonRpcRequest& request,
                       json::JsonRpcResponse*)
{
   std::string cacheKey;
   Error error = json::readParam(request.params, 0, &cacheKey);
   if (error)
      return error;

   return removeCacheKey(cacheKey);
}

void onShutdown(bool terminatedNormally)
{
   if (terminatedNormally) 
   {
      // when R suspends or shuts down, write out the contents of the cache
      // environment to disk so we can load them again if we need to
      Error error = r::exec::RFunction(".rs.saveCachedData", viewerCacheDir())
         .call();
      if (error)
         LOG_ERROR(error);
   }
}

void onSuspend(const r::session::RSuspendOptions&, core::Settings*)
{
   onShutdown(true);
}

void onResume(const Settings&)
{
}

void onDetectChanges(module_context::ChangeSource source)
{
   DROP_RECURSIVE_CALLS;

   // unlikely that data will change outside of a REPL
   if (source != module_context::ChangeSourceREPL) 
      return;

   r::sexp::Protect protect;
   for (std::map<std::string, CachedFrame>::iterator i = s_cachedFrames.begin();
        i != s_cachedFrames.end();
        i++) 
   {
      SEXP sexp = findInNamedEnvir(i->second.envName, i->second.objName);
      if (sexp != i->second.observedSEXP) 
      {
         // create a new frame object to capture the new state of the frame
         CachedFrame newFrame(i->second.envName, i->second.objName, sexp);

         // clear working data for the object
         r::exec::RFunction(".rs.removeWorkingData", i->first).call();

         // replace cached copy (if we have something to replace it with)
         if (sexp != NULL)
            r::exec::RFunction(".rs.assignCachedData", 
                  i->first, sexp, i->second.objName).call();

         // emit client event
         json::Object changed;
         changed["cache_key"] = i->first;
         changed["structure_changed"] = i->second.ncol != newFrame.ncol || 
            i->second.colNames != newFrame.colNames;
         ClientEvent event(client_events::kDataViewChanged, changed);
         module_context::enqueClientEvent(event);

         // replace old frame with new
         s_cachedFrames[i->first] = newFrame;
      }
   }
}

void onClientInit()
{
   // ensure the viewer cache directory exists--we create this eagerly on 
   // client init (rather than on-demand) so we have time to correct its 
   // permissions 
   FilePath cacheDir(viewerCacheDir());
   if (cacheDir.exists())
      return;

   Error error = cacheDir.ensureDirectory();
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

#ifndef _WIN32
   // tighten permissions on viewer cache directory
   error = core::system::changeFileMode(
            cacheDir,  core::system::UserReadWriteExecuteMode);
   if (error)
   {
      // not fatal, log and continue
      LOG_ERROR(error);
   }
#endif
}

void onDocPendingRemove(
        boost::shared_ptr<source_database::SourceDocument> pDoc)
{
   // see if the document has a path (if it does, it can't be a data viewer
   // item)
   std::string path;
   source_database::getPath(pDoc->id(), &path);
   if (!path.empty())
      return;

   // see if it has a cache key we need to remove (if not, no work to do)
   std::string cacheKey = pDoc->getProperty("cacheKey");
   if (cacheKey.empty())
      return;

   // remove cache env object and backing file
   Error error = removeCacheKey(cacheKey);
   if (error)
      LOG_ERROR(error);
}

void onDeferredInit(bool newSession)
{
   // get all the cache keys in the source database
   std::vector<boost::shared_ptr<source_database::SourceDocument> > docs;
   Error error = source_database::list(&docs);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   std::vector<std::string> sourceKeys;
   BOOST_FOREACH(boost::shared_ptr<source_database::SourceDocument> pDoc, docs)
   {
      std::string key = pDoc->getProperty("cacheKey");
      if (!key.empty())
         sourceKeys.push_back(key);
   }
   
   // get all the cache keys in the cache
   FilePath cache(viewerCacheDir());
   std::vector<FilePath> cacheFiles;
   if (cache.exists())
   {
      Error error = cache.children(&cacheFiles);
      if (error)
      {
         LOG_ERROR(error);
         return;
      }
   }

   std::vector<std::string> cacheKeys;
   BOOST_FOREACH(const FilePath& cacheFile, cacheFiles)
   {
      cacheKeys.push_back(cacheFile.stem());
   }

   // sort each set of keys (so we can diff the sets below)
   std::sort(sourceKeys.begin(), sourceKeys.end());
   std::sort(cacheKeys.begin(), cacheKeys.end());

   std::vector<std::string> orphanKeys;
   std::set_difference(cacheKeys.begin(), cacheKeys.end(),
                       sourceKeys.begin(), sourceKeys.end(),
                       std::back_inserter(orphanKeys));

   // remove each key no longer bound to a source file
   BOOST_FOREACH(const std::string& orphanKey, orphanKeys)
   {
      error = cache.complete(orphanKey + ".Rdata").removeIfExists();
      if (error)
         LOG_ERROR(error);
   }
}

} // anonymous namespace
   
Error initialize()
{
   using namespace module_context;

   // register viewData method
   R_CallMethodDef methodDef ;
   methodDef.name = "rs_viewData" ;
   methodDef.fun = (DL_FUNC) rs_viewData ;
   methodDef.numArgs = 6;
   r::routines::addCallMethod(methodDef);

   source_database::events().onDocPendingRemove.connect(onDocPendingRemove);

   module_context::events().onShutdown.connect(onShutdown);
   module_context::events().onDetectChanges.connect(onDetectChanges);
   module_context::events().onClientInit.connect(onClientInit);
   module_context::events().onDeferredInit.connect(onDeferredInit);
   addSuspendHandler(SuspendHandler(onSuspend, onResume));

   using boost::bind;
   using namespace rstudio::r::function_hook ;
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
} // namespace session
} // namespace rstudio

