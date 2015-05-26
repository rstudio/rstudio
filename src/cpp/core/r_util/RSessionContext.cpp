/*
 * RSessionContext.cpp
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

#include <core/r_util/RSessionContext.hpp>

#include <iostream>

#include <boost/format.hpp>

#include <boost/algorithm/string/trim.hpp>
#include <boost/algorithm/string/regex.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <core/FilePath.hpp>
#include <core/Settings.hpp>
#include <core/FileSerializer.hpp>

#include <core/http/Util.hpp>
#include <core/http/URL.hpp>

#include <core/system/System.hpp>
#include <core/system/Environment.hpp>

#define kSessionSuffix "-session"

namespace rstudio {
namespace core {
namespace r_util {

SessionScope projectNoneSessionScope()
{
   return SessionScope("default", "0");
}

std::string urlPathForSessionScope(const SessionScope& scope)
{
   // get a URL compatible project path
   std::string project = http::util::urlEncode(scope.project);
   boost::algorithm::replace_all(project, "%2F", "/");

   // create url
   boost::format fmt("/s/%1%/~%2%/");
   return boost::str(fmt % project % scope.id);
}

void parseSessionUrl(const std::string& url,
                     SessionScope* pScope,
                     std::string* pUrlPrefix,
                     std::string* pUrlWithoutPrefix)
{
   static boost::regex re("/s/(.+?)/~(\\d+)/");

   boost::smatch match;
   if (boost::regex_search(url, match, re))
   {
      if (pScope)
      {
         pScope->project = http::util::urlDecode(match[1]);
         pScope->id = match[2];
      }
      if (pUrlPrefix)
      {
         *pUrlPrefix = match[0];
      }
      if (pUrlWithoutPrefix)
      {
         *pUrlWithoutPrefix = boost::algorithm::replace_first_copy(
                                   url, std::string(match[0]), "/");
      }
   }
   else
   {
      if (pScope)
         *pScope = SessionScope();
      if (pUrlPrefix)
         *pUrlPrefix = std::string();
      if (pUrlWithoutPrefix)
         *pUrlWithoutPrefix = url;
   }
}


std::string createSessionUrl(const std::string& hostPageUrl,
                             const SessionScope& scope)
{
   // get url without prefix
   std::string url;
   parseSessionUrl(hostPageUrl, NULL, NULL, &url);

   // build path for project
   std::string path = urlPathForSessionScope(scope);

   // complete the url and return it
   return http::URL::complete(url, path);
}


std::ostream& operator<< (std::ostream& os, const SessionContext& context)
{
   os << context.username;
   if (!context.scope.project.empty())
      os << " -- " << context.scope.project;
   if (!context.scope.id.empty())
      os << " [" << context.scope.id << "]";
   return os;
}


std::string sessionScopeFile(std::string prefix,
                             const SessionScope& scope)
{   
   // resolve project path
   std::string project = scope.project;
   if (!project.empty())
   {
      // pluralize the prefix so there is no conflict when switching
      // between the single file and directory based schemas
      prefix += "s";

      if (!boost::algorithm::starts_with(project, "/"))
         project = "/" + project;

      if (!scope.id.empty())
      {
         if (!boost::algorithm::ends_with(project, "/"))
            project = project + "/";
      }
   }

   // return file path
   return prefix + project + scope.id;
}

std::string sessionContextFile(const SessionContext& context)
{
   return sessionScopeFile(context.username + kSessionSuffix, context.scope);
}

} // namespace r_util
} // namespace core 
} // namespace rstudio



