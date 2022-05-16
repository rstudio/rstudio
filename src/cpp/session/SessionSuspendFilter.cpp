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

#include <session/SessionSuspendFilter.hpp>

namespace rstudio {
namespace session {
namespace suspend {

// Define new filters here
namespace filter  {

/**
 * Marks the /distributed_events URI as a connection that should not reset a session's suspend timeout
 */
class DistEventFilter : public SessionSuspendFilter
{
public:
   DistEventFilter()  {}
   ~DistEventFilter() {}

   virtual bool shouldResetSuspendTimer(boost::shared_ptr<HttpConnection> pConnection) override
   {
      if (pConnection->request().uri() == "/distributed_events")
         return false;

      return true;
   };
};

/**
 * /rpc/set_currently_editing shouldn't reset a session's suspend timer because what
 * another user is doing/editing shouldn't affect our session
 */
class SetEditingFilter : public SessionSuspendFilter
{
public:
   SetEditingFilter()  {}
   ~SetEditingFilter() {}

   virtual bool shouldResetSuspendTimer(boost::shared_ptr<HttpConnection> pConnection) override
   {
      if (pConnection->request().uri() == "/rpc/set_currently_editing")
         return false;

      return true;
   };
};

} // namespace filter

SessionSuspendFilters::SessionSuspendFilters()
   : m_filters()
{
   using namespace rstudio::session::suspend::filter;

   // Add filters here to prevent specific connections from resetting a session's suspend timeout
   m_filters.insert(m_filters.end(), {
                       boost::make_shared<DistEventFilter>(),
                       boost::make_shared<SetEditingFilter>()
                    });
}

SessionSuspendFilters::~SessionSuspendFilters() {}

/**
 * Determines if recieving a given HttpConnection should reset the suspend timer for the session.
 * Assumes that the timer *should* be reset unless a filter explicitly says not to
 *
 * @param pConnection The HttpConnection in question
 *
 * @return False if any filter determines the timer should not be reset. True otherwise.
 */
bool SessionSuspendFilters::shouldResetSuspendTimer(boost::shared_ptr<HttpConnection> pConnection)
{
   // Assume we *should* reset unless we have a concrete reason not to
   if (!pConnection)
      return true;

   for (auto &filter : m_filters)
   {
      // bail on first filter that says we shouldn't reset the timer
      if (!filter->shouldResetSuspendTimer(pConnection))
         return false;
   }

   return true;
}

} // namespace suspend
} // namespace session
} // namespace rstudio
