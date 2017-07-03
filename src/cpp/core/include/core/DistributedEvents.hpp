/*
 * DistributedEvents.hpp
 *
 * Copyright (C) 2017 by RStudio, Inc.
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

#ifndef DISTRIBUTED_EVENTS_HPP
#define DISTRIBUTED_EVENTS_HPP

#include <core/Exec.hpp>
#include <core/json/Json.hpp>
#include <core/http/AsyncConnection.hpp>
#include <core/http/Request.hpp>
#include <core/http/Response.hpp>

#define kDistributedEventsEndpoint "/distributed_events"
#define kDistEvtSecureCookie "event_secure_cookie"

#define kDistEvtTargetType "target_type"
#define kDistEvtTarget     "target"
#define kDistEvtEventType  "event_type"
#define kDistEvtEventData  "event_data"
#define kDistEvtOrigin     "origin_session"

#define kDistEvtTargetProject "project"
#define kDistEvtTargetSession "session"
#define kDistEvtTargetWorkspaces "workspaces"

namespace rstudio {
namespace core {

class Error;

enum DistEvtType
{
   DistEvtUserJoined,
   DistEvtUserLeft,
   DistEvtUserChanged,
   DistEvtFollowStarted,
   DistEvtFollowEnded,
   DistEvtActiveFileChanged,
   DistEvtActiveFileMatched,
   DistEvtCollabStartRequested,
   DistEvtCollabStarted,
   DistEvtCollabEnded,
   DistEvtProjectACLChanged,
   DistEvtFileSaved, 
   DistEvtNotebookChunkOutput,
   DistEvtNotebookConsoleOutput,
   DistEvtShutdownSessions,
   DistEvtKillSessions
};

class DistributedEvent
{
public:
   DistributedEvent(DistEvtType type, const core::json::Object& data):
      type_(type),
      data_(data)
   {
   }

   DistributedEvent(DistEvtType type, const core::json::Object& data,
                    const std::string& origin):
      type_(type),
      data_(data),
      origin_(origin)
   {
   }

   DistEvtType type() const
   {
      return type_;
   }

   const core::json::Object& data() const
   {
      return data_;
   }

   std::string origin() const
   {
      return origin_;
   }

private:
   DistEvtType type_;
   core::json::Object data_;
   std::string origin_;
};

typedef boost::function<void (const DistributedEvent&, boost::shared_ptr<http::AsyncConnection>)> DistributedEventAsyncHandler;
typedef boost::function<void (const DistributedEvent&)> DistributedEventSyncHandler;

namespace distributed_events {

// emit (broadcast) a distributed event
Error emitDistributedEvent(const std::string& targetType,
                           const std::string& target,
                           const DistributedEvent& distEvt);

// receives a distributed session event from the server and broadcasts it
// asynchronous mode
Error fireDistributedEventAsync(boost::shared_ptr<http::AsyncConnection> pConnection);

// receives a distributed session event from the server and broadcasts it
// synchronous mode
Error fireDistributedEvent(const http::Request& request,
                           http::Response* pResponse);

// initialize in asynchronous mode
// pair this with calls to fireDistributedEventAsync
Error initializeAsync(DistributedEventAsyncHandler eventHandler);


// initialize in synchronous mode
// pair this with calls to fireDistributedEvent
Error initialize(DistributedEventSyncHandler eventHandler);

} // namespace distributed_events
} // namespace core
} // namespace rstudio

#endif // DISTRIBUTED_EVENTS_HPP


