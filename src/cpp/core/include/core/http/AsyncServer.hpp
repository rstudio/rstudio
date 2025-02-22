/*
 * AsyncServer.hpp
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

#ifndef CORE_HTTP_ASYNC_SERVER_HPP
#define CORE_HTTP_ASYNC_SERVER_HPP

#include <string>

#include <boost/shared_ptr.hpp>
#include <boost/date_time/posix_time/ptime.hpp>
#include <boost/asio/io_context.hpp>

#include <core/ScheduledCommand.hpp>

#include <core/http/UriHandler.hpp>
#include <core/http/AsyncUriHandler.hpp>
#include <core/http/Response.hpp>
#include <core/http/AsyncConnection.hpp>

namespace rstudio {
namespace core {
namespace http {

struct ConnectionInfo {
   bool closed, requestParsed, sendingResponse;
   std::string requestUri, username;
   boost::posix_time::ptime startTime;
   int requestSequence;
};

struct ServerInfo {
   long numRequests;
   long numStreaming;
   boost::posix_time::time_duration minTime;
   boost::posix_time::time_duration maxTime;
   std::string maxUrl;
   boost::posix_time::time_duration totalTime;
   boost::posix_time::time_duration elapsedTime;
};

class AsyncServer
{
public:   
   virtual ~AsyncServer()
   {
   }

   virtual boost::asio::io_context& ioContext() = 0;
   
   virtual void setAbortOnResourceError(bool abortOnResourceError) = 0;
   
   virtual void addHandler(const std::string& prefix,
                           const AsyncUriHandlerFunction& handler) = 0;

   virtual void addUploadHandler(const std::string& prefix,
                                 const AsyncUriUploadHandlerFunction& handler) = 0;

   virtual void addProxyHandler(const std::string& prefix,
                                const AsyncUriHandlerFunction& handler) = 0;


   virtual void addBlockingHandler(const std::string& prefix,
                                   const UriHandlerFunction& handler) = 0;


   virtual void setDefaultHandler(const AsyncUriHandlerFunction& handler) = 0;


   virtual void setBlockingDefaultHandler(const UriHandlerFunction& handler) = 0;

   virtual void setScheduledCommandInterval(
                           boost::posix_time::time_duration interval) = 0;
   virtual void addScheduledCommand(boost::shared_ptr<ScheduledCommand> pCmd) = 0;

   virtual void setRequestFilter(RequestFilter requestFilter) = 0;
   virtual void setResponseFilter(ResponseFilter responseFilter) = 0;

   virtual Error runSingleThreaded() = 0;

   virtual Error run(std::size_t threadPoolSize = 1) = 0;

   virtual bool isRunning() = 0;

   virtual void stop() = 0;
   
   virtual void waitUntilStopped() = 0;

   virtual void setNotFoundHandler(const NotFoundHandler& handler) = 0;

   virtual void addStreamingUriPrefix(const std::string& uriPrefix) = 0;

   virtual int getActiveConnectionCount() = 0;

   virtual long getServerInfoSnapshot(std::vector<ConnectionInfo>& connInfoList, ServerInfo& serverInfo, bool reset) = 0;
};

class AsyncServerStatsProvider
{
public:
   AsyncServerStatsProvider()
   {
   }
   virtual ~AsyncServerStatsProvider()
   {
   }

   // Just before the uri handler is called
   virtual void httpStart(const AsyncConnection& connection) {}

   // Just after the uri handler returns used to see how long handlers are blocking the thread
   virtual void httpEndHandler(const AsyncConnection& connection) {}

   // Just after the response
   virtual void httpEnd(const core::http::Request& request, const core::http::Response& response, const bool isStreaming) {}

   virtual void httpNoResponse() {}
};

} // namespace http
} // namespace core
} // namespace rstudio

#endif // CORE_HTTP_ASYNC_SERVER_HPP


