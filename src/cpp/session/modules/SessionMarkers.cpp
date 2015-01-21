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

#include <boost/foreach.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <core/Exec.hpp>
#include <core/Settings.hpp>

#include <r/RSexp.hpp>
#include <r/RRoutines.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core ;

namespace rstudio {
namespace session {

namespace {

json::Object sourceMarkerSetAsJson(const module_context::SourceMarkerSet& set)
{
   using namespace module_context;
   json::Object jsonSet;
   jsonSet["name"] = set.name;
   if (set.basePath.empty())
   {
      jsonSet["base_path"] = json::Value();
   }
   else
   {
      std::string basePath = createAliasedPath(set.basePath);
      // ensure that the base_path ends with "/" so that markers don't
      // display the path
      if (!boost::algorithm::ends_with(basePath, "/"))
         basePath.append("/");
      jsonSet["base_path"] = basePath;
   }
   jsonSet["markers"] = sourceMarkersAsJson(set.markers);
   jsonSet["auto_select"] = static_cast<int>(set.autoSelect);
   return jsonSet;
}


class MarkersState : boost::noncopyable
{
public:
   MarkersState() : visible_(false)
   {
   }

   bool visible() const { return visible_; }
   void setVisible(bool visible) { visible_ = visible; }

   void setActiveSourceMarkers(const module_context::SourceMarkerSet& markerSet)
   {
      activeSet_ = markerSet.name;
      markerSets_[markerSet.name] = markerSet;
   }

public:
   Error readFromJson(const json::Object& asJson)
   {
      bool visible;
      std::string activeSet;
      json::Object setsJson;
      Error error = json::readObject(asJson,
                                     "visible", &visible,
                                     "active_set", &activeSet,
                                     "sets", &setsJson);
      if (error)
         return error;

      MarkerSets markerSets;

      BOOST_FOREACH(const json::Object::value_type& setJson, setsJson)
      {
         std::string name = setJson.first;
         json::Value markerSetJson = setJson.second;
         if (json::isType<json::Object>(markerSetJson))
         {
            std::string basePath;
            int autoSelect;
            json::Array markersJson;
            Error error = json::readObject(markerSetJson.get_obj(),
                                           "base_path", &basePath,
                                           "markers", &markersJson,
                                           "auto_select", &autoSelect);
            if (error)
            {
               LOG_ERROR(error);
               continue;
            }
            std::vector<module_context::SourceMarker> markers;
            BOOST_FOREACH(json::Value markerJson, markersJson)
            {
               if (json::isType<json::Object>(markerJson))
               {
                  int type;
                  std::string path;
                  int line, column;
                  std::string message;
                  bool showErrorList;
                  Error error = json::readObject(
                     markerJson.get_obj(),
                     "type", &type,
                     "path", &path,
                     "line", &line,
                     "column", &column,
                     "message", &message,
                     "show_error_list", &showErrorList);
                  if (error)
                  {
                     LOG_ERROR(error);
                     continue;
                  }

                  module_context::SourceMarker marker(
                      (module_context::SourceMarker::Type)type,
                      module_context::resolveAliasedPath(path),
                      line,
                      column,
                      message,
                      showErrorList);

                  markers.push_back(marker);
               }
            }

            using namespace module_context;
            FilePath base = !basePath.empty() ?
                       resolveAliasedPath(basePath) :
                       FilePath();

            markerSets[name] = SourceMarkerSet(
                                    name, base, markers,
                                    (SourceMarkerSet::AutoSelect)autoSelect);
         }


      }

      visible_ = visible;
      activeSet_ = activeSet;
      markerSets_ = markerSets;

      return Success();
   }

   json::Object asJson()
   {
      json::Object obj;
      obj["visible"] = visible_;
      obj["active_set"] = activeSet_;
      json::Object setsJson;
      BOOST_FOREACH(const MarkerSets::value_type& set, markerSets_)
      {
         setsJson[set.first] = sourceMarkerSetAsJson(set.second);
      }
      obj["sets"] = setsJson;

      return obj;
   }

private:
   bool visible_;
   std::string activeSet_;
   typedef std::map<std::string,module_context::SourceMarkerSet> MarkerSets;
   MarkerSets markerSets_;
};

MarkersState& markersState()
{
   static MarkersState instance;
   return instance;
}

} // anonymous namespace

namespace module_context {

void showSourceMarkers(const SourceMarkerSet& markerSet)
{
   markersState().setVisible(true);

   markersState().setActiveSourceMarkers(markerSet);

   ClientEvent event(client_events::kShowMarkers,
                     sourceMarkerSetAsJson(markerSet));
   module_context::enqueClientEvent(event);
}

} // namespace module_context


namespace modules { 
namespace markers {

namespace {

Error markersTabClosed(const core::json::JsonRpcRequest& request,
                       json::JsonRpcResponse* pResponse)
{
   markersState().setVisible(false);
   return Success();
}

void onSuspend(core::Settings* pSettings)
{
   std::ostringstream os;
   json::write(markersState().asJson(), os);
   pSettings->set("source-markers-state", os.str());
}

void onResume(const core::Settings& settings)
{
   std::string state = settings.get("source-markers-state");
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


SEXP rs_showMarkers()
{
   using namespace module_context;
   std::vector<SourceMarker> markers;
   markers.push_back(SourceMarker(SourceMarker::Error,
                                  resolveAliasedPath("~/woozy11.cpp"),
                                  10,
                                  1,
                                  "you did this totally wrong",
                                  true));

   SourceMarkerSet markerSet("cpp-errors",
                             resolveAliasedPath("~"),
                             markers,
                             SourceMarkerSet::AutoSelectFirst);

   showSourceMarkers(markerSet);

   return R_NilValue;
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

   RS_REGISTER_CALL_METHOD(rs_showMarkers, 0);

   // complete initialization
   using boost::bind;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "markers_tab_closed", markersTabClosed))
      (bind(sourceModuleRFile, "SessionMarkers.R"));
   return initBlock.execute();

}
   
   
} // namespace markers
} // namespace modules
} // namesapce session
} // namespace rstudio

