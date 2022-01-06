/*
 * SessionClipboard.cpp
 *
 * Copyright (C) 2022 by RStudio, PBC
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

#include "SessionClipboard.hpp"

#include <shared_core/json/Json.hpp>

#include <r/RRoutines.hpp>
#include <r/RSexp.hpp>

#include <session/SessionOptions.hpp>
#include <session/SessionClientEvent.hpp>
#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace clipboard {

void clipboardSetText(const std::string& text)
{
   // not supported on RStudio Server
   if (options().programMode() == kSessionProgramModeServer)
      return;
   
   json::Object payload;
   payload["type"] = "set";
   payload["text"] = text;
   
   ClientEvent event(client_events::kClipboardAction, payload);
   module_context::enqueClientEvent(event);
}

SEXP rs_clipboardSetText(SEXP textSEXP)
{
   std::string text = r::sexp::asString(textSEXP);
   clipboardSetText(text);
   return R_NilValue;
}

Error initialize()
{
   RS_REGISTER_CALL_METHOD(rs_clipboardSetText);
   return Success();
}

} // end namespace clipboard
} // end namespace modules
} // end namespace session
} // end namespace rstudio
