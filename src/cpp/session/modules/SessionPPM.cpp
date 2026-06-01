/*
 * SessionPPM.cpp
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

#include "SessionPPM.hpp"

#include <functional>

#include <boost/bind/bind.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/json/Json.hpp>

#include <core/Exec.hpp>
#include <core/http/Request.hpp>
#include <core/http/Response.hpp>
#include <core/http/TcpIpAsyncClient.hpp>
#include <core/http/TcpIpAsyncClientSsl.hpp>
#include <core/http/URL.hpp>

#include <r/RExec.hpp>
#include <r/RJson.hpp>
#include <r/RRoutines.hpp>
#include <r/RSexp.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionServerRpc.hpp>

using namespace rstudio::core;
using namespace boost::placeholders;

#define kPwbPpmIntegrationEnabled  "PWB_PPM_INTEGRATION_ENABLED"
#define kPwbPpmRepoUrl             "PWB_PPM_REPO_URL"
#define kPwbPpmMetadataKey         "PWB_PPM_METADATA_KEY"
#define kPwbPpmMetadataColumnLabel "PWB_PPM_METADATA_COLUMN_LABEL"

namespace rstudio {
namespace session {
namespace modules {
namespace ppm {

std::string getPpmRepositoryUrl()
{
   return core::system::getenv(kPwbPpmRepoUrl);
}

std::string getPpmMetadataKey()
{
   std::string key;

   // primarily for testing
   key = core::system::getenv(kPwbPpmMetadataKey);
   if (!key.empty())
      return key;

   // otherwise, read from session options
   return session::options().getOverlayOption("posit-package-manager-metadata-key");
}

std::string getPpmMetadataColumnLabel()
{
   std::string label;

   // primarily for testing
   label = core::system::getenv(kPwbPpmMetadataColumnLabel);
   if (!label.empty())
      return label;

   // otherwise, read from session options
   label = session::options().getOverlayOption("posit-package-manager-metadata-key-display-name");
   if (!label.empty())
      return label;

   // if nothing else provided, just use a default label
   return "Metadata";
}

bool isPpmIntegrationEnabled()
{
   // primarily for testing
   std::string enabled = core::system::getenv(kPwbPpmIntegrationEnabled);
   if (!enabled.empty())
      return string_utils::isTruthy(enabled);

   // otherwise, assume integration is enabled if a repository URL was provided
   std::string url = core::system::getenv(kPwbPpmRepoUrl);
   return !url.empty();
}

bool isPpmMetadataColumnEnabled()
{
   if (!isPpmIntegrationEnabled())
      return false;

   std::string key = getPpmMetadataKey();
   if (key.empty())
      return false;

   std::string label = getPpmMetadataColumnLabel();
   if (label.empty())
      return false;

   return true;
}

namespace {

// Cap how long we'll wait to connect to PPM. The whole point of moving this
// off the main thread is so a slow or unreachable PPM never blocks the IDE;
// keep the timeout short so a dead endpoint fails fast and quietly.
const boost::posix_time::time_duration kConnectionTimeout =
   boost::posix_time::seconds(10);

// Number of vulnerability requests currently in flight, plus a flag noting
// that another refresh was requested while requests were outstanding. Both are
// only ever touched on the main thread.
int s_activeRequests = 0;
bool s_refreshPending = false;

void startVulnerabilityRequests();
void enqueCachedVulnerabilities();

// Runs on the main thread once a single repo's request has completed (whether
// it succeeded or failed). On success, fold the response body into the per-repo
// cache. We publish the aggregated result to the client once the whole batch
// drains (see below) rather than per response, so that even a batch where every
// request fails still produces a deterministic update -- otherwise the Packages
// pane could keep showing stale badges after a repo/package change whose fetch
// failed.
void onVulnerabilityRequestComplete(const std::string& repoUrl,
                                    bool succeeded,
                                    const std::string& body)
{
   if (succeeded)
   {
      Error error = r::exec::RFunction(".rs.ppm.recordVulnerabilityResponse")
                       .addParam(repoUrl)
                       .addParam(body)
                       .call();
      if (error)
         LOG_ERROR(error);
   }

   if (s_activeRequests > 0)
      s_activeRequests--;

   // not done yet -- wait for the remaining requests in this batch
   if (s_activeRequests > 0)
      return;

   // the batch has drained; if another refresh came in while we were busy run
   // it now, otherwise publish the (possibly unchanged) cached set so the UI is
   // always updated deterministically -- including when every request failed
   if (s_refreshPending)
   {
      s_refreshPending = false;
      startVulnerabilityRequests();
   }
   else
   {
      enqueCachedVulnerabilities();
   }
}

void onVulnerabilityResponse(const std::string& repoUrl,
                             const std::string& endpoint,
                             const http::Response& response)
{
   bool ok = response.statusCode() >= 200 && response.statusCode() < 300;
   if (!ok)
   {
      LOG_ERROR_MESSAGE("PPM vulnerability request to " + endpoint +
                        " returned HTTP " +
                        safe_convert::numberToString(response.statusCode()));
   }

   std::string body = response.body();
   module_context::executeOnMainThread(
      boost::bind(onVulnerabilityRequestComplete, repoUrl, ok, body));
}

void onVulnerabilityError(const std::string& repoUrl,
                          const std::string& endpoint,
                          const Error& error)
{
   LOG_ERROR_MESSAGE("PPM vulnerability request to " + endpoint +
                     " failed: " + error.getSummary());
   module_context::executeOnMainThread(
      boost::bind(onVulnerabilityRequestComplete, repoUrl, false, std::string()));
}

// Issue one async POST for a single repo's request-plan entry.
void sendVulnerabilityRequest(const json::Object& entry)
{
   std::string repoUrl, endpoint, authHeader, body;
   Error error = json::readObject(entry,
                                  "repoUrl", repoUrl,
                                  "endpoint", endpoint,
                                  "authHeader", authHeader,
                                  "body", body);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   // https is the norm, but the legacy curl path also worked against http PPM
   // deployments, so keep supporting both rather than silently dropping vuln
   // checks for an http-configured repository
   http::URL url(endpoint);
   bool useSsl = url.protocol() == "https";
   if (!url.isValid() || (!useSsl && url.protocol() != "http"))
   {
      LOG_ERROR_MESSAGE("PPM vulnerability endpoint is not a valid http(s) URL: " + endpoint);
      return;
   }

   http::Request request;
   request.setMethod("POST");
   request.setUri(url.path());
   request.setHost(url.hostWithPort());
   request.setHeader("Connection", "close");
   request.setContentType("application/json");
   if (!authHeader.empty())
      request.setHeader("Authorization", authHeader);
   request.setBody(body);

   boost::shared_ptr<http::IAsyncClient> pClient;
   if (useSsl)
   {
      pClient.reset(new http::TcpIpAsyncClientSsl(server_rpc::ioContext(),
                                                  url.hostname(),
                                                  url.portStr(),
                                                  true, // verify certificates
                                                  std::string(), // cert authority
                                                  kConnectionTimeout));
   }
   else
   {
      pClient.reset(new http::TcpIpAsyncClient(server_rpc::ioContext(),
                                               url.hostname(),
                                               url.portStr(),
                                               kConnectionTimeout));
   }

   pClient->request().assign(request);

   s_activeRequests++;
   pClient->execute(
      boost::bind(onVulnerabilityResponse, repoUrl, endpoint, _1),
      boost::bind(onVulnerabilityError, repoUrl, endpoint, _1));
}

// Forward the currently-cached vulnerability data to the client without making
// any network request. Used when there's nothing new to fetch, so that (for
// example) switching from a PPM repo to a non-PPM repo clears stale badges.
void enqueCachedVulnerabilities()
{
   r::sexp::Protect protect;
   SEXP vulnsSEXP = R_NilValue;
   Error error = r::exec::RFunction(".rs.ppm.getCachedVulnerabilities")
                    .call(&vulnsSEXP, &protect);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   json::Value vulnsJson;
   error = r::json::jsonValueFromObject(vulnsSEXP, &vulnsJson);
   if (error)
      LOG_ERROR(error);
   else
      module_context::enqueClientEvent(
         ClientEvent(client_events::kPackageVulnerabilitiesReady, vulnsJson));
}

// Ask R for the request plan (what to fetch + how to authenticate) and fire off
// an async request for each repo that has uncached packages. Runs on the main
// thread.
void startVulnerabilityRequests()
{
   r::sexp::Protect protect;
   SEXP planSEXP = R_NilValue;
   Error error = r::exec::RFunction(".rs.ppm.getVulnerabilityRequestPlan")
                    .call(&planSEXP, &protect);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   json::Value planJson;
   error = r::json::jsonValueFromObject(planSEXP, &planJson);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   json::Array plan = planJson.isArray() ? planJson.getArray() : json::Array();

   // nothing new to fetch -- still publish what we have cached so the client
   // reflects the current repository state (e.g. clears badges after a switch
   // to a non-PPM repo)
   if (plan.isEmpty())
   {
      enqueCachedVulnerabilities();
      return;
   }

   for (const json::Value& entry : plan)
   {
      if (entry.isObject())
         sendVulnerabilityRequest(entry.getObject());
   }
}

SEXP rs_ppmIntegrationEnabled()
{
   return isPpmIntegrationEnabled() ? Rf_ScalarLogical(TRUE) : Rf_ScalarLogical(FALSE);
}

SEXP rs_ppmMetadataColumnEnabled()
{
   return isPpmMetadataColumnEnabled() ? Rf_ScalarLogical(TRUE) : Rf_ScalarLogical(FALSE);
}

SEXP rs_ppmMetadataKey()
{
   r::sexp::Protect protect;
   return r::sexp::create(getPpmMetadataKey(), &protect);
}

} // end anonymous namespace

void refreshVulnerabilitiesAsync()
{
   if (!isPpmIntegrationEnabled())
      return;

   // coalesce: if requests are still in flight, defer until they drain so the
   // refresh reflects the latest installed-package / repo state
   if (s_activeRequests > 0)
   {
      s_refreshPending = true;
      return;
   }

   startVulnerabilityRequests();
}

Error initialize()
{
   using std::bind;
   using namespace module_context;

   RS_REGISTER_CALL_METHOD(rs_ppmIntegrationEnabled);
   RS_REGISTER_CALL_METHOD(rs_ppmMetadataColumnEnabled);
   RS_REGISTER_CALL_METHOD(rs_ppmMetadataKey);

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(sourceModuleRFile, "SessionPPM.R"));

   return initBlock.execute();
}

} // namespace ppm
} // namespace modules
} // namespace session
} // namespace rstudio


