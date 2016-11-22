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

#include <core/Base64.hpp>
#include <core/FileSerializer.hpp>
#include <core/StringUtils.hpp>
#include <core/json/Json.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/text/CsvParser.hpp>

#include <boost/system/error_code.hpp>
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

static const char* const kWidgetTypes[] = {
   kProjectTemplateWidgetTypeCheckBox,
   kProjectTemplateWidgetTypeSelectBox,
   kProjectTemplateWidgetTypeTextInput,
   kProjectTemplateWidgetTypeFileInput
};

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
      
      core::Error error = core::json::readObject(
               object,
               "parameter", &ptwd.parameter,
               "type",      &ptwd.type,
               "label",     &ptwd.label);
      
      if (error)
         LOG_ERROR(error);

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
   std::string icon;
   std::vector<ProjectTemplateWidgetDescription> widgets;
   
   static core::Error parseWidget(const std::string& widget, std::string* pWidgetType)
   {
      std::string widgetLower = core::string_utils::toLower(widget);
      for (std::size_t i = 0, n = sizeof(kWidgetTypes); i < n; ++i)
      {
         const char* widgetType = kWidgetTypes[i];
         if (widgetLower == widgetType)
         {
            pWidgetType->assign(widgetType);
            return core::Success();
         }
      }
      
      return core::systemError(
               boost::system::errc::protocol_error,
               ERROR_LOCATION);
   }
   
   static core::Error parseFields(const std::string& fields, std::vector<std::string>* pFields)
   {
      std::vector<std::string> parsedFields;
      core::text::parseCsvLine(
               fields.begin(),
               fields.end(),
               true,
               &parsedFields);
      
      if (parsedFields.empty())
         return core::systemError(boost::system::errc::protocol_error, ERROR_LOCATION);
      
      for (std::size_t i = 0, n = parsedFields.size(); i < n; ++i)
         parsedFields[i] = core::string_utils::trimWhitespace(parsedFields[i]);
      
      *pFields = parsedFields;
      return core::Success();
   }
   
   template <typename T>
   static core::Error populate(const core::FilePath& resourcePath,
                               const T& map,
                               ProjectTemplateDescription* pDescription)
   {
      ProjectTemplateWidgetDescription widget;
      for (typename T::const_iterator it = map.begin();
           it != map.end();
           ++it)
      {
         const std::string& key   = it->first;
         const std::string& value = it->second;
         
         // populate primary keys
         if (key == "Binding")
            pDescription->binding = value;
         else if (key == "Title")
            pDescription->title = value;
         else if (key == "Subtitle")
            pDescription->subtitle = value;
         else if (key == "Caption")
            pDescription->caption = value;
         else if (key == "Icon")
         {
            // read icon file from disk
            core::FilePath iconPath = resourcePath.parent().complete(value);
            
            // encode file contents as base64
            std::string encoded;
            core::Error error = core::base64::encode(iconPath, &encoded);
            if (error)
               LOG_ERROR(error);
            
            // send up to client as base64-encoded blob
            pDescription->icon = encoded;
         }
         
         // populate widget
         else if (key == "Parameter")
            widget.parameter = value;
         else if (key == "Label")
            widget.label = value;
         else if (key == "Widget")
         {
            core::Error error = parseWidget(value, &widget.type);
            if (error)
               return error;
         }
         else if (key == "Fields")
         {
            core::Error error = parseFields(value, &widget.fields);
            if (error)
               return error;
         }
      }
      
      // if we discovered a widget here, add it to the description
      if (!widget.parameter.empty())
         pDescription->widgets.push_back(widget);
      
      return core::Success();
   }
   
   core::json::Value toJson() const
   {
      core::json::Object object;
      
      object["package"]  = package;
      object["binding"]  = binding;
      object["title"]    = title;
      object["subtitle"] = subtitle;
      object["caption"]  = caption;
      object["icon"]     = icon;
      
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
      
      core::Error error = core::json::readObject(
               object,
               "package",  &ptd.package,
               "binding",  &ptd.binding,
               "title",    &ptd.title,
               "subtitle", &ptd.subtitle,
               "caption",  &ptd.caption,
               "icon",     &ptd.icon);
      
      if (error)
         LOG_ERROR(error);
      
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
