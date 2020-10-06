/*
 * SessionHttpConnectionQueue.hpp
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

#ifndef SESSION_HTTP_CONNECTION_QUEUE_HPP
#define SESSION_HTTP_CONNECTION_QUEUE_HPP

#include <queue>

#include <boost/shared_ptr.hpp>

#include <boost/utility.hpp>

#include <core/BoostThread.hpp>

#include <session/SessionHttpConnection.hpp>

namespace rstudio {
namespace core {
   class Error;
}
}

namespace rstudio {
namespace session {

class HttpConnectionQueue : boost::noncopyable
{
public:
   HttpConnectionQueue()
      : pMutex_(new boost::mutex()),
        pWaitCondition_(new boost::condition())
   {
   }

   void enqueConnection(boost::shared_ptr<HttpConnection> ptrConnection);

   boost::shared_ptr<HttpConnection> dequeConnection();

   boost::shared_ptr<HttpConnection> dequeConnection(
               const boost::posix_time::time_duration& waitDuration);

   std::string peekNextConnectionUri();

   boost::posix_time::ptime lastConnectionTime();

private:
   boost::shared_ptr<HttpConnection> doDequeConnection();
   bool waitForConnection(const boost::posix_time::time_duration& waitDuration);

private:
   // synchronization objects. heap based so they are never destructed
   // we don't want them destructed because in desktop mode we don't
   // explicitly stop the queue and this sometimes results in mutex
   // destroy assertions if someone is waiting on the queue while
   // it is being destroyed
   boost::mutex* pMutex_;
   boost::condition* pWaitCondition_;

   // instance data
   boost::posix_time::ptime lastConnectionTime_;
   std::queue<boost::shared_ptr<HttpConnection> > queue_;
};

} // namespace session
} // namespace rstudio

#endif // SESSION_HTTP_CONNECTION_QUEUE_HPP

