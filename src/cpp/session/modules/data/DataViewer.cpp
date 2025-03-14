/*
 * DataViewer.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
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
#include <gsl/gsl-lite.hpp>

#include <boost/format.hpp>
#include <boost/algorithm/string/predicate.hpp>
#include <boost/bind/bind.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/SafeConvert.hpp>

#include <core/Log.hpp>
#include <core/Exec.hpp>
#include <core/FileSerializer.hpp>
#include <core/RecursionGuard.hpp>
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
#include <session/SessionSourceDatabase.hpp>

#include <session/prefs/UserPrefs.hpp>

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
using namespace boost::placeholders;

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
 *    working copy as a starting position rather than the original or cached
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
      // (i.e. a search for "walnuts" (inner) is within "walnut" (outer))
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
// can successfully obtain dimensions
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

// CachedFrame represents an object that's currently active in a data viewer window.
struct CachedFrame
{
   CachedFrame(const std::string& env, const std::string& obj, SEXP sexp):
      envName(env),
      objName(obj),
      observedSEXP(sexp)
   {
      if (sexp == nullptr)
         return;

      // cache list of column names
      r::sexp::Protect protect;
      SEXP namesSEXP = R_NilValue;
      r::exec::RFunction("names", sexp).call(&namesSEXP, &protect);
      if (!Rf_isNull(namesSEXP))
      {
         r::sexp::extract(namesSEXP, &colNames);
      }

      // cache number of rows, but only for 'local' data
      // (avoid potentially expensive queries for remote tables)
      nrow = Rf_inherits(sexp, "data.frame") || Rf_inherits(sexp, "matrix")
            ? safeDim(sexp, DIM_ROWS)
            : -1;
      
      // cache number of columns
      ncol = safeDim(sexp, DIM_COLS);
   };
   
   // Class is movable but not copyable
   CachedFrame(CachedFrame&&) = default;
   CachedFrame(const CachedFrame&) = delete;

   // The location of the frame (if we know it)
   std::string envName;
   std::string objName;

   // The number of rows and columns; used to determine whether the shape of the frame has changed
   // (necessitating a full reload of any displayed version of the frame)
   int nrow, ncol;
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
   std::vector<int> workingOrderCols;
   std::vector<std::string> workingOrderDirs;

   // The R underlying object being monitored
   // Protection is required as we introspect the object for changes
   r::sexp::PreservedSEXP observedSEXP;
};

// The set of active frames. Used primarily to check each for changes.
using CachedFrames = std::map<std::string, CachedFrame>;
CachedFrames s_cachedFrames;

std::string viewerCacheDir() 
{
   return module_context::sessionScratchPath().completeChildPath(kViewerCacheDir)
      .getAbsolutePath();
}

SEXP findInNamedEnvir(const std::string& environmentName,
                      const std::string& objectName)
{
   r::sexp::Protect protect;

   // shortcut for unbound environment
   if (environmentName == kNoBoundEnv)
      return nullptr;

   // use the global environment or resolve environment name
   SEXP envirSEXP = R_NilValue;
   if (environmentName.empty() || environmentName == "R_GlobalEnv")
   {
      envirSEXP = R_GlobalEnv;
   }
   else 
   {
      Error error = r::exec::RFunction(".rs.safeAsEnvironment")
            .addParam(environmentName)
            .call(&envirSEXP, &protect);
      
      if (error)
      {
         LOG_ERROR(error);
         return nullptr;
      }
   }

   // if we failed to find an environment by name, return a null SEXP
   bool lookupFailed = envirSEXP == nullptr || envirSEXP == R_NilValue;
   if (lookupFailed)
      return nullptr;
         
   // find the SEXP directly in the environment; return null if unbound
   SEXP objectSEXP = r::sexp::findVar(objectName, envirSEXP);
   if (objectSEXP == R_UnboundValue)
      return nullptr;
   
   return objectSEXP;
}

// data items are used both as the payload for the client event that opens an
// editor viewer tab and as a server response when duplicating that tab's
// contents
json::Object makeDataItem(SEXP dataSEXP, 
                          const std::string& expr,
                          const std::string& caption, 
                          const std::string& objName, const std::string& envName, 
                          const std::string& cacheKey, int preview)
{
   int nrow = safeDim(dataSEXP, DIM_ROWS);
   int ncol = safeDim(dataSEXP, DIM_COLS);

   // fire show data event
   json::Object dataItem;
   dataItem["expression"] = expr;
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
      http::util::urlEncode(cacheKey, true) + "&max_display_columns=" + 
      safe_convert::numberToString(prefs::userPrefs().dataViewerMaxColumns());
   dataItem["preview"] = preview;

   return dataItem;
}

SEXP rs_viewData(SEXP dataSEXP, SEXP exprSEXP, SEXP captionSEXP, SEXP nameSEXP, 
                 SEXP envSEXP, SEXP cacheKeySEXP, SEXP previewSEXP)
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
         throw r::exec::RErrorException(error.getSummary());
      }
      if (dataFrameSEXP != nullptr && dataFrameSEXP != R_NilValue)
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
            r::sexp::safeAsString(exprSEXP),
            r::sexp::safeAsString(captionSEXP), 
            objName, envName, cacheKey, preview);
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
   core::FilePath gridResource = options().rResourcesPath().completeChildPath(path);
   pResponse->setCacheableFile(gridResource, request);
}

json::Value getCols(SEXP dataSEXP,
                    int maxRows,
                    int maxCols)
{
   SEXP colsSEXP = R_NilValue;
   r::sexp::Protect protect;
   json::Value result;

   Error error = r::exec::RFunction(".rs.describeCols")
         .addParam(dataSEXP)
         .addParam(maxRows)
         .addParam(maxCols)
         .addParam(MAX_FACTORS)
         .call(&colsSEXP, &protect);

   if (error || colsSEXP == R_NilValue) 
   {
      json::Object err;
      if (error) 
         err["error"] = error.getSummary();
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

json::Value getColSlice(SEXP dataSEXP,
                         int columnOffset,
                         int maxDisplayColumns)
{
   SEXP colsSEXP = R_NilValue;
   r::sexp::Protect protect;
   json::Value result;

   // DataTables use 0-based indexing, but R uses 1-based indexing, so add 1 to the columnOffset
   int sliceStart = columnOffset + 1;
   int totalCols = safeDim(dataSEXP, DIM_COLS);
   int sliceEnd = columnOffset + maxDisplayColumns < totalCols ? columnOffset + maxDisplayColumns : totalCols;

   Error error = r::exec::RFunction(".rs.describeColSlice")
         .addParam(dataSEXP)
         .addParam(sliceStart)
         .addParam(sliceEnd)
         .call(&colsSEXP, &protect);

   if (error || colsSEXP == R_NilValue) 
   {
      json::Object err;
      if (error) 
         err["error"] = error.getSummary();
      else
         err["error"] = "Failed to retrieve column definitions for the data slice.";
      result = err;
   }
   else 
   {
      r::json::jsonValueFromList(colsSEXP, &result);
   }
   return result;
}

// given an object from which to return data, and a description of the data to
// return via URL-encoded parameters supplied by the DataTables API, returns the
// data requested by the parameters. 
//
// the shape of the API is described here:
// http://datatables.net/manual/server-side
// 
// NB: may throw exceptions! these are expected to be handled by the handlers
// in getGridData, where they will be marshaled to JSON and displayed on the
// client.
json::Object getData(SEXP dataSEXP,
                     int maxRows,
                     int maxCols,
                     const http::Fields& fields)
{
   Error error;
   r::sexp::Protect protect;

   // subset dataset if necessary
   SEXP subsettedDataSEXP = R_NilValue;
   error = r::exec::RFunction(".rs.subsetData")
         .addParam(dataSEXP)
         .addParam(maxRows)
         .addParam(maxCols)
         .call(&subsettedDataSEXP, &protect);

   if (error)
   {
      LOG_ERROR(error);
   }
   else
   {
      dataSEXP = subsettedDataSEXP;
   }

   // read draw parameters from DataTables
   int draw = http::util::fieldValue<int>(fields, "draw", 0);
   int start = http::util::fieldValue<int>(fields, "start", 0);
   int length = http::util::fieldValue<int>(fields, "length", 0);

   std::string search = http::util::urlDecode(
         http::util::fieldValue<std::string>(fields, "search[value]", ""));

   std::string cacheKey = http::util::urlDecode(
         http::util::fieldValue<std::string>(fields, "cache_key", ""));
   
   // Parameters from the client to delimit the column slice to return
   int columnOffset = http::util::fieldValue<int>(fields, "column_offset", 0);
   int maxDisplayColumns = http::util::fieldValue<int>(fields, "max_display_columns", 0);

   // loop through sort columns
   std::vector<int> ordercols;
   std::vector<std::string> orderdirs;
   int orderIdx = 0;
   int ordercol = -1;
   std::string orderdir;
   do
   {
      std::string ordercolstr = "order[" + std::to_string(orderIdx) + "][column]";
      std::string orderdirstr = "order[" + std::to_string(orderIdx) + "][dir]";
      ordercol = http::util::fieldValue<int>(fields, ordercolstr,  -1);
      orderdir = http::util::fieldValue<std::string>(fields, orderdirstr, "asc");

      if (ordercol > 0)
      {
         ordercols.push_back(ordercol + columnOffset);
         orderdirs.push_back(orderdir);
      }

      orderIdx++;
   } while (ordercol > 0);

   int nrow = safeDim(dataSEXP, DIM_ROWS);
   int ncol = safeDim(dataSEXP, DIM_COLS);

   int filteredNRow = 0;

   // extract filters
   std::vector<std::string> filters;
   bool hasFilter = false;

   // fill the initial filters outside of the visible frame
   // unfortunately the code that consumes these filters assumes
   // it's purely index based and needs to be padded out
   for (int i = 0; i < columnOffset; i++)
   {
      std::string emptyStr = "";
      filters.push_back(emptyStr);
   }
   
   for (int i = 1; i <= ncol; i++)
   {
      std::string filterVal = http::util::urlDecode(
            http::util::fieldValue<std::string>(fields,
                  "columns[" + boost::lexical_cast<std::string>(i) + "]"
                  "[search][value]", ""));

      if (!filterVal.empty())
      {
         hasFilter = true;
      }
      filters.push_back(filterVal);
   }

   bool needsTransform = ordercols.size() > 0 || hasFilter || !search.empty();
   bool hasTransform = false;

   // check to see if we have an ordered/filtered view we can build from
   auto cachedFrame = s_cachedFrames.find(cacheKey);
   if (needsTransform)
   {
      if (cachedFrame != s_cachedFrames.end())
      {
         // do we have a previously ordered/filtered view?
         SEXP workingDataSEXP = R_NilValue;
         r::exec::RFunction(".rs.findWorkingData", cacheKey)
            .call(&workingDataSEXP, &protect);
         
         if (workingDataSEXP != R_NilValue)
         {
            if (cachedFrame->second.workingSearch == search &&
                cachedFrame->second.workingFilters == filters && 
                cachedFrame->second.workingOrderDirs == orderdirs &&
                cachedFrame->second.workingOrderCols == ordercols)
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
      transform.addParam("cols", ordercols);     // which column to order on
      transform.addParam("dirs", orderdirs);     // order direction ("asc"/"desc")
      transform.call(&dataSEXP, &protect);
      if (error)
         throw r::exec::RErrorException(error.getSummary());

      // check to see if we've accidentally transformed ourselves into nothing
      // (this shouldn't generally happen without a specific error)
      if (dataSEXP == R_NilValue)
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
         cachedFrame->second.workingOrderDirs = orderdirs;
         cachedFrame->second.workingOrderCols = ordercols;
      }
   }

   // apply new row count if we've transformed the data (or need to)
   filteredNRow = needsTransform || hasTransform
      ? safeDim(dataSEXP, DIM_ROWS)
      : nrow;

   // return the lesser of the rows available and rows requested
   length = std::min(length, filteredNRow - start);

   // DataTables uses 0-based indexing, but R uses 1-based indexing
   start++;

   // extract the portion of the column vector requested by the client
   int numFormattedColumns = ncol - columnOffset < maxDisplayColumns ? ncol - columnOffset : maxDisplayColumns;
   SEXP formattedDataSEXP = Rf_allocVector(VECSXP, numFormattedColumns);
   protect.add(formattedDataSEXP);

   int initialIndex = 0 + columnOffset;
   for (int i = initialIndex; i < initialIndex + numFormattedColumns; i++)
   {
      if (i >= r::sexp::length(dataSEXP))
      {
         throw r::exec::RErrorException(
                  string_utils::sprintf(
                     "Internal error: attempted to access column %i in vector of size %i",
                     i,
                     r::sexp::length(dataSEXP)));
      }
      
      SEXP columnSEXP = VECTOR_ELT(dataSEXP, i);
      if (columnSEXP == nullptr || columnSEXP == R_NilValue)
      {
         throw r::exec::RErrorException(
                  string_utils::sprintf("No data in column %i", i));
      }
      
      SEXP formattedColumnSEXP = R_NilValue;
      r::exec::RFunction formatFx(".rs.formatDataColumn");
      formatFx.addParam(columnSEXP);
      formatFx.addParam(gsl::narrow_cast<int>(start));
      formatFx.addParam(gsl::narrow_cast<int>(length));
      error = formatFx.call(&formattedColumnSEXP, &protect);
      if (error)
         throw r::exec::RErrorException(error.getSummary());
      
      SET_VECTOR_ELT(formattedDataSEXP, i - initialIndex, formattedColumnSEXP);
   }

   // format the row names
   SEXP rownamesSEXP = R_NilValue;
   r::exec::RFunction(".rs.formatRowNames", dataSEXP, start, length)
      .call(&rownamesSEXP, &protect);
   
   // create the result grid as JSON
   
   json::Array data;
   for (int row = 0; row < length; row++)
   {
      // first, handle row names
      json::Array rowData;
      if (rownamesSEXP != nullptr && TYPEOF(rownamesSEXP) == STRSXP)
      {
         SEXP nameSEXP = STRING_ELT(rownamesSEXP, row);
         if (nameSEXP == nullptr)
         {
            rowData.push_back(row + start);
         }
         else if (nameSEXP == NA_STRING)
         {
            rowData.push_back(SPECIAL_CELL_NA);
         }
         else if (r::sexp::length(nameSEXP) == 0)
         {
            rowData.push_back(row + start);
         }
         else
         {
            rowData.push_back(Rf_translateCharUTF8(nameSEXP));
         }
      }
      else
      {
         rowData.push_back(row + start);
      }

      // now, handle remaining columns in formatted data
      for (int col = 0, ncol = r::sexp::length(formattedDataSEXP); col < ncol; col++)
      {
         // NOTE: it is possible for malformed data.frames to have columns with
         // differing number of elements; this is rare in practice but needs
         // to be handled to avoid crashes
         // https://github.com/rstudio/rstudio/issues/9364
         SEXP columnSEXP = VECTOR_ELT(formattedDataSEXP, col);
         if (row >= r::sexp::length(columnSEXP))
         {
            // because R's default print method pads with NAs in this case,
            // we replicate that with our own padded NAs
            rowData.push_back(SPECIAL_CELL_NA);
            continue;
         }
         
         // validate that we have a character vector
         if (columnSEXP == nullptr || TYPEOF(columnSEXP) != STRSXP)
         {
            rowData.push_back("");
            continue;
         }
         
         // we have a valid character vector; access the string element
         // and push back data as appropriate
         SEXP stringSEXP = STRING_ELT(columnSEXP, row);
         if (stringSEXP == nullptr)
         {
            rowData.push_back("");
         }
         else if (stringSEXP == NA_STRING)
         {
            rowData.push_back(SPECIAL_CELL_NA);
         }
         else if (r::sexp::length(stringSEXP) == 0)
         {
            rowData.push_back("");
         }
         else
         {
            rowData.push_back(Rf_translateCharUTF8(stringSEXP));
         }
      }
      
      // all done, add row data
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
            http::util::fieldValue<std::string>(fields, "env", ""));

      std::string objName = http::util::urlDecode(
            http::util::fieldValue<std::string>(fields, "obj", ""));

      std::string cacheKey = http::util::urlDecode(
            http::util::fieldValue<std::string>(fields, "cache_key", ""));

      std::string maxRowsField = http::util::urlDecode(
               http::util::fieldValue<std::string>(fields, "max_rows", ""));

      std::string maxColsField = http::util::urlDecode(
               http::util::fieldValue<std::string>(fields, "max_cols", ""));

      std::string maxDisplayColumnsField = http::util::urlDecode(
               http::util::fieldValue<std::string>(fields, "max_display_columns", ""));

      std::string columnOffsetField = http::util::urlDecode(
               http::util::fieldValue<std::string>(fields, "column_offset", ""));

      std::string show = http::util::fieldValue<std::string>(
               fields, "show", "data");

      int maxRows = safe_convert::stringTo<int>(maxRowsField, -1);
      int maxCols = safe_convert::stringTo<int>(maxColsField, -1);
      int maxDisplayColumns = safe_convert::stringTo<int>(maxDisplayColumnsField, -1);
      int columnOffset = safe_convert::stringTo<int>(columnOffsetField, 0);

      if (objName.empty() && cacheKey.empty()) 
      {
         return Success();
      }

      r::sexp::Protect protect;

      // begin observing if we aren't already
      if (!cacheKey.empty() && envName != kNoBoundEnv)
      {
         SEXP objSEXP = findInNamedEnvir(envName, objName);
         auto it = s_cachedFrames.find(cacheKey);
         if (it == s_cachedFrames.end())
         {
            s_cachedFrames.emplace(cacheKey, CachedFrame(envName, objName, objSEXP));
         }
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

      // can we find it _anywhere_ ?!
      if (dataSEXP == nullptr || dataSEXP == R_UnboundValue || 
          Rf_isNull(dataSEXP) || TYPEOF(dataSEXP) == NILSXP)
      {
         error = r::exec::RFunction(".rs.getAnywhere", objName).call(&dataSEXP, &protect);
         if (error) 
         {
            LOG_ERROR(error);
         }
      }

      // couldn't find the original object
      if (dataSEXP == nullptr || dataSEXP == R_UnboundValue || 
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
            if (columnOffset >= 0 && maxDisplayColumns > 0)
            {
               result = getColSlice(dataSEXP, columnOffset, maxDisplayColumns);
            }
            else
            {
               result = getCols(dataSEXP, maxRows, maxCols);
            }
         }
         else if (show == "data")
         {
            result = getData(dataSEXP, maxRows, maxCols, fields);
         }
      }
   }
   catch (r::exec::RErrorException& e)
   {
      // marshal R errors to the client in the format DataTables (and our own
      // error handling code) expects
      json::Object err;
      err["error"] = e.message();
      result = err;
      status = http::status::BadRequest;
   }
   CATCH_UNEXPECTED_EXCEPTION

   // There are some unprintable ASCII control characters that are written
   // verbatim by json::write, but that won't parse in most Javascript JSON
   // parsing implementations, even if contained in a string literal. Scan the
   // output data for these characters and replace them with spaces. Escaping
   // is another option here for some character ranges but since (a) these are
   // unprintable and (b) some characters are invalid *even if escaped* e.g.
   // \v, there's little to be gained here in trying to marshal them to the
   // viewer.
   std::string output = result.write();
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
   pResponse->setContentType("application/json");
   pResponse->setBody(output);

   return Success();
}

Error removeRCachedData(const std::string& cacheKey);

void removeRCachedDataCallback(const std::string& cacheKey)
{
   Error error = removeRCachedData(cacheKey);
   if (error)
      LOG_ERROR(error);
}

Error removeRCachedData(const std::string& cacheKey)
{
   if (core::thread::isMainThread())
   {
      // remove cache env object and backing file
      return r::exec::RFunction(".rs.removeCachedData", cacheKey,
            viewerCacheDir()).call();
   }
   else
   {
      module_context::executeOnMainThread(boost::bind(removeRCachedDataCallback, cacheKey));
      return Success();
   }
}

Error removeCacheKey(const std::string& cacheKey)
{
   // remove from watchlist
   auto it = s_cachedFrames.find(cacheKey);
   if (it != s_cachedFrames.end())
      s_cachedFrames.erase(it);
   
    return removeRCachedData(cacheKey);
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

   if (!core::thread::isMainThread())
      return;

   // unlikely that data will change outside of a REPL
   if (source != module_context::ChangeSourceREPL) 
      return;

   r::sexp::Protect protect;
   for (auto i = s_cachedFrames.begin(); i != s_cachedFrames.end(); i++)
   {
      SEXP sexp = findInNamedEnvir(i->second.envName, i->second.objName);
      if (sexp == nullptr)
         continue;
      
      // create a new frame object to capture the new state of the frame
      CachedFrame newFrame(i->second.envName, i->second.objName, sexp);
         
      // clear working data for the object
      r::exec::RFunction(".rs.removeWorkingData", i->first).call();
         
      // check for changes in the SEXP itself
      SEXP observedSEXP = i->second.observedSEXP.get();
      bool sexpChanged = sexp != observedSEXP;
      
      // it's possible that the object was mutated in place;
      // attempt to detect this as well
      bool typeChanged = false;
      if (observedSEXP != nullptr)
      {
         SEXP oldClass = Rf_getAttrib(observedSEXP, R_ClassSymbol);
         SEXP newClass = Rf_getAttrib(sexp, R_ClassSymbol);
         typeChanged = !R_compute_identical(oldClass, newClass, 0);
      }

      bool structureChanged =
            i->second.nrow != newFrame.nrow ||
            i->second.ncol != newFrame.ncol ||
            i->second.colNames != newFrame.colNames;

      if (sexpChanged || typeChanged || structureChanged)
      {
         // replace cached copy
         r::exec::RFunction(".rs.assignCachedData")
               .addParam(i->first)
               .addParam(sexp)
               .addParam(i->second.objName)
               .call();

         // figure out the object classes for this thing
         std::vector<std::string> objectClass;
         Error error = r::exec::RFunction("base:::class")
               .addParam(sexp)
               .call(&objectClass);

         if (error)
            LOG_ERROR(error);

         // emit client event
         json::Object changed;
         changed["cache_key"] = i->first;
         changed["type_changed"] = typeChanged;
         changed["structure_changed"] = structureChanged;
         changed["object_exists"] = true;
         changed["object_class"] = json::toJsonArray(objectClass);
         ClientEvent event(client_events::kDataViewChanged, changed);
         module_context::enqueClientEvent(event);

         // replace old frame with new
         s_cachedFrames.emplace(i->first, std::move(newFrame));
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
   error = cacheDir.changeFileMode(core::FileMode::USER_READ_WRITE_EXECUTE);
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
   for (boost::shared_ptr<source_database::SourceDocument> pDoc : docs)
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
      Error error = cache.getChildren(cacheFiles);
      if (error)
      {
         LOG_ERROR(error);
         return;
      }
   }

   std::vector<std::string> cacheKeys;
   for (const FilePath& cacheFile : cacheFiles)
   {
      cacheKeys.push_back(cacheFile.getStem());
   }

   // sort each set of keys (so we can diff the sets below)
   std::sort(sourceKeys.begin(), sourceKeys.end());
   std::sort(cacheKeys.begin(), cacheKeys.end());

   std::vector<std::string> orphanKeys;
   std::set_difference(cacheKeys.begin(), cacheKeys.end(),
                       sourceKeys.begin(), sourceKeys.end(),
                       std::back_inserter(orphanKeys));

   // remove each key no longer bound to a source file
   for (const std::string& orphanKey : orphanKeys)
   {
      error = cache.completePath(orphanKey + ".Rdata").removeIfExists();
      if (error)
         LOG_ERROR(error);
   }
}

} // anonymous namespace
   
Error initialize()
{
   using namespace module_context;

   // register viewData method
   RS_REGISTER_CALL_METHOD(rs_viewData);

   source_database::events().onDocPendingRemove.connect(onDocPendingRemove);

   module_context::events().onShutdown.connect(onShutdown);
   module_context::events().onDetectChanges.connect(onDetectChanges);
   module_context::events().onClientInit.connect(onClientInit);
   module_context::events().onDeferredInit.connect(onDeferredInit);
   addSuspendHandler(SuspendHandler(onSuspend, onResume));

   using boost::bind;
   using namespace rstudio::r::function_hook;
   using namespace session::module_context;
   ExecBlock initBlock;
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

