/*
 * RSessionScope.hpp
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

#ifndef CORE_R_UTIL_R_SESSION_SCOPE_HPP
#define CORE_R_UTIL_R_SESSION_SCOPE_HPP

#include <boost/regex.hpp>
#include <boost/format.hpp>

#include <core/http/Util.hpp>

namespace rstudio {
namespace core {
namespace r_util {

struct SessionScope
{
   SessionScope()
   {
   }

   explicit SessionScope(const std::string& project,
                         const std::string& id)
      : project(project), id(id)
   {
   }

   std::string project;
   std::string id;

   bool empty() const { return project.empty(); }

   bool operator==(const SessionScope &other) const {
      return project == other.project && id == other.id;
   }

   bool operator!=(const SessionScope &other) const {
      return !(*this == other);
   }

   bool operator<(const SessionScope &other) const {
       return project < other.project ||
              (project == other.project && id < other.id);
   }
};

inline SessionScope projectNoneSessionScope()
{
   return SessionScope("default", "0");
}

inline std::string urlPathForSessionScope(const SessionScope& scope)
{
   // get a URL compatible project path
   std::string project = http::util::urlEncode(scope.project);
   boost::algorithm::replace_all(project, "%2F", "/");

   // create url
   boost::format fmt("/s/%1%/~%2%/");
   return boost::str(fmt % project % scope.id);
}

inline void parseSessionUrl(const std::string& url,
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
                                                   url, *pUrlPrefix, "/");
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


} // namespace r_util
} // namespace core 
} // namespace rstudio


#endif // CORE_R_UTIL_R_SESSION_SCOPE_HPP

