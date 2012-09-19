/*
 * SessionData.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include "SessionData.hpp"

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

#include "SessionContentUrls.hpp"

using namespace core;

namespace session {
namespace modules { 
namespace data {

namespace {   
     
void appendTag(std::string* pHTML,
               const std::string& text,
               const std::string& tag,
               const std::string& className = std::string())
{
   std::string classAttrib;
   if (!className.empty())
      classAttrib = " class=\"" + className + "\"";

   boost::format fmt("<%1%%2%>%3%</%1%>");
   pHTML->append(boost::str(fmt % tag %
                                  classAttrib %
                                  string_utils::textToHtml(text)));
}

void appendTD(std::string* pHTML,
              const std::string& text,
              const std::string& className = std::string())
{
   appendTag(pHTML, text, "td", className);
}

void appendTH(std::string* pHTML, const std::string& text)
{
   appendTag(pHTML, text, "th");
}


SEXP rs_viewData(SEXP dataSEXP, SEXP titleSEXP)
{    
   try
   {
      // validate title
      if (!Rf_isString(titleSEXP) || Rf_length(titleSEXP) != 1)
         throw r::exec::RErrorException("invalid title argument");
           
      // validate data
      if (TYPEOF(dataSEXP) != VECSXP)
         throw r::exec::RErrorException("invalid data argument (not a list)");
      
      // validate names (View ensures that length(names) == length(data))
      SEXP namesSEXP = Rf_getAttrib(dataSEXP, R_NamesSymbol);
      if (TYPEOF(namesSEXP) != STRSXP || 
          Rf_length(namesSEXP) != Rf_length(dataSEXP))
      {
         throw r::exec::RErrorException(
                              "invalid data argument (names not specified)");
      }

      // get column count
      int columnCount = r::sexp::length(dataSEXP);

      // calculate columns to display
      const int kMaxColumns = 100;
      int displayedColumns = std::min(columnCount, kMaxColumns);

      // extract title and column names
      std::string title = r::sexp::asString(titleSEXP);
      std::vector<std::string> columnNames;
      Error error = r::sexp::extract(namesSEXP, &columnNames);
      if (error)
         throw r::exec::RErrorException("invalid names: " +
                                        error.code().message());

      // truncate columns names to displayedColumns
      columnNames.resize(displayedColumns);

      // get column lenghts and then calculate # of rows based on the maximum #
      // of elements in single column (technically R can pass columns which have
      // a disparate # of rows to this method)
      int rowCount = 0;
      std::vector<int> columnLengths;
      for (int i=0; i<displayedColumns; i++)
      {
          // get the column and record its length (updating rowCount)
          SEXP columnSEXP = VECTOR_ELT(dataSEXP, i);
          int columnLength = r::sexp::length(columnSEXP);
          columnLengths.push_back(columnLength);
          rowCount = std::max(columnLength, rowCount);

          // validate data type (R converts all inbound vectors to REAL or STR)
          if (TYPEOF(columnSEXP) != REALSXP && TYPEOF(columnSEXP) != STRSXP)
          {
             throw r::exec::RErrorException("Unexpected type for column: "
                                            + r::sexp::typeAsString(columnSEXP));
          }
      }

      // calculate rows to display
      const int kMaxRows = 1000;
      int displayedRows = std::min(rowCount, kMaxRows);

      // format the data for presentation
      r::sexp::Protect rProtect;
      SEXP formattedDataSEXP = Rf_allocVector(VECSXP, displayedColumns);
      rProtect.add(formattedDataSEXP);
      for (int i=0; i<displayedColumns; i++)
      {
         SEXP columnSEXP = VECTOR_ELT(dataSEXP, i);
         SEXP formattedColumnSEXP;
         r::exec::RFunction formatFx(".rs.formatDataColumn");
         formatFx.addParam(columnSEXP);
         formatFx.addParam(displayedRows);
         Error error = formatFx.call(&formattedColumnSEXP, &rProtect);
         if (error)
            throw r::exec::RErrorException(error.summary());
         SET_VECTOR_ELT(formattedDataSEXP, i, formattedColumnSEXP);
       }

      // write html header
      boost::format headerFmt(
         "<html>\n"
         "  <head>\n"
         "     <title>%1%</title>\n"
         "     <meta charset=\"utf-8\"/>\n"
         "     <link rel=\"stylesheet\" type=\"text/css\" href=\"css/data.css\"/>\n"
         "  </head>\n"
         "  <body>\n");
      std::string html = boost::str(headerFmt % title);

      // output begin table & header
      html += "<table>\n";
      html += "<thead><tr>\n";
      html += "<td id=\"origin\">&nbsp;</td>"; // above row numbers
      std::for_each(columnNames.begin(),
                    columnNames.end(),
                    boost::bind(appendTH, &html, _1));
      html += "\n</tr></thead>\n";

      html += "<tbody>\n";
      // output rows
      for (int row=0; row<displayedRows; row++)
      {
         html += "<tr>\n";

         // row number
         appendTD(&html, boost::lexical_cast<std::string>(row+1), "rn");

         // output a data element from each column where this row is available
         for (int col=0; col<Rf_length(formattedDataSEXP); col++)
         {
            if (columnLengths[col] > row)
            {
               SEXP columnSEXP = VECTOR_ELT(formattedDataSEXP, col);
               SEXP stringSEXP = STRING_ELT(columnSEXP, row);
               if (stringSEXP != NULL &&
                   stringSEXP != NA_STRING &&
                   r::sexp::length(stringSEXP) > 0)
               {
                  std::string text(Rf_translateChar(stringSEXP));
                  appendTD(&html, text);
               }
               else
               {
                  html += "<td>&nbsp;</td>";
               }
            }
            else
            {
               html += "<td>&nbsp;</td>";
            }
         }

         html += "\n</tr>\n";
      }
      html += "</tbody>\n";

      // append table footer
      html += "\n</table>\n";

      // append document footer
      html += "</body></html>\n";


      // compute variables based on presence of row.names
      int variables = columnCount;
      if (columnNames.size() > 0 && columnNames[0] == "row.names")
         variables--;

      // fire show data event
      json::Object dataItem;
      dataItem["title"] = title;
      dataItem["totalObservations"] = rowCount;
      dataItem["displayedObservations"] = displayedRows;
      dataItem["variables"] = variables;
      dataItem["displayedVariables"] = displayedColumns;
      dataItem["contentUrl"] = content_urls::provision(title, html, ".htm");
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
      (bind(sourceModuleRFile, "SessionData.R"));
   
   return initBlock.execute();
}


} // namespace data
} // namespace modules
} // namesapce session

