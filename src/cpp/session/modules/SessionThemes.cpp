/*
 * SessionThemes.cpp
 *
 * Copyright (C) 2018 by RStudio, Inc.
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

#include "SessionThemes.hpp"

#include <boost/bind.hpp>

#include <core/Error.hpp>
#include <core/Exec.hpp>

#include <r/RSexp.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace themes {

namespace {

Error convertTheme(const json::JsonRpcRequest& request,
                   json::JsonRpcResponse* pResponse)
{
   // TODO: process arguments, call r method, populate response.
   return Success();
}

Error addTheme(const json::JsonRpcRequest& request,
               json::JsonRpcResponse* pResponse)
{
   // TODO: process arguments, call r method, populate response.
   return Success();
}

Error applyTheme(const json::JsonRpcRequest& request,
                 json::JsonRpcResponse* pResponse)
{
   // TODO: process arguments, call r method, populate response.
   return Success();
}

Error removeTheme(const json::JsonRpcRequest& request,
                  json::JsonRpcResponse* pResponse)
{
   // TODO: process arguments, call r method, populate response.
   return Success();
}

} // anonymous namespace

Error initialize()
{
   using boost::bind;

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "theme_convert", convertTheme))
      (bind(registerRpcMethod, "theme_add", addTheme))
      (bind(registerRpcMethod, "theme_apply", applyTheme))
      (bind(registerRpcMethod, "theme_remove", removeTheme))
      (bind(sourceModuleRFile, "SessionThemes.R"));
}

} // namespace themes
} // namespace modules
} // namespace session
} // namespace rstudio
