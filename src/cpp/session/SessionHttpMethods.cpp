/*
 * SessionHttpMethods.hpp
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

// We need LogLevel::DEBUG so don't #define DEBUG for this file
#define RSTUDIO_DEBUG_MACROS_DISABLED

#include "SessionHttpMethods.hpp"
#include "SessionConsoleInput.hpp"
#include "SessionMainProcess.hpp"
#include "SessionClientInit.hpp"
#include "SessionInit.hpp"
#include "SessionUriHandlers.hpp"
#include "SessionDirs.hpp"
#include "SessionRpc.hpp"
#include "http/SessionTcpIpHttpConnectionListener.hpp"

#include "session-config.h"

#include <atomic>
#include <chrono>
#include <cmath>
#include <thread>

#include <boost/algorithm/string.hpp>
#include <boost/optional.hpp>

#include <shared_core/json/Json.hpp>
#include <shared_core/Logger.hpp>
#include <shared_core/Memory.hpp>

#include <core/StartupTiming.hpp>
#include <core/Thread.hpp>
#include <core/gwt/GwtFileHandler.hpp>
#include <core/gwt/GwtLogHandler.hpp>
#include <core/http/CSRFToken.hpp>
#include <core/http/ProxyUtils.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/system/Crypto.hpp>
#include <core/text/TemplateFilter.hpp>

#include <r/RExec.hpp>
#include <r/ROptions.hpp>
#include <r/session/RSession.hpp>
#include <r/session/REventLoop.hpp>

#include <session/RVersionSettings.hpp>
#include <session/SessionHttpConnection.hpp>
#include <session/SessionHttpConnectionListener.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/SessionOptions.hpp>
#include <session/SessionPersistentState.hpp>
#include <session/SessionScopes.hpp>
#include <session/SessionSuspend.hpp>
#include <session/projects/SessionProjects.hpp>
#include <session/prefs/UserPrefs.hpp>

#ifdef RSTUDIO_SERVER
#include <server_core/sessions/SessionSignature.hpp>
#endif

#include "SessionAsyncRpcConnection.hpp"
#include "SessionOfflineService.hpp"

using namespace rstudio::core;
using namespace boost::placeholders;

namespace rstudio {
namespace session {
namespace {

// leaked: invoked directly on the static asset thread (#18318)
core::http::UriHandlerFunction& s_defaultUriHandler =
      core::make_leaked<core::http::UriHandlerFunction>();

// whether s_defaultUriHandler has been assigned; the listener thread gates
// its static-asset fast path on this. registerGwtHandlers() runs (in desktop
// and standalone modes only) before the listener thread starts, so the store
// is always visible to it; the atomic is defensive, not load-bearing
std::atomic<bool> s_gwtHandlersRegistered(false);

// version of the executable -- this is the legacy version designator. we
// set it to double max so that it always invalidates legacy clients
double s_version = std::numeric_limits<double>::max();

// names of waitForMethod handlers (used to screen out of bkgnd processing)
std::vector<std::string> s_waitForMethodNames;

// R code dispatched from the idle loop. waitForMethod() runs R's input
// handlers (later callbacks among them) between dequeues, so a handler that
// runs for a long time -- shiny::runApp() scheduled with later::later() --
// keeps it from dequeuing, while console_input::executing() stays false
// because the console is at its prompt. Tracked so that the polled event
// handler keeps serving requests, interrupts can end the handler, and the
// client learns that R is busy (#19236).
struct IdleEventState
{
   bool active = false;
   boost::posix_time::ptime started;
   // the busy report is armed; cleared when a handler's own console read
   // clears the client's busy state, so that it is reported again
   bool busyNotified = false;
   // the client was told R is busy at some point: busy=false is owed when
   // the handlers return, however many times the report was re-armed
   bool busyReported = false;
   // the busy report applies: the handlers run for a console read, with no
   // console input executing. not for a read nested in a running command
   // (the client is busy anyway), and not for a wait for the client inside
   // a command or an RPC's R code (a dialog): the console shows no prompt
   // afterwards that would clear a report, and the client is in the dialog
   bool reportsBusy = false;
   // what the wait the handlers run for tells a client that (re)connects
   // meanwhile (its waitForMethod initFunction): the waiting read's prompt,
   // or a dialog's event again. a reload while a handler runs is answered
   // with it, as the wait itself would answer it
   boost::function<void()> clientInit;
   // prompts issued when the busy report was last (re)armed; a handler's own
   // console read (readline()) issues one, which clears the client's busy
   // state, so the report is armed again after it
   unsigned promptsIssued = 0;
   // how long a handler runs before the report (see idleEventBusyDelay);
   // read when the report is first considered
   boost::optional<boost::posix_time::time_duration> busyDelay;
};
IdleEventState s_idleEvents;

// when the active handlers were dispatched (steady clock, nanoseconds; zero
// while inactive), for the listener thread (see processingIdleEvents)
std::atomic<std::int64_t> s_idleEventsSince(0);

// how long the handlers run before they count as a callback running rather
// than the dispatch itself, which with no callback due takes microseconds
// and runs twenty times a second. requests are served from the polled
// event handler only past it, and an interrupt counts as meant for the
// handlers only past it: landing in the dispatch alone it would be
// swallowed with the next read still waiting, while one meant for an
// interactive read (Escape at readline(), or at a continuation prompt) has
// to become a cancel of that read instead (see idleEventsPastDispatch)
const std::chrono::milliseconds kIdleEventDispatchGrace(50);

std::int64_t steadyNowNs()
{
   return std::chrono::duration_cast<std::chrono::nanoseconds>(
            std::chrono::steady_clock::now().time_since_epoch()).count();
}

std::int64_t dispatchGraceNs()
{
   return std::chrono::duration_cast<std::chrono::nanoseconds>(kIdleEventDispatchGrace).count();
}

void setIdleEvents(const IdleEventState& state)
{
   s_idleEvents = state;
   s_idleEventsSince = state.active ? steadyNowNs() : 0;
}

// how long an input handler runs before the client is told R is busy: the
// R option rstudio.idleEventBusyDelay, in seconds, one by default -- the
// callbacks later usually runs take well under a millisecond. read once a
// handler has run long enough for the report to be considered (see
// polledEventHandler): the handlers are dispatched twenty times a second
// while the session idles, and nearly every dispatch returns at once
boost::posix_time::time_duration idleEventBusyDelay()
{
   // anything but a finite delay of up to a day is a mistake: use the default
   double seconds = r::options::getOption<double>("rstudio.idleEventBusyDelay", 1.0, false);
   if (!std::isfinite(seconds) || seconds < 0 || seconds > 86400)
      seconds = 1.0;

   // never less than the dispatch grace: the polled event handler runs in
   // the dispatch itself, and a delay of zero would report every one busy
   long ms = std::max(static_cast<long>(seconds * 1000),
                      static_cast<long>(kIdleEventDispatchGrace.count()));
   return boost::posix_time::milliseconds(ms);
}

// url for next session
std::string s_nextSessionUrl;

// are we shutting down?
bool s_shuttingDown = false;

bool s_protocolDebugEnabled = false;
bool s_sessionDebugLogCreated = false;

void onShutdown(bool)
{
   s_shuttingDown = true;
}

bool parseAndValidateJsonRpcConnection(
   boost::shared_ptr<HttpConnection> ptrConnection,
   json::JsonRpcRequest* pJsonRpcRequest);

void processEvents()
{
   if (!ASSERT_MAIN_THREAD())
   {
      return;
   }

   // execute safely since this can call arbitrary R code (and
   // (can also cause jump_to_top if an interrupt is pending)
   Error error = rstudio::r::exec::executeSafely(
            rstudio::r::session::event_loop::processEvents);

   if (error) // If the statement is interrupted, this can return an error so logging as debug to limit visibility
      LOG_DEBUG_MESSAGE(error.asString());
}

// s_idleEvents for the duration of a scope: active while R's input handlers
// run from the idle loop, inactive while a wait for the client runs its own
// loop (a handler can itself wait: readline() inside a later callback), since
// the handler is then not what keeps requests from being served
class IdleEventsScope
{
public:
   explicit IdleEventsScope(bool active,
                            bool reportsBusy = false,
                            const boost::function<void()>& clientInit = boost::function<void()>())
      : saved_(s_idleEvents)
   {
      IdleEventState state;
      state.active = active;
      state.started = boost::posix_time::microsec_clock::universal_time();
      state.reportsBusy = reportsBusy;
      state.clientInit = clientInit;
      state.promptsIssued = console_input::promptsIssued();
      setIdleEvents(state);
   }

   ~IdleEventsScope()
   {
      setIdleEvents(saved_);
   }

private:
   IdleEventState saved_;
};

// run R's input handlers from the idle loop (see IdleEventState) for a wait
// on `method`, with `initFunction` what that wait tells a client that
// connects meanwhile. returns whether the waiting console read's prompt was
// reissued -- done only for a console read: a wait for the client inside a
// command (a dialog) has no prompt to restore, and the prompt current then
// (the enclosing read's, or none) is not what the console shows
bool processIdleEvents(const std::string& method,
                       const boost::function<void()>& initFunction)
{
   bool busyReported = false;
   bool executingBefore = console_input::executing();
   bool atTopLevelBefore = r::session::atTopLevelPrompt();
   bool browserBefore = r::session::browserContextActive();
   unsigned promptsBefore = console_input::promptsIssued();
   unsigned readsBefore = console_input::readsCompleted();
   unsigned discardsBefore = console_input::inputDiscards();
   {
      IdleEventsScope idleEventsActive(
               true,
               method == kConsoleInput && !console_input::executing(),
               initFunction);
      processEvents();
      busyReported = s_idleEvents.busyReported;
   }

   // a console read a handler ran (readline()) marks console input as
   // executing, and R as no longer at its top-level prompt, when it hands R
   // the answer, as any read does; the handler has returned now and the
   // waiting read is back where it was (a prompt reports R busy otherwise)
   if (!executingBefore && console_input::executing())
   {
      console_input::setExecuting(false);
      console_input::updateSessionExecuting();
   }
   if (atTopLevelBefore && !r::session::atTopLevelPrompt())
      r::session::setAtTopLevelPrompt(true);

   // a browser() a handler entered marked a browser active from its
   // Browse[N]> read; the handler has returned past it, and the next
   // top-level read, which would clear the mark, is the one already waiting
   // (quitting is refused while a browser is active). the environment
   // module's own debug state follows at the next REPL prompt (#19259)
   if (!browserBefore && r::session::browserContextActive())
      r::session::setBrowserActive(false);

   // busy=false is owed once the client was told R is busy
   if (busyReported)
   {
      ClientEvent busyEvent(client_events::kBusy, false);
      module_context::enqueClientEvent(busyEvent);
   }

   if (method != kConsoleInput)
      return false;

   // reissue the waiting read's prompt, which also tells the client R is
   // back, when the client was told R is busy; when a prompt was issued
   // meanwhile -- a handler's own console read
   // (readline()), whose prompt replaced the waiting read's, or a reload's
   // client_init answered with the waiting read's prompt, which R mid-
   // handler reported busy; when a read took input the client submitted,
   // leaving it busy with no prompt to follow; or when input queued
   // meanwhile was discarded (an interrupt, an uncaught error with the
   // discard pref on), which leaves the client busy the same way. not when
   // a reset found nothing queued: the client saw nothing of it, and a
   // prompt would reset the console's history position and refresh the
   // panes for no command. and not with input pending for the read, queued
   // or held by the wait: that input runs next and prompts when done; a
   // prompt ahead of it would be taken for the end of what has not run (a
   // notebook chunk's code is a console_input request like any other, and
   // its queue finishes the chunk on the prompt)
   if (console_input::hasPendingInput())
      return false;

   if (busyReported ||
       console_input::promptsIssued() != promptsBefore ||
       console_input::readsCompleted() != readsBefore ||
       console_input::inputDiscards() != discardsBefore)
   {
      console_input::restoreConsolePrompt();
      return true;
   }

   return false;
}

bool parseAndValidateJsonRpcConnection(
         boost::shared_ptr<HttpConnection> ptrConnection,
         json::JsonRpcRequest* pJsonRpcRequest)
{
   // Async rpc requests will have already parsed the request so just copy it over here
   if (ptrConnection->isAsyncRpc())
   {
      boost::shared_ptr<rpc::AsyncRpcConnection> asyncConnection =
              boost::static_pointer_cast<rpc::AsyncRpcConnection>(ptrConnection);
      *pJsonRpcRequest = asyncConnection->jsonRpcRequest();
      return true;
   }

   // attempt to parse the request into a json-rpc request
   Error error = json::parseJsonRpcRequest(ptrConnection->request().body(),
                                           pJsonRpcRequest);
   if (error)
   {
      ptrConnection->sendJsonRpcError(error);
      return false;
   }

   // check for valid CSRF headers in server mode 
   if (options().programMode() == kSessionProgramModeServer && 
       !core::http::validateCSRFHeaders(ptrConnection->request()))
   {
      // CSRF validation failed - check for a signed server bypass header.
      // Server-originated requests (session reaper, workbench API) may not have
      // browser CSRF tokens but carry a signed bypass header instead.
      std::string bypassInfo;
      bool bypassed = false;
#ifdef RSTUDIO_SERVER
      if (options().verifySignatures() && !options().signingKey().empty())
      {
         Error bypassError = server_core::sessions::verifyCSRFBypass(
             options().signingKey(), ptrConnection->request(), &bypassInfo);
         if (!bypassError)
         {
            bypassed = true;
            LOG_DEBUG_MESSAGE("CSRF bypassed for '" + ptrConnection->request().uri() +
                              "' by signed server request (" + bypassInfo + ")");
         }
      }
#endif
      if (!bypassed)
      {
         LOG_WARNING_MESSAGE("RPC request to '" + ptrConnection->request().uri() + "' has missing or "
               "mismatched " + std::string(kCSRFTokenCookie) + " cookie or " +
               std::string(kCSRFTokenHeader) + " header");
         ptrConnection->sendJsonRpcError(Error(json::errc::Unauthorized, ERROR_LOCATION));
         return false;
      }
   }

   // check for invalid client id - must match, unless it's rserver using quit session from the homepage (no client id in that case)
#ifdef RSTUDIO_SERVER
   bool rserverQuitSession = pJsonRpcRequest->clientId == "" && pJsonRpcRequest->method == kQuitSession;
#else
   bool rserverQuitSession = false;
#endif
   if (pJsonRpcRequest->clientId != persistentState().activeClientId() && !rserverQuitSession)
   {
      Error error(json::errc::InvalidClientId, ERROR_LOCATION);
      ptrConnection->sendJsonRpcError(error);
      return false;
   }

   // check for legacy client version (need to invalidate any client using
   // the old version field)
   if ( (pJsonRpcRequest->version > 0) &&
        (s_version > pJsonRpcRequest->version) )
   {
      Error error(json::errc::InvalidClientVersion, ERROR_LOCATION);
      ptrConnection->sendJsonRpcError(error);
      return false;
   }

   // check for client version
   if (!pJsonRpcRequest->clientVersion.empty() &&
       http_methods::clientVersion() != pJsonRpcRequest->clientVersion)
   {
      Error error(json::errc::InvalidClientVersion, ERROR_LOCATION);
      ptrConnection->sendJsonRpcError(error);
      return false;
   }

   // got through all of the validation, return true
   return true;
}

void endHandleConnection(boost::shared_ptr<HttpConnection> ptrConnection,
                         http_methods::ConnectionType connectionType,
                         core::http::Response* pResponse)
{
   ptrConnection->sendResponse(*pResponse);
   if (!console_input::executing())
      module_context::events().onDetectChanges(module_context::ChangeSourceURI);
}

bool isMethod(const std::string& uri, const std::string& method)
{
   return boost::algorithm::ends_with(uri, method);
}

bool isMethod(boost::shared_ptr<HttpConnection> ptrConnection,
              const std::string& method)
{
   return isMethod(ptrConnection->request().uri(), method);
}

Error startHttpConnectionListener()
{
   Error error = initializeHttpConnectionListener();
   if (error)
      return error;

   error = httpConnectionListener().start();
   if (error)
      return error;

   if (options().standalone())
   {
      // log the endpoint to which we have bound to so other services can discover us
      TcpIpHttpConnectionListener& listener =
            static_cast<TcpIpHttpConnectionListener&>(httpConnectionListener());

      boost::asio::ip::tcp::endpoint endpoint = listener.getLocalEndpoint();

      std::string protocol = listener.isSsl() ? " https" : "";

      std::cout << "Listener bound to address " << endpoint.address().to_string()
                << " port " << endpoint.port() << protocol << std::endl;

      // set the standalone port so rpostback and others know how to
      // connect back into the session process
      std::string port = safe_convert::numberToString(endpoint.port());
      core::system::setenv(kRSessionStandalonePortNumber, port);

      // save the standalone port for possible session relaunches - launcher sessions
      // need to rebind to the same port
      persistentState().setReusedStandalonePort(port);
   }

   return Success();
}

bool isWaitForMethodUri(const std::string& uri)
{
   for (const std::string& methodName : s_waitForMethodNames)
   {
      if (isMethod(uri, methodName))
         return true;
   }

   return false;
}

void polledEventHandler()
{
   // if R is getting called after a fork this is likely multicore or
   // some other parallel computing package that uses fork. in this
   // case be defensive by shutting down as many things as we can
   // which might cause mischief in the child process
   if (main_process::wasForked())
   {
      // no more polled events
      rstudio::r::session::event_loop::permanentlyDisablePolledEventHandler();

      // done
      return;
   }

   if (!ASSERT_MAIN_THREAD())
   {
      return;
   }

   // static lastPerformed value used for throttling
   using namespace boost::posix_time;
   static ptime s_lastPerformed;
   if (s_lastPerformed.is_not_a_date_time())
      s_lastPerformed = microsec_clock::universal_time();

   // throttle to no more than once every 50ms
   static time_duration s_intervalMs = milliseconds(50);
   if (microsec_clock::universal_time() <= (s_lastPerformed + s_intervalMs))
      return;

   // notify modules
   module_context::onBackgroundProcessing(false);

   // set last performed (should be set after calling onBackgroundProcessing so
   // that long running background processing handlers can't overflow the 50ms
   // interval between background processing invocations)
   s_lastPerformed = microsec_clock::universal_time();

   // a handler's own console read issued a prompt, which cleared the client's
   // busy state: arm the busy report again for the rest of the handler
   if (s_idleEvents.active &&
       console_input::promptsIssued() != s_idleEvents.promptsIssued)
   {
      s_idleEvents.promptsIssued = console_input::promptsIssued();
      s_idleEvents.busyNotified = false;
      s_idleEvents.started = s_lastPerformed;
   }

   // an input handler that keeps running looks idle to the client (unless
   // the console was busy anyway): say R is busy so it offers the interrupt
   // (undone when the handler returns)
   if (s_idleEvents.active && !s_idleEvents.busyNotified &&
       s_idleEvents.reportsBusy)
   {
      if (!s_idleEvents.busyDelay)
         s_idleEvents.busyDelay = idleEventBusyDelay();

      if (s_lastPerformed - s_idleEvents.started > *s_idleEvents.busyDelay)
      {
         s_idleEvents.busyNotified = true;
         s_idleEvents.busyReported = true;
         ClientEvent busyEvent(client_events::kBusy, true);
         module_context::enqueClientEvent(busyEvent);
      }
   }

   // check for pending connections only while R is running code: processing
   // console input, or an input handler dispatched from the idle loop
   // (otherwise we'll handle them directly in waitForMethod)
   if (console_input::isRunningCode())
   {
      // check the uri of the next connection
      std::string nextConnectionUri =
       httpConnectionListener().mainConnectionQueue().peekNextConnectionUri();
      if (nextConnectionUri.empty())
         return;

      // bail on one of our special waitForMethod calls so that the
      // waitForMethod logic can handle it. console input is not one of
      // them: its registered handler (bufferConsoleInput, SessionMain.cpp)
      // queues it for the next prompt, whether typed ahead of a running
      // command or sent while an input handler runs
      if (isWaitForMethodUri(nextConnectionUri))
         return;

      // attempt to deque a connection and handle it. for now we just handle
      // a single connection at a time (we'll be called back again if processing
      // continues)
      boost::shared_ptr<HttpConnection> ptrConnection =
            httpConnectionListener().mainConnectionQueue().dequeConnection();
      if (ptrConnection)
      {
         // ensure request signature is valid
         if (!http_methods::verifyRequestSignature(ptrConnection->request()))
         {
            LOG_ERROR_MESSAGE("Invalid signature in poll handler for request URI " + ptrConnection->request().uri());
            core::http::Response response;
            response.setError(http::status::Unauthorized, "Invalid message signature");
            ptrConnection->sendResponse(response);
            return;
         }

         if (isMethod(ptrConnection, kClientInit))
         {
            if (ptrConnection->isAsyncRpc())
            {
               BOOST_ASSERT(false); // The listener thread skips client_init when converting RPC to async
            }
            // client_init means the user is attempting to reload the browser
            // in the middle of a computation. process client_init and post
            // a busy event as our initFunction -- or, when the computation
            // is an input handler, with the init of the wait it runs for,
            // as waitForMethod would: the waiting read's prompt, or a
            // dialog's event again. a reissued prompt re-arms the busy
            // report (above), so a handler that runs on is reported again
            using namespace session::module_context;
            if (s_idleEvents.active)
            {
               client_init::handleClientInit(s_idleEvents.clientInit, ptrConnection);
            }
            else
            {
               ClientEvent busyEvent(client_events::kBusy, true);
               client_init::handleClientInit(
                     boost::bind(enqueClientEvent, busyEvent), ptrConnection);
            }
         }
         else
         {
            if (s_protocolDebugEnabled)
            {
               std::chrono::duration<double> duration =
                       std::chrono::steady_clock::now() - ptrConnection->receivedTime();
               LOG_DEBUG_MESSAGE("Handle background: " + ptrConnection->request().uri() +
                                 " after: " + string_utils::formatDouble(duration.count(), 2));
            }
            handleConnection(ptrConnection, http_methods::BackgroundConnection);
         }

         // a request served is activity, as in waitForMethod: console input
         // queued behind an input handler and then discarded (an interrupt)
         // is never handed back, so nothing else would keep an idle suspend
         // from following a handler that outlasted the timeout
         suspend::resetSuspendTimeout(ptrConnection);
      }
   }
}

bool registeredWaitForMethod(const std::string& method,
                             const ClientEvent& event,
                             core::json::JsonRpcRequest* pRequest)
{
   // enque the event which notifies the client we want input
   module_context::enqueClientEvent(event);

   // wait for method
   return http_methods::waitForMethod(method,
                        boost::bind(http_methods::waitForMethodInitFunction, 
                                    event),
                        suspend::disallowSuspend,
                        pRequest);
}


} // anonymous namespace

namespace module_context
{
module_context::WaitForMethodFunction registerWaitForMethod(
      const std::string& methodName)
{
   s_waitForMethodNames.push_back(methodName);
   return boost::bind(registeredWaitForMethod, methodName, _2, _1);
}

} // namespace module_context

namespace http_methods {

bool verifyRequestSignature(const core::http::Request& request)
{
#ifndef RSTUDIO_SERVER
   // signatures only supported in server mode - requires a dependency
   // on server_core, which is only available to server platforms
   return true;
#else
   // only verify signatures if we are in standalone mode and
   // we are explicitly configured to verify signatures
   if (!options().verifySignatures() || !options().standalone())
      return true;

   // determine which signing key pair to use
   // we use an automatically generated key for postback requests
   // (since we do not have access to the private key of rserver)
   std::string signingKey = options().signingKey();
   if (request.headerValue("X-Session-Postback") == "1")
      signingKey = options().sessionRsaPublicKey();

   // ensure specified signature is valid
   // note: we do not validate the signing username if we are running in a root container
   Error error = !core::system::effectiveUserIsRoot() ?
                    server_core::sessions::verifyRequestSignature(signingKey, core::system::username(), request) :
                    server_core::sessions::verifyRequestSignature(signingKey, request);

   if (error)
   {
      LOG_ERROR(error);
      return false;
   }

   return true;
#endif
}


// client version -- this is determined by the git revision hash. the client
// and the server can diverge if a new version of the server was installed
// underneath a previously rendered client. if versions diverge then a reload
// of the client is forced
std::string clientVersion()
{
   // never return a version in desktop mode
   if (options().programMode() == kSessionProgramModeDesktop)
      return std::string();

   // never return a version in standalone mode
   if (options().standalone())
      return std::string();

   // clientVersion is the git revision hash
   return RSTUDIO_GIT_REVISION_HASH;
}

bool protocolDebugEnabled()
{
   return s_protocolDebugEnabled;
}

bool processingIdleEvents()
{
   std::int64_t since = s_idleEventsSince;
   return since != 0 && steadyNowNs() - since > dispatchGraceNs();
}

bool idleEventsPastDispatch()
{
   std::int64_t since = s_idleEventsSince;
   if (since == 0)
      return false;

   // within the grace it is not yet known whether this is a callback or the
   // dispatch alone: wait for the grace to run out while the same dispatch
   // stays active (the main thread cannot wait, it is the dispatch)
   std::int64_t graceNs = dispatchGraceNs();
   while (steadyNowNs() - since <= graceNs)
   {
      std::this_thread::sleep_for(std::chrono::milliseconds(1));
      if (s_idleEventsSince != since)
         return false;
   }

   return true;
}

void waitForMethodInitFunction(const ClientEvent& initEvent)
{
   module_context::enqueClientEvent(initEvent);

   if (console_input::executing())
   {
      ClientEvent busyEvent(client_events::kBusy, true);
      module_context::enqueClientEvent(busyEvent);
   }
   else
   {
      console_input::reissueLastConsolePrompt();
   }
}

// wait for the specified method. will either:
//   - return true and the method request in pRequest
//   - return false indicating failure (e.g. called after fork in child)
//   - suspend or quit the process
// exit the process as a result of suspend or quit)
bool waitForMethod(const std::string& method,
                   const boost::function<void()>& initFunction,
                   const boost::function<bool()>& allowSuspend,
                   core::json::JsonRpcRequest* pRequest)
{
   if (main_process::wasForked())
      return false;
   
   if (!ASSERT_MAIN_THREAD(method))
   {
      return false;
   }

   IdleEventsScope idleEventsSuspended(false);

   // establish timeouts
   suspend::resetSuspendTimeout();
   boost::posix_time::time_duration connectionQueueTimeout =
                                   boost::posix_time::milliseconds(50);

   int checkForIdleCount = 0;

   // If this method (and the provided allowSuspend() function) blocks auto suspension,
   // make sure to record it
   suspend::addBlockingOp(method, allowSuspend);

   // the main thread only begins serving queued requests here; until now any
   // request from the client (including the initial page load) has been
   // waiting in the connection queue
   static bool s_firstWait = true;
   if (s_firstWait)
   {
      s_firstWait = false;
      core::startup_timing::checkpoint("first-wait-for-method");
   }

   // wait until we get the method we are looking for
   while (true)
   {
      // before waiting, suspend if necessary due to forced or requests suspend (does not return if a suspend occurs)
      suspend::checkForRequestedSuspend(allowSuspend);

      // look for a connection (waiting for the specified interval)
      boost::shared_ptr<HttpConnection> ptrConnection =
          httpConnectionListener().mainConnectionQueue().dequeConnection(
                                            connectionQueueTimeout);

      static bool s_firstConnection = true;
      if (ptrConnection && s_firstConnection)
      {
         s_firstConnection = false;
         core::startup_timing::checkpoint("first-request:" + ptrConnection->request().uri());
      }

      if (ptrConnection)
      {
         // ensure request signature is valid
         if (!http_methods::verifyRequestSignature(ptrConnection->request()))
         {
            LOG_ERROR_MESSAGE("Invalid signature for request URI " + ptrConnection->request().uri());
            core::http::Response response;
            response.setError(http::status::Unauthorized, "Invalid message signature");
            ptrConnection->sendResponse(response);
            continue;
         }
      }

      // any request but the one this wait is for is served now, before the
      // input handlers run: held across a long handler it would hang, since
      // the handlers serve only what is still queued (polledEventHandler).
      // the waited-for request stays held meanwhile: a console read a
      // handler opens must not take it (nested reads take their input from
      // the queue), an interrupt meanwhile discards it like queued input,
      // and when handed over it goes ahead of input the handlers queued
      // (see polledEventHandler and rConsoleRead)
      if (ptrConnection && !isMethod(ptrConnection, method))
      {
         // check for client_init
         if (isMethod(ptrConnection, kClientInit))
         {
            client_init::handleClientInit(initFunction, ptrConnection);
         }

         // another connection type, dispatch it
         else
         {
            if (s_protocolDebugEnabled)
            {
               std::chrono::steady_clock::time_point now = std::chrono::steady_clock::now();
               std::chrono::duration<double> beforeTime = now - ptrConnection->receivedTime();
               LOG_DEBUG_MESSAGE("- Handle foreground: " + ptrConnection->request().uri() +
                                 " after: " + string_utils::formatDouble(beforeTime.count(), 2));
               handleConnection(ptrConnection, ForegroundConnection);
               std::chrono::duration<double> afterTime = std::chrono::steady_clock::now() - now;
               LOG_DEBUG_MESSAGE("--- complete:        " + ptrConnection->request().uri() +
                                 " in: " + string_utils::formatDouble(afterTime.count(), 2));
            }
            else
               handleConnection(ptrConnection, ForegroundConnection);
         }

         // since we got a connection we can reset the timeout time
         suspend::resetSuspendTimeout(ptrConnection);

         // after we've processed at least one waitForMethod it is now safe to
         // initialize the polledEventHandler (which is used to maintain rsession
         // responsiveness even when R is executing code received at the console).
         // we defer this to make sure that the FIRST request is always handled
         // by the logic above. if we didn't do this then client_init or
         // console_input (often the first request) could go directly to
         // handleConnection which wouldn't know what to do with them
         if (!rstudio::r::session::event_loop::polledEventHandlerInitialized())
            rstudio::r::session::event_loop::initializePolledEventHandler(
               polledEventHandler);

         ptrConnection.reset();
      }

      // held console input is pending input all the same, to the idle
      // handlers' return and to work that waits for the console to be free
      // (the notebook queue's retry, in the background processing below);
      // it is handed over or discarded after the handlers have run
      bool inputHeld = ptrConnection && method == kConsoleInput;
      if (inputHeld)
         console_input::setInputHeld(true);

      // perform background processing (true for isIdle)
      module_context::onBackgroundProcessing(true);

      // process pending events
      unsigned clearsBefore = console_input::bufferClears();
      bool promptRestored = processIdleEvents(method, initFunction);
      if (inputHeld)
         console_input::setInputHeld(false);

      // a reset that cleared the queue meanwhile (an interrupt, an uncaught
      // error with the discard pref on) discards held input too,
      // acknowledged like the input it was queued behind. the client is
      // busy from its submit with no prompt to follow, unless the handlers'
      // return restored it already
      if (inputHeld && console_input::bufferClears() != clearsBefore)
      {
         json::JsonRpcRequest discarded;
         if (parseAndValidateJsonRpcConnection(ptrConnection, &discarded))
            ptrConnection->sendJsonRpcResponse();
         suspend::resetSuspendTimeout(ptrConnection);
         ptrConnection.reset();

         if (!promptRestored && !console_input::hasPendingInput())
            console_input::restoreConsolePrompt();
      }

      // the method we are waiting on
      if (ptrConnection)
      {
         // parse and validate request then proceed. a held request that
         // fails here (a reload meanwhile changed the client) was still the
         // only thing keeping the waiting read's prompt from being reissued
         // when the handlers returned; it is nobody's input now
         if (inputHeld && !parseAndValidateJsonRpcConnection(ptrConnection, pRequest))
         {
            ptrConnection.reset();
            if (!promptRestored && !console_input::hasPendingInput())
               console_input::restoreConsolePrompt();
         }
         else if (parseAndValidateJsonRpcConnection(ptrConnection, pRequest))
         {
            // respond to the method
            ptrConnection->sendJsonRpcResponse();

            // ensure initialized
            init::ensureSessionInitialized();

            if (s_protocolDebugEnabled && method != kConsoleInput)
            {
               LOG_DEBUG_MESSAGE("Handle wait for:     " + ptrConnection->request().uri());
            }

            // since we got a connection we can reset the timeout time
            suspend::resetSuspendTimeout(ptrConnection);

            if (!rstudio::r::session::event_loop::polledEventHandlerInitialized())
               rstudio::r::session::event_loop::initializePolledEventHandler(
                  polledEventHandler);

            break; // got the method, we are out of here!
         }
      }

      // console input an input handler let into the queue (see
      // polledEventHandler) is what this wait is for; hand it back without
      // a request (rConsoleRead reads the queue). queued input only: what an
      // enclosing read's wait holds is not for a read nested in a handler.
      // checked ahead of the idle suspend below: a suspend with input queued
      // would lose the input
      if (method == kConsoleInput && console_input::hasQueuedInput())
      {
         *pRequest = json::JsonRpcRequest();
         init::ensureSessionInitialized();
         break;
      }

      if (!ptrConnection)
      {
         // avoid calling allowSuspend too often as it iterates through the environment in R
         // looking to be sure all objects are suspendable
         if ((++checkForIdleCount % 20) == 0)
         {
            // timed-out waiting for (usually) console input 20 times (1s)
            suspend::checkForIdleSuspend(allowSuspend);
         }
      }
   }

   suspend::removeBlockingOp(suspend::kGenericMethod + method);

   // satisfied the request
   return true;
}


// wait for the specified method (will either return the method or 
// exit the process as a result of suspend or quit)
bool waitForMethod(const std::string& method,
                   const ClientEvent& initEvent,
                   const boost::function<bool()>& allowSuspend,
                   core::json::JsonRpcRequest* pRequest)
{
   return waitForMethod(method,
                        boost::bind(module_context::enqueClientEvent,
                                    initEvent),
                        allowSuspend,
                        pRequest);
}

bool isJsonRpcRequest(boost::shared_ptr<HttpConnection> ptrConnection)
{
    return boost::algorithm::starts_with(ptrConnection->request().uri(),
                                         "/rpc/");
}

bool isStaticAssetRequest(boost::shared_ptr<HttpConnection> ptrConnection)
{
   // only GET requests that fall through to the GWT file handler: anything
   // with a module-registered handler, and every RPC or event request, needs
   // the main thread (and possibly R)
   if (!s_gwtHandlersRegistered.load(std::memory_order_acquire))
      return false;

   const core::http::Request& request = ptrConnection->request();
   if (request.method() != "GET")
      return false;

   const std::string& uri = request.uri();
   if (boost::algorithm::starts_with(uri, "/rpc/") ||
       boost::algorithm::starts_with(uri, "/events/"))
   {
      return false;
   }

   return !uri_handlers::handlers().handlerFor(uri);
}

void handleStaticAssetRequest(boost::shared_ptr<HttpConnection> ptrConnection)
{
   // serve with the default (GWT file) handler directly. the caller decided
   // via isStaticAssetRequest() that no module handler applies; consulting
   // the handler map again (as handleConnection() would) could pick up a
   // handler registered in between and run it off the main thread
   core::http::Response response;
   s_defaultUriHandler(ptrConnection->request(), &response);
   ptrConnection->sendResponse(response);
}

bool isAsyncJsonRpcRequest(boost::shared_ptr<HttpConnection> ptrConnection)
{
   std::string uri = ptrConnection->request().uri();
   // Excluding the client_init operation because it's one of those special cases that will
   // require more code + testing. Since its only made once per browser process it won't pile up
   return boost::algorithm::starts_with(uri, "/rpc/") && uri != "/rpc/client_init";
}

void handleConnection(boost::shared_ptr<HttpConnection> ptrConnection,
                      ConnectionType connectionType)
{
   // check for a uri handler registered by a module
   const core::http::Request& request = ptrConnection->request();
   std::string uri = request.uri();
   boost::optional<core::http::UriAsyncHandlerFunctionVariant> uriHandler =
     uri_handlers::handlers().handlerFor(uri);

   if (uriHandler) // uri handler
   {
      if (ptrConnection->isAsyncRpc())
      {
         BOOST_ASSERT(false); // should not get here for uri handlers
      }
      core::http::visitHandler(uriHandler.get(),
                               request,
                               boost::bind(endHandleConnection,
                                           ptrConnection,
                                           connectionType,
                                           _1));

      // r code may execute - ensure session is initialized
      init::ensureSessionInitialized();
   }
   else if (isJsonRpcRequest(ptrConnection)) // check for json-rpc
   {
      using namespace module_context;
      // r code may execute - ensure session is initialized
      init::ensureSessionInitialized();

      // attempt to parse & validate
      json::JsonRpcRequest jsonRpcRequest;
      if (parseAndValidateJsonRpcConnection(ptrConnection, &jsonRpcRequest))
      {
         // quit_session: exit process
         if (jsonRpcRequest.method == kQuitSession)
         {
#ifdef _WIN32
            // if we are on windows then we can't quit while the browser
            // context is active
            if (rstudio::r::session::browserContextActive())
            {
               module_context::consoleWriteError(
                        "Error: unable to quit when browser is active\n");
               json::JsonRpcResponse response;
               response.setResult(false);
               ptrConnection->sendJsonRpcResponse(response);
               return;
            }
#endif

            // see whether we should save the workspace
            bool saveWorkspace = true;
            std::string switchToProject, hostPageUrl;
            json::Value switchToVersionJson;
            Error error = json::readParams(jsonRpcRequest.params,
                                           &saveWorkspace,
                                           &switchToProject,
                                           &switchToVersionJson,
                                           &hostPageUrl);
            if (error)
               LOG_ERROR(error);
            
            // note switch to project
            if (!switchToProject.empty())
            {
               // if we don't have a project file then create it (can
               // occur when e.g. opening a project from a directory for
               // which we don't yet have a .Rproj file)
               if (switchToProject != kProjectNone)
               {
                  FilePath projFile = module_context::resolveAliasedPath(switchToProject);
                  if (projFile.getParent().exists() && !projFile.exists())
                  {
                     Error error = r_util::writeProjectFile(
                              projFile,
                              projects::ProjectContext::buildDefaults(),
                              projects::ProjectContext::defaultConfig());
                     if (error)
                        LOG_ERROR(error);
                  }
               }

               // update project and working dir
               if (switchToProject == kProjectNone)
               {
                  // update the project and working dir
                  activeSession().setProject(kProjectNone);
                  activeSession().setWorkingDir(
                    createAliasedPath(dirs::getDefaultWorkingDirectory()));
               }
               else
               {
                  FilePath projFile = module_context::resolveAliasedPath(switchToProject);
                  std::string projDir = createAliasedPath(projFile.getParent());
                  activeSession().setProject(projDir);
                  activeSession().setWorkingDir(projDir);
               }

               if (options().switchProjectsWithUrl())
               {
                  r_util::SessionScope scope;
                  if (switchToProject == kProjectNone)
                  {
                     scope = r_util::SessionScope::projectNone(
                                          options().sessionScope().id());
                  }
                  else
                  {
                     // extract the directory (aliased)
                     FilePath projFile = module_context::resolveAliasedPath(switchToProject);
                     std::string projDir = createAliasedPath(projFile.getParent());
                     scope = r_util::SessionScope::fromProject(
                              projDir,
                              options().sessionScope().id(),
                              filePathToProjectId(options().userScratchPath(),
                                   FilePath(options().getOverlayOption(
                                               kSessionSharedStoragePath))));
                  }

                  // Set project ID for this session
                  activeSession().setProjectId(scope.projectId().id());

                  // set next session url
                  s_nextSessionUrl = r_util::createSessionUrl(hostPageUrl,
                                                              scope);
               }
               else
               {
                  projects::ProjectsSettings(options().userScratchPath()).
                                    setSwitchToProjectPath(switchToProject);
               }

               // note switch to R version if requested
               if (json::isType<json::Object>(switchToVersionJson))
               {
                  std::string version, rHome, label;
                  Error error = json::readObject(
                                            switchToVersionJson.getObject(),
                                            "version", version,
                                            "r_home", rHome,
                                            "label", label);
                  if (!error)
                  {
                     // set version for active session
                     activeSession().setRVersion(version, rHome, label);

                     // if we had a project directory as well then
                     // set it's version (this is necessary because
                     // project versions override session versions)
                     if (switchToProject != kProjectNone)
                     {
                        FilePath projFile = resolveAliasedPath(switchToProject);
                        std::string projDir = createAliasedPath(projFile.getParent());
                        RVersionSettings verSettings(
                                            options().userScratchPath(),
                                            FilePath(options().getOverlayOption(
                                                  kSessionSharedStoragePath)));
                        verSettings.setProjectLastRVersion(projDir,
                                                           module_context::sharedProjectScratchPath(),
                                                           version,
                                                           rHome,
                                                           label);
                     }
                 }
                 else
                    LOG_ERROR(error);
               }
               LOG_DEBUG_MESSAGE("Switching projects: setting activityState to shutting_down");
            }
            else
               LOG_DEBUG_MESSAGE("Quitting session: setting activityState to shutting_down");

            // Mark this session as "Shutting Down" in the homepage (do we need a "SwitchingProjects" state?)
            activeSession().setActivityState(r_util::kActivityStateShuttingDown, true);

            // exit status
            int status = switchToProject.empty() ? EXIT_SUCCESS : EX_CONTINUE;
            if (options().getBoolOverlayOption(kLauncherSessionOption) && (core::system::getenv("RSTUDIO_FORCE_NON_ZERO_EXIT_CODE") != "1"))
            {
               // Avoid generating nonzero exit codes when running under
               // Launcher. Error codes from normal behaviour like this are
               // confusing for Slurm and Kubernetes administrators unfamiliar
               // with our workloads.
               // However, non-zero exit codes are needed by Sagemaker, so we allow
               // them when requested by setting env var RSTUDIO_FORCE_NON_ZERO_EXIT_CODE=1.
               status = EXIT_SUCCESS;
            }

            // acknowledge request & quit session
            json::JsonRpcResponse response;
            response.setResult(true);
            ptrConnection->sendJsonRpcResponse(response);
            rstudio::r::session::quit(saveWorkspace, status); // does not return
         }
         else if (jsonRpcRequest.method == kSuspendSession)
         {
            // stop the offline service thread -- we don't want to service any
            // more incoming requests while preparing to restart
            offlineService().stop();
            
            // check for force
            bool force = true;
            Error error = json::readParams(jsonRpcRequest.params, &force);
            if (error)
               LOG_ERROR(error);

            // acknowledge request and set flags to suspend session
            ptrConnection->sendJsonRpcResponse();
            if (force)
               suspend::handleUSR2(0);
            else
               suspend::handleUSR1(0);
         }

         // interrupt
         else if (jsonRpcRequest.method == kInterrupt)
         {
            // Discard any buffered input
            console_input::clearConsoleInputBuffer();

            // acknowledge request
            ptrConnection->sendJsonRpcResponse();

            // only accept interrupts while R is running code: processing
            // input, or an input handler dispatched from the idle loop
            if (console_input::isRunningCode())
               rstudio::r::exec::setInterruptsPending(true);
         }
         
         // ping -- don't respond if we're shutting down; assume the ping
         // was intended for a session which will soon be launched anew
         else if (jsonRpcRequest.method == kPing)
         {
            if (s_shuttingDown)
            {
               Error error = systemError(boost::system::errc::connection_refused, ERROR_LOCATION);
               ptrConnection->sendJsonRpcError(error);
            }
            else
            {
               ptrConnection->sendJsonRpcResponse();
            }
         }

         // other rpc method, handle it
         else
         {
            jsonRpcRequest.isBackgroundConnection =
                  (connectionType == BackgroundConnection);
            rpc::handleRpcRequest(jsonRpcRequest, ptrConnection, connectionType);
         }
      }
   }
   else if (s_defaultUriHandler)
   {
      core::http::Response response;
      s_defaultUriHandler(request, &response);
      ptrConnection->sendResponse(response);
   }
   else
   {
      core::http::Response response;
      response.setNotFoundError(request);
      ptrConnection->sendResponse(response);
   }
}

boost::shared_ptr<HttpConnection> handleAsyncRpc(boost::shared_ptr<HttpConnection> ptrConnection)
{
   json::JsonRpcRequest jsonRpcRequest;
   boost::shared_ptr<HttpConnection> res;
   if (parseAndValidateJsonRpcConnection(ptrConnection, &jsonRpcRequest))
   {
      std::string asyncHandle = core::system::generateUuid(true);
      // This will eventually call sendResponse and close ptrConnection
      rpc::sendJsonAsyncPendingResponse(jsonRpcRequest, ptrConnection, asyncHandle);

      res.reset(new rpc::AsyncRpcConnection(ptrConnection, jsonRpcRequest, asyncHandle));
   }
   // else - an invalid request - an http error response has already been sent so return nothing
   return res;
}

WaitResult startHttpConnectionListenerWithTimeout()
{
   Error error = startHttpConnectionListener();

   // When the rsession restarts, it may take a few ms for the port to become
   // available; therefore, retry connection, but only for address_in_use error
   if (!error)
       return WaitResult(WaitSuccess, Success());
   else if (error != boost::system::error_code(boost::system::errc::address_in_use, boost::system::generic_category()))
      return WaitResult(WaitError, error);
   else
      return WaitResult(WaitContinue, error);
}


void registerGwtHandlers()
{
   // called once, before the HTTP listener starts, so the listener thread can
   // serve the client while R initializes (see isStaticAssetRequest)
   static bool s_registered = false;
   if (s_registered)
      return;
   s_registered = true;

   // alias options
   session::Options& options = session::options();

   // establish logging handler
   module_context::registerUriHandler(
         "/log",
         boost::bind(gwt::handleLogRequest, options.userIdentity(), _1, _2));

   // establish progress handler
   FilePath wwwPath(options.wwwLocalPath());
   FilePath progressPagePath = wwwPath.completePath("progress.htm");
   module_context::registerUriHandler(
         "/progress",
          boost::bind(text::handleTemplateRequest, progressPagePath, _1, _2));

   // initialize gwt symbol maps
   gwt::initializeSymbolMaps(options.wwwSymbolMapsPath());

   // declare program mode in JS init block (for GWT boot time access)
   std::string initJs = "window.program_mode = \"" + options.programMode() + "\";\n";

   // set default handler; the desktop talks to us over loopback, where
   // compressing the multi-megabyte client costs far more than sending it
   bool compress = options.programMode() != kSessionProgramModeDesktop;
   s_defaultUriHandler = gwt::fileHandlerFunction(options.wwwLocalPath(),
                                                  "/",
                                                  http::UriFilterFunction(),
                                                  initJs,
                                                  std::string(),
                                                  false,
                                                  std::string(),
                                                  std::string(),
                                                  compress);

   // publish the handler to the listener thread (see isStaticAssetRequest)
   s_gwtHandlersRegistered.store(true, std::memory_order_release);
}

namespace {

/**
 * When a session debug logging preference is enabled, either create a new FileLog or update an existing one
 * to have DEBUG so these messages show up. This prevents the user from needing to configure logging.conf
 * for the session just to turn on debug logging.
 */
void initSessionDebugLog()
{
   if (s_sessionDebugLogCreated)
      return;
   s_sessionDebugLogCreated = true;

   system::initFileLogDestination(log::LogLevel::DEBUG, core::system::xdg::userLogDir());
}

void onUserPrefsChanged(const std::string& layer, const std::string& pref)
{
   if (pref == kSessionProtocolDebug)
   {
      bool newVal = prefs::userPrefs().sessionProtocolDebug();
      if (newVal != s_protocolDebugEnabled)
      {
         s_protocolDebugEnabled = newVal;
         if (newVal)
             initSessionDebugLog();
      }
   }
}

} // end anonymous namespace

core::Error initialize()
{
   s_protocolDebugEnabled = prefs::userPrefs().sessionProtocolDebug();
   if (s_protocolDebugEnabled)
      initSessionDebugLog();
   
   module_context::events().onShutdown.connect(onShutdown);
   prefs::userPrefs().onChanged.connect(onUserPrefsChanged);
   
   return Success();
}

std::string nextSessionUrl()
{
   return s_nextSessionUrl;
}

} // namespace http_methods
} // namespace session
} // namespace rstudio

