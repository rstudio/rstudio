/*
 * SessionProjectTemplate.hpp
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

#ifndef SESSION_PROJECT_TEMPLATE_HPP
#define SESSION_PROJECT_TEMPLATE_HPP

#include <core/json/Json.hpp>

#include <boost/foreach.hpp>

namespace rstudio {
namespace core {
   class Error;
}
}

namespace rstudio {
namespace session {
namespace modules {
namespace projects {
namespace templates {

#define kProjectTemplateWidgetTypeCheckBox  "checkbox"
#define kProjectTemplateWidgetTypeSelectBox "selectbox"
#define kProjectTemplateWidgetTypeTextInput "textinput"
#define kProjectTemplateWidgetTypeFileInput "fileinput"

struct ProjectTemplateWidgetDescription
{
   std::string parameter;
   std::string type;
   std::string label;
   std::vector<std::string> fields;

   core::json::Value toJson() const
   {
      core::json::Object object;

      object["parameter"] = parameter;
      object["type"]      = type;
      object["label"]     = label;

      object["fields"]    = core::json::toJsonArray(fields);

      return object;
   }

   static ProjectTemplateWidgetDescription fromJson(core::json::Object& object)
   {
      ProjectTemplateWidgetDescription ptwd;

      ptwd.parameter = object["parameter"].get_str();
      ptwd.type      = object["type"].get_str();
      ptwd.label     = object["label"].get_str();
      
      core::json::fillVectorString(
            object["fields"].get_array(),
            &(ptwd.fields));

      return ptwd;
   }
};

struct ProjectTemplateDescription
{
   std::string package;
   std::string binding;
   std::string title;
   std::string subtitle;
   std::string caption;
   std::vector<ProjectTemplateWidgetDescription> widgets;
   
   core::json::Value toJson() const
   {
      core::json::Object object;
      
      object["package"]  = package;
      object["binding"]  = binding;
      object["title"]    = title;
      object["subtitle"] = subtitle;
      object["caption"]  = caption;
      
      core::json::Array widgetsJson;
      BOOST_FOREACH(const ProjectTemplateWidgetDescription& widgetDescription, widgets)
      {
         widgetsJson.push_back(widgetDescription.toJson());
      }
      object["widgets"] = widgetsJson;
      
      return object;
   }
   
   static ProjectTemplateDescription fromJson(core::json::Object& object)
   {
      ProjectTemplateDescription ptd;
      
      ptd.package  = object["package"].get_str();
      ptd.binding  = object["binding"].get_str();
      ptd.title    = object["title"].get_str();
      ptd.subtitle = object["subtitle"].get_str();
      ptd.caption  = object["caption"].get_str();
      
      core::json::Array widgetsJson = object["widgets"].get_array();
      BOOST_FOREACH(core::json::Value& widgetJson, widgetsJson)
      {
         ProjectTemplateWidgetDescription description =
               ProjectTemplateWidgetDescription::fromJson(widgetJson.get_obj());
         ptd.widgets.push_back(description);
      }
      
      return ptd;
   }
};

core::Error initialize();

} // end namespace templates
} // end namespace projects
} // end namespace modules
} // end namespace session
} // end namespace rstudio

#endif /* SESSION_PROJECT_TEMPLATE_HPP */
