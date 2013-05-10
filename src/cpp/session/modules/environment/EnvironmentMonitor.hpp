/*
 * EnvironmentMonitor.hpp
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

#include <r/RSexp.hpp>
#include <r/RInterface.hpp>

namespace session {
namespace modules {
namespace environment {

// EnvironmentMonitor listens for changes to objects in the given environment
// context, and emits object add/remove events.
class EnvironmentMonitor : boost::noncopyable
{
public:
   void setMonitoredEnvironment(SEXP pEnvironment);
   void checkForChanges();
private:
   void listEnv(std::vector<r::sexp::Variable>* pEnvironment);

   std::vector<r::sexp::Variable> lastEnv_;
   r::sexp::PreservedSEXP environment_;
   bool initialized_;
};

} // namespace environment
} // namespace modules
} // namespace session
