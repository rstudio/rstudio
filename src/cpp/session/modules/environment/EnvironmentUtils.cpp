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

json::Value valueOfVar(SEXP var)
{
   std::string value;
   Error error = r::exec::RFunction(".rs.valueAsStr",
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

} // anonymous namespace

json::Object varToJson(const r::sexp::Variable& var)
{
   json::Object varJson;
   varJson["name"] = var.first;
   SEXP varSEXP = var.second;
   varJson["type"] = r::sexp::typeAsString(varSEXP);
   varJson["len"] = r::sexp::length(varSEXP);
   varJson["value"] = valueOfVar(varSEXP);
   return varJson;
}

} // namespace environment
} // namespace modules
} // namespace session
