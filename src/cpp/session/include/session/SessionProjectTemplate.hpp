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

struct ProjectTemplateDescription
{
   std::string package;
   std::string binding;
   std::string title;
   std::string subtitle;
   std::string caption;
   
   core::json::Value toJson() const
   {
      core::json::Object object;
      
      object["package"]  = package;
      object["binding"]  = binding;
      object["title"]    = title;
      object["subtitle"] = subtitle;
      object["caption"]  = caption;
      
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
