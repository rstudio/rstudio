/*
 * DistributedEvents.cpp
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

#include <core/Error.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/r_util/RSessionContext.hpp>
#include <core/SocketRpc.hpp>

#include <core/DistributedEvents.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace core {
namespace distributed_events {

namespace {

static DistributedEventAsyncHandler s_fireEventAsync;
static DistributedEventSyncHandler s_fireEventSync;

Error fireDistributedEventImpl(const std::string& requestBody,
                               boost::function<void (const DistributedEvent&)> notification)
{
   json::Value eventVal;

   // parse event value object from the request
   if (!json::parse(requestBody, &eventVal) ||
       eventVal.type() != json::ObjectType)
      return Error(json::errc::ParseError, ERROR_LOCATION);

   // read the event from the object
   int eventType;
   std::string eventOrigin;
   json::Object eventData;
   Error error = json::readObject(eventVal.get_obj(),
                                  kDistEvtEventType, &eventType,
                                  kDistEvtEventData, &eventData,
                                  kDistEvtOrigin,    &eventOrigin);
   if (error)
      return error;

   // fire to listeners
   if (notification)
      notification(DistributedEvent(static_cast<DistEvtType>(eventType), eventData, eventOrigin));

   return Success();
}

} // anonymous namespace

Error fireDistributedEventAsync(boost::shared_ptr<http::AsyncConnection> pConnection)
{
   const http::Request& request = pConnection->request();
   http::Response& response = pConnection->response();

   if (s_fireEventAsync)
   {
      Error error = fireDistributedEventImpl(request.body(), boost::bind(s_fireEventAsync,
                                                                         _1,
                                                                         pConnection));
      if (error)
         return error;
   }

   // acknowledge the event
   json::Object result;
   result["suceeded"] = true;
   std::ostringstream oss;
   json::write(result, oss);

   response.setStatusCode(http::status::Ok);
   response.setBody(oss.str());
   pConnection->writeResponse();

   return Success();
}

Error fireDistributedEvent(const http::Request& request,
                           http::Response* pResponse)
{
   if (s_fireEventSync)
   {
      Error error = fireDistributedEventImpl(request.body(), s_fireEventSync);
      if (error)
         return error;
   }

   // acknowledge the event
   json::Object result;
   result["suceeded"] = true;
   std::ostringstream oss;
   json::write(result, oss);

   pResponse->setStatusCode(http::status::Ok);
   pResponse->setBody(oss.str());

   return Success();
}

Error emitDistributedEvent(const std::string& targetType,
                           const std::string& target,
                           const DistributedEvent& distEvt)
{
   // construct the event to broadcast
   json::Object event;
   event[kDistEvtTargetType] = targetType;
   event[kDistEvtTarget]     = target;
   event[kDistEvtEventType]  = distEvt.type();
   event[kDistEvtEventData]  = distEvt.data();
   event[kDistEvtOrigin]     = distEvt.origin();

   // and broadcast it!
   json::Value result;

   return socket_rpc::invokeRpc(FilePath(kServerRpcSocketPath), kDistributedEventsEndpoint, event, &result);
}

Error initializeAsync(DistributedEventAsyncHandler eventHandler)
{
   s_fireEventAsync = eventHandler;
   return Success();
}

Error initialize(DistributedEventSyncHandler eventHandler)
{
   s_fireEventSync = eventHandler;
   return Success();
}

} // distributed_events namespace
} // core namespace
} // rstudio namespace
