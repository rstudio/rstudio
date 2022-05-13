/*
 * SessionSuspendFilter.hpp
 *
 * Copyright (C) 2022 by RStudio, PBC
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

#include <unordered_set>

#include "core/DistributedEvents.hpp"

#include "session/SessionSuspendFilter.hpp"

namespace rstudio {
namespace session {
namespace suspend {
namespace filter  {

class DistEventFilter : public SessionSuspendFilter
{
public:
   DistEventFilter()
      : m_uri(kDistributedEventsEndpoint),
        m_filteredEvents()
   {
      m_filteredEvents.insert({
                                 core::DistEvtUserLeft
                              });
   };
   ~DistEventFilter() {};

   virtual bool shouldResetSuspendTimer(boost::shared_ptr<HttpConnection> pConnection) override
   {
      return true;
   };

private:
   std::string m_uri;
   std::unordered_set<core::DistEvtType> m_filteredEvents;
};

} // namespace filter
} // namespace suspend
} // namespace session
} // namespace rstudio
