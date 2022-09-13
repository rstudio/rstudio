/*
 * SessionSuspendFilter.hpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef SESSION_SUSPEND_FILTER_HPP
#define SESSION_SUSPEND_FILTER_HPP

#include "SessionHttpConnection.hpp"

namespace rstudio {
namespace session {
namespace suspend {

/**
 * Interface for filtering out which HttpConnections should reset a session's suspend timeout
 */
class SessionSuspendFilter
{
public:
   virtual ~SessionSuspendFilter() {}
   virtual bool shouldResetSuspendTimer(boost::shared_ptr<HttpConnection> pConnection) = 0;
};

/**
 * This class holds an expandable list of filter instances that help a session decide
 * whether or not its suspend timeout should be reset, based any characteristics
 * of a request.
 *
 * By default, accepted HttpConnections will reset a session's suspend timeout. To
 * add a new filter for a specific type of request, simply
 * 1. define the filter in SessionSuspendFilter.cpp and implement the
 *    SessionSuspendFilter interface
 * 2. add the new filter instance into this class' constructor
 * 
 * Turn on session protocol debug to aid in discovering which requests are triggering
 * session suspend timeout resets
 */
class SessionSuspendFilters
{
public:
   SessionSuspendFilters();
   ~SessionSuspendFilters();

   bool shouldResetSuspendTimer(boost::shared_ptr<HttpConnection> pConnection);

protected:
   std::vector<boost::shared_ptr<SessionSuspendFilter>> m_filters; //!< Expandable list of filters to check connections with
};

} // namespace suspend
} // namespace session
} // namespace rstudio

#endif // SESSION_SUSPEND_FILTER_HPP

