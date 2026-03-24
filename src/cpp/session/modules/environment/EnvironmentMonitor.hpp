/*
 * EnvironmentMonitor.hpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <r/RSexp.hpp>
#include <r/RInterface.hpp>

#ifndef SESSION_MODULES_ENVIRONMENT_MONITOR_HPP
#define SESSION_MODULES_ENVIRONMENT_MONITOR_HPP

namespace rstudio {
namespace session {
namespace modules {
namespace environment {

// EnvironmentMonitor listens for changes to objects in the given environment
// context, and emits object add/remove events.
class EnvironmentMonitor : boost::noncopyable
{
public:
   EnvironmentMonitor();
   void setMonitoredEnvironment(SEXP pEnvironment, bool refresh = false);
   SEXP getMonitoredEnvironment();
   bool hasEnvironment();
   void checkForChanges();

   // opaque snapshot of a binding for change detection
   struct BindingSnapshot
   {
      std::string name;
      r::sexp::BindingType type;
      SEXP identityToken; // opaque pointer for change detection; never dereferenced

      bool operator<(const BindingSnapshot& other) const
      {
         if (name != other.name)
            return name < other.name;
         if (type != other.type)
            return type < other.type;
         return std::less<SEXP>()(identityToken, other.identityToken);
      }

      bool operator==(const BindingSnapshot& other) const
      {
         return name == other.name &&
                type == other.type &&
                identityToken == other.identityToken;
      }

      bool operator!=(const BindingSnapshot& other) const
      {
         return !(*this == other);
      }
   };

   void listEnv(std::vector<std::string>* pNames);
   void snapshotBindings(SEXP env,
                         const std::vector<std::string>& names,
                         std::vector<BindingSnapshot>* pSnapshot);
   void enqueRemovedEvent(const std::string& name);
   void enqueAssignedEvent(const std::string& name);

   std::vector<BindingSnapshot> lastEnv_;
   std::vector<std::string> unevaledPromises_;
   r::sexp::PreservedSEXP environment_;
   bool initialized_;
   bool refreshOnInit_;
};

} // namespace environment
} // namespace modules
} // namespace session
} // namespace rstudio

#endif /* SESSION_MODULES_ENVIRONMENT_MONITOR_HPP */
