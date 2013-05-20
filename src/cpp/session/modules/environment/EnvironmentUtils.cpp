/*
 * EnvironmentUtils.cpp
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

#include "EnvironmentUtils.hpp"

#include <r/RExec.hpp>

using namespace core;

namespace session {
namespace modules {
namespace environment {
namespace {

json::Value classOfVar(SEXP var)
{
   std::string value;
   Error error = r::exec::RFunction(".rs.getSingleClass",
                                    var).call(&value);
   if (error)
   {
      LOG_ERROR(error);
      return json::Value(); // return null
   }
   else
   {
      return value;
   }
}

json::Value valueOfVar(SEXP var)
{
   std::string value;
   Error error = r::exec::RFunction(".rs.valueAsString",
                                    var).call(&value);
   if (error)
   {
      LOG_ERROR(error);
      return json::Value(); // return null
   }
   else
   {
      return value;
   }
}

json::Value descriptionOfVar(SEXP var)
{
   std::string value;
   Error error = r::exec::RFunction(".rs.valueDescription",
                                    var).call(&value);
   if (error)
   {
      LOG_ERROR(error);
      return json::Value(); // return null
   }
   else
   {
      return value;
   }
}

json::Array contentsOfVar(SEXP var)
{
   std::vector<std::string> value;
   Error error = r::exec::RFunction(".rs.valueContents", var).call(&value);
   if (error)
   {
      LOG_ERROR(error);
      return json::Array(); // return null
   }
   else
   {
      return json::toJsonArray(value);
   }
}

} // anonymous namespace

json::Object varToJson(const r::sexp::Variable& var)
{
   json::Object varJson;
   varJson["name"] = var.first;
   SEXP varSEXP = var.second;

   // get R alias to object and get its type and lengt
   //
   // NOTE: check for isLanguage is a temporary fix for error messages
   // that were printed at the console for a <- bquote(test()) -- this
   // was the result of errors being thrown from the .rs.valueDescription, etc.
   // calls above used to probe for object info.
   // the practical impact of this workaround is that immediately after
   // assignment language expressions show up as "(unknown)" but then are
   // correctly displayed in refreshed listings of the workspace.

   if ((varSEXP != R_UnboundValue) && !r::sexp::isLanguage(varSEXP))
   {
      json::Value varClass = classOfVar(varSEXP);
      varJson["type"] = varClass;
      varJson["value"] = valueOfVar(varSEXP);
      varJson["description"] = descriptionOfVar(varSEXP);
      if (varClass == "data.frame"
          || varClass == "data.table"
          || varClass == "list"
          || varClass == "cast_df")
      {
         varJson["contents"] = contentsOfVar(varSEXP);
      }
      else
      {
         varJson["contents"] = json::Array();
      }
   }
   else
   {
      if (r::sexp::isLanguage((varSEXP)))
      {
         varJson["type"] = std::string("language");
      }
      else
      {
         varJson["type"] = std::string("unknown");
      }
      varJson["value"] = std::string("<unknown>");
      varJson["description"] = std::string("");
      varJson["contents"] = json::Array();
      }
   return varJson;
}

} // namespace environment
} // namespace modules
} // namespace session
