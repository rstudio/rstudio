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

#define kProjectTemplateWidgetTypeCheckBoxInput "checkboxinput"
#define kProjectTemplateWidgetTypeSelectInput   "selectinput"
#define kProjectTemplateWidgetTypeTextInput     "textinput"
#define kProjectTemplateWidgetTypeFileInput     "fileinput"

struct ProjectTemplateWidgetDescription
{
   // COPYING: copyable members
   
   std::string parameter;
   std::string type;
   std::string label;
   std::string defaultValue;
   std::string position;
   std::vector<std::string> fields;

   core::json::Value toJson() const;
};

core::Error fromJson(
      const core::json::Object& object,
      ProjectTemplateWidgetDescription* pDescription);

struct ProjectTemplateDescription
{
   // COPYING: copyable members
   
   std::string package;
   std::string binding;
   std::string title;
   std::string subtitle;
   std::string caption;
   std::string icon;
   std::vector<std::string> openFiles;
   std::vector<ProjectTemplateWidgetDescription> widgets;
   
   core::json::Value toJson() const;
};

core::Error fromJson(
      const core::json::Object&,
      ProjectTemplateDescription* pDescription);

core::Error initialize();

} // end namespace templates
} // end namespace projects
} // end namespace modules
} // end namespace session
} // end namespace rstudio

#endif /* SESSION_PROJECT_TEMPLATE_HPP */
