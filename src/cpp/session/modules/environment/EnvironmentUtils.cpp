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

// the string sent to the client when we're unable to get the display value
// of a variable
const char UNKNOWN_VALUE[] = "<unknown>";

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
   Error error = r::exec::RFunction(
            isUnevaluatedPromise(var) ?
                  ".rs.promiseDescription" :
                  ".rs.valueDescription",
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

// a variable is an unevaluated promise if its promise value is still unbound
bool isUnevaluatedPromise (SEXP var)
{
   return (TYPEOF(var) == PROMSXP) && (PRVALUE(var) == R_UnboundValue);
}

// convert a language variable to a value. language variables are special in
// that we can't allow them to be evaluated (doing so may e.g. trigger early
// evaluation of a call), so instead we pass the name of the variable and a
// reference to its environment so the lookup only happens in the context of
// the R session.
json::Value languageVarToJson(SEXP env, std::string objectName)
{
   std::string value;
   Error error = r::exec::RFunction(".rs.languageDescription",
               env, objectName)
               .call(&value);
   if (error)
   {
      LOG_ERROR(error);
      return UNKNOWN_VALUE;
   }
   else
   {
      return value;
   }
}

json::Object varToJson(SEXP env, const r::sexp::Variable& var)
{
   json::Object varJson;
   varJson["name"] = var.first;
   SEXP varSEXP = var.second;

   // is this a type of object for which we can get something that looks like
   // a value? if so, get the value appropriate to the object's class.
   if ((varSEXP != R_UnboundValue) &&
       (varSEXP != R_MissingArg) &&
       !r::sexp::isLanguage(varSEXP) &&
       !isUnevaluatedPromise(varSEXP) &&
       TYPEOF(varSEXP) != SYMSXP)
   {
      json::Value varClass = classOfVar(varSEXP);
      varJson["type"] = varClass;
      varJson["value"] = valueOfVar(varSEXP);
      varJson["description"] = descriptionOfVar(varSEXP);
      varJson["length"] = r::sexp::length(varSEXP);
      if (varClass == "data.frame" ||
          varClass == "data.table" ||
          varClass == "list" ||
          varClass == "cast_df" ||
          varClass == "xts" ||
          Rf_isS4(varSEXP))
      {
         varJson["contents"] = contentsOfVar(varSEXP);
      }
      else
      {
         varJson["contents"] = json::Array();
      }
   }
   // this is not a type of object for which we can get a value; describe
   // what we can and stub out the rest.
   else
   {
      if (r::sexp::isLanguage(varSEXP) ||
          TYPEOF(varSEXP) == SYMSXP)
      {
         varJson["type"] = std::string("language");
         varJson["value"] = languageVarToJson(env, var.first);
      }
      else if (isUnevaluatedPromise(varSEXP))
      {
         varJson["type"] = std::string("promise");
         varJson["value"] = descriptionOfVar(varSEXP);
      }
      else
      {
         varJson["type"] = std::string("unknown");
         if (varSEXP == R_MissingArg)
         {
            varJson["value"] = descriptionOfVar(varSEXP);
         }
         else
         {
            varJson["value"] = UNKNOWN_VALUE;
         }
      }
      varJson["description"] = std::string("");
      varJson["contents"] = json::Array();
      varJson["length"] = 0;
   }
   return varJson;
}

} // namespace environment
} // namespace modules
} // namespace session
