/*
 * SessionHttpConnectionListener.hpp
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

#ifndef SESSION_HTTP_CONNECTION_LISTENER_HPP
#define SESSION_HTTP_CONNECTION_LISTENER_HPP


/*
 The HttpConnectionListener is an IO service which runs in a background thread
 and accepts http requests. Once a request has been its accepted its
 HttpConnection object is placed on a threadsafe queue for retrieval by
 the foreground R thread.

 The foreground R thread will typically pull connections off the queue
 at two specific junctures:

   1) When it is waiting for a specific method from the client it will pull
      connections off the queue in hopes that the connection has the expected
      method. If the connection is not the expected method then it will still
      be handled and foreground R thread will continue waiting in a loop.

   2) Periodically during R_PolledEvents. This allows the client to remain
      responsive even while computations are being peformed.

 If a request pulled off the queue by the main thread can potentially be
 executed in a background thread (e.g. file or source operation) then it
 may (optionally) do so. Note that these request handlers should NEVER
 execute R code since it must all be called from the main thread.

 Note that since R_PolledEvents can occur during the processing of R code
 it is possible that requests which execute R code can execute in a nested fashion
 This is analogous to what occurs when GUIs pump events (during R_PolledEvents)
 that result in handling user gestures that call R code.

 R code which is running a computation can therefore be impacted by user
 gestures which occur during the computation (e.g. the value of an object
 in the global environment could be changed from under a computation or a
 required package could be unloaded during a computation). This sort of
 interaction is currently permitted by the OSX client and is considered OK
 presumably because the user directly manipulated the environment and therefore
 won't be suprised if his computation changes. The tradeoff is that operations
 that are read-only and highly useful to peform during computations (e.g.
 requesting completions or syntax checking in source mode, browsing objects,
 searching and viewing help, etc.) are allowed to execute thus maintaining
 a high level of interactivity in the client even when long computations
 are running.

 If we become uncomfortable with this behavior we could mark certain rpc or
 http handlers as requiring more stringent serialization. For example, they
 could be queued and executed only when the REPL loop comes back to the top.
 Note however that if too many of these requests are queued then browser
 request throttling may come into play and start queing ALL requests which
 exceed the browser limit. For this reason we will start with the position
 that nested execution of R handlers during computation is OK and back off
 only as necessary.

*/

#include "SessionHttpConnectionQueue.hpp"

namespace rstudio {
namespace core {
   class Error;
}
}

namespace rstudio {
namespace session {

// global initialization (allows instantation of listener which
// implements the protocol appropriate for our current configuration)
void initializeHttpConnectionListener();

// singleton
class HttpConnectionListener;
HttpConnectionListener& httpConnectionListener();

class HttpConnectionListener
{  
public:
   virtual ~HttpConnectionListener() {}

   // start and stop
   virtual core::Error start() = 0;
   virtual void stop() = 0;

   // connection queues
   virtual HttpConnectionQueue& mainConnectionQueue() = 0;
   virtual HttpConnectionQueue& eventsConnectionQueue() = 0;
};

} // namespace session
} // namespace rstudio

#endif // SESSION_HTTP_CONNECTION_LISTENER_HPP

