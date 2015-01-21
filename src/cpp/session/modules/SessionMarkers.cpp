/*
 * SessionMarkers.cpp
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

#include "SessionMarkers.hpp"

#include <core/Exec.hpp>
#include <core/Settings.hpp>

#include <r/RSexp.hpp>
#include <r/RRoutines.hpp>

#include <session/SessionModuleContext.hpp>

using namespace core ;

namespace session {
namespace modules { 
namespace markers {

namespace {

class MarkersState : boost::noncopyable
{
public:
   MarkersState() : visible_(false)
   {
   }

   bool visible() const { return visible_; }
   void setVisible(bool visible) { visible_ = visible; }

public:
   Error readFromJson(const json::Object& asJson)
   {
      Error error = json::readObject(asJson,
                                     "visible", &visible_);
      if (error)
         return error;

      return Success();
   }

   json::Object asJson()
   {
      json::Object obj;
      obj["visible"] = visible_;
      return obj;
   }

private:
   bool visible_;
};

MarkersState& markersState()
{
   static MarkersState instance;
   return instance;
}


// IN: Array<String> paths, String targetPath
Error clearAllMarkers(const core::json::JsonRpcRequest& request,
                      json::JsonRpcResponse* pResponse)
{
   markersState().setVisible(false);
   return Success();
}

SEXP rs_showMarkers()
{
   markersState().setVisible(true);

   ClientEvent event(client_events::kShowMarkers);
   module_context::enqueClientEvent(event);

   return R_NilValue;
}

void onSuspend(core::Settings* pSettings)
{
   std::ostringstream os;
   json::write(markersState().asJson(), os);
   pSettings->set("markers-state", os.str());
}

void onResume(const core::Settings& settings)
{
   std::string state = settings.get("markers-state");
   if (!state.empty())
   {
      json::Value stateJson;
      if (!json::parse(state, &stateJson))
      {
         LOG_WARNING_MESSAGE("invalid find results state json");
         return;
      }

      Error error = markersState().readFromJson(stateJson.get_obj());
      if (error)
         LOG_ERROR(error);
   }
}

} // anonymous namespace

json::Value markersStateAsJson()
{
   return markersState().asJson();
}

Error initialize()
{
   // suspend/resume handler to save state
   using namespace module_context;
   addSuspendHandler(SuspendHandler(bind(onSuspend, _2), onResume));

   // register rs_showMarkers with R
   RS_REGISTER_CALL_METHOD(rs_showMarkers, 0);

   // complete initialization
   using boost::bind;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "clear_all_markers", clearAllMarkers))
      (bind(sourceModuleRFile, "SessionMarkers.R"));
   return initBlock.execute();

}
   
   
} // namespace markers
} // namespace modules
} // namesapce session

