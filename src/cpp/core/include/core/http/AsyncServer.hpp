/*
 * AsyncServer.hpp
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

#ifndef CORE_HTTP_ASYNC_SERVER_HPP
#define CORE_HTTP_ASYNC_SERVER_HPP

#include <string>

#include <boost/shared_ptr.hpp>
#include <boost/date_time/posix_time/ptime.hpp>
#include <boost/asio/io_service.hpp>

#include <core/ScheduledCommand.hpp>

#include <core/http/UriHandler.hpp>
#include <core/http/AsyncUriHandler.hpp>
#include <core/http/Response.hpp>

namespace rstudio {
namespace core {
namespace http {

class AsyncServer
{
public:   
   virtual ~AsyncServer()
   {
   }

   virtual boost::asio::io_service& ioService() = 0;
   
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

};

} // namespace http
} // namespace core
} // namespace rstudio

#endif // CORE_HTTP_ASYNC_SERVER_HPP


