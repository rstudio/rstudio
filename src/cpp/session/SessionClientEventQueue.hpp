/*
 * SessionClientEventQueue.hpp
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

#ifndef SESSION_SESSION_CLIENT_EVENT_QUEUE_HPP
#define SESSION_SESSION_CLIENT_EVENT_QUEUE_HPP

#include <string>
#include <vector>

#include <boost/function.hpp>
#include <boost/utility.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>

#include <core/BoostThread.hpp>

#include <session/SessionClientEvent.hpp>

namespace rstudio {
namespace session {
   
// initialization
void initializeClientEventQueue();

// singleton
class ClientEventQueue;
ClientEventQueue& clientEventQueue();

class ClientEventQueue : boost::noncopyable
{   
private:
   ClientEventQueue();
   friend void initializeClientEventQueue();
   
public:
   // COPYING: boost::noncopyable
     
   // add an event
   void add(const ClientEvent& event);
   
   // remove all available events
   void remove(std::vector<ClientEvent>* pEvents);
   
   // are there any events pending?
   bool hasEvents();
   
   // clear the event queue
   void clear();
   
   // wait for a new event 
   bool waitForEvent(const boost::posix_time::time_duration& waitDuration);
   
   // has an event been added since the specified time
   bool eventAddedSince(const boost::posix_time::ptime& time);

   // set the active console to be attached to console events; returns true if
   // the active console changed
   bool setActiveConsole(const std::string& console);
      
private:   
   void flushPendingConsoleOutput();

   void enqueueClientOutputEvent(int event, const std::string& text);
 
private:
   // synchronization objects. heap based so they are never destructed
   // we don't want them destructed because in desktop mode we don't
   // explicitly stop the queue and this sometimes results in mutex
   // destroy assertions if someone is waiting on the queue while
   // it is being destroyed
   boost::mutex* pMutex_;
   boost::condition* pWaitForEventCondition_;

   // instance data
   std::string pendingConsoleOutput_;
   std::string activeConsole_;
   std::vector<ClientEvent> pendingEvents_;
   boost::posix_time::ptime lastEventAddTime_;
   

};

} // namespace session
} // namespace rstudio

#endif // SESSION_SESSION_CLIENT_EVENT_QUEUE_HPP
