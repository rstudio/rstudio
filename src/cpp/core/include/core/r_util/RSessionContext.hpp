/*
 * RSessionContext.hpp
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

#ifndef CORE_R_UTIL_R_SESSION_CONTEXT_HPP
#define CORE_R_UTIL_R_SESSION_CONTEXT_HPP

#include <string>

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

SessionScope projectNoneSessionScope();

std::string urlPathForSessionScope(const SessionScope& scope);


void parseSessionUrl(const std::string& url,
                     SessionScope* pScope,
                     std::string* pUrlPrefix,
                     std::string* pUrlWithoutPrefix);

std::string createSessionUrl(const std::string& hostPageUrl,
                             const SessionScope& scope);


struct SessionContext
{
   SessionContext()
   {
   }

   explicit SessionContext(const std::string& username,
                           const SessionScope& scope = SessionScope())
      : username(username), scope(scope)
   {
   }
   std::string username;
   SessionScope scope;

   bool empty() const { return username.empty(); }

   bool operator==(const SessionContext &other) const {
      return username == other.username && scope == other.scope;
   }

   bool operator<(const SessionContext &other) const {
       return username < other.username ||
              (username == other.username && scope < other.scope);
   }
};


std::ostream& operator<< (std::ostream& os, const SessionContext& context);

std::string sessionContextToStreamFile(const SessionContext& context);

SessionContext streamFileToSessionContext(const std::string& file);



} // namespace r_util
} // namespace core 
} // namespace rstudio


#endif // CORE_R_UTIL_R_SESSION_CONTEXT_HPP

