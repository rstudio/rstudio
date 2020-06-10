/*
 * REventLoop.hpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

#ifndef R_EVENT_LOOP_HPP
#define R_EVENT_LOOP_HPP

#include <boost/date_time/posix_time/posix_time_duration.hpp>

namespace rstudio {
namespace r {
namespace session {
namespace event_loop {

//
// R calls the polled event handler via R_PolledEvents. this occurs at
// the following times:
//
//    - During Rstd_ReadConsole (via R_runHandlers). Note that front-ends
//      which install a custom ptr_R_ReadConsole will not be called in
//      this context.
//
//    - During do_syssleep (via R_runHandlers)
//
//    - During R_CheckUserInterrupt
//
//    - During R_SocketWait
//
//    - During RxmlNanoHTTPConnectAttempt and RxmlNanoHTTPRecv
//
// The fact that the handler is called from so many contexts make it
// suitable for implementing the event-polling required for a responsive
// GUI. This could take the form of pumping GUI events or checking for
// inbound network requests. The point is that this function will continue
// to be called even while R is busy doing other things like processing
// network requests, waiting for input, sleeping, or performing computations
//
void initializePolledEventHandler(void (*newPolledEventHandler)(void));

void permanentlyDisablePolledEventHandler();

// check whether the polled event handler has already been initialized
bool polledEventHandlerInitialized();

// event processing (allowing R gui components like GraphApp or the quartz
// device to remain responsive)
void processEvents();



} // namespace event_loop
} // namespace session
} // namespace r
} // namespace rstudio


#endif // R_EVENT_LOOP_HPP

