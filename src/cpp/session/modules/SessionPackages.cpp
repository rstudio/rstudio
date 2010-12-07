/*
 * SessionPackages.cpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include "SessionPackages.hpp"

#include <boost/bind.hpp>
#include <boost/regex.hpp>

#include <core/Error.hpp>
#include <core/Exec.hpp>
#include <core/http/URL.hpp>
#include <core/http/TcpIpBlockingClient.hpp>

#include <r/RSexp.hpp>
#include <r/RExec.hpp>
#include <r/RFunctionHook.hpp>

#include <session/SessionModuleContext.hpp>

using namespace core;

namespace session {
namespace modules { 
namespace packages {

namespace {

Error availablePackagesBegin(const core::json::JsonRpcRequest& request,
                             std::string* pContribUrl)
{
   return r::exec::evaluateString<std::string>(
         "contrib.url(getOption('repos'), getOption('pkgType'))",
         pContribUrl);
}

Error availablePackagesEnd(const core::json::JsonRpcRequest& request,
                           const std::string& contribUrl,
                           core::json::JsonRpcResponse* pResponse)
{
   http::URL url(contribUrl + "/PACKAGES");
   http::Request pkgRequest;
   pkgRequest.setMethod("GET");
   pkgRequest.setHost(url.hostname());
   pkgRequest.setUri(url.path());
   pkgRequest.setHeader("Accept", "*/*");
   pkgRequest.setHeader("Connection", "close");
   http::Response pkgResponse;

   Error error = http::sendRequest(url.hostname(),
                                   boost::lexical_cast<std::string>(url.port()),
                                   pkgRequest,
                                   &pkgResponse);

   if (error)
      return error;

   if (pkgResponse.statusCode() == 200)
   {
      std::string body = pkgResponse.body();
      boost::regex re("^Package:\\s*([^\\s]+?)\\s*$");

      boost::sregex_iterator matchBegin(body.begin(), body.end(), re);
      boost::sregex_iterator matchEnd;
      std::vector<std::string> results;
      for (; matchBegin != matchEnd; matchBegin++)
         results.push_back((*matchBegin)[1]);

      json::Array jsonResults;
      for (size_t i = 0; i < results.size(); i++)
         jsonResults.push_back(results.at(i));

      pResponse->setResult(jsonResults);
      return Success();
   }
   else
   {
      std::string msg = "Could not retrieve " + contribUrl + ", http status: " +
                        boost::lexical_cast<std::string>(pkgResponse.statusCode());

      return systemError(boost::system::errc::protocol_error,
                         msg,
                         ERROR_LOCATION);
   }
}

} // anonymous namespace

Error initialize()
{
   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcAsyncCoupleMethod<std::string>,
            "available_packages",
            availablePackagesBegin,
            availablePackagesEnd))
      (bind(sourceModuleRFile, "SessionPackages.R"))
      (bind(r::exec::executeString, ".rs.packages.initialize()"));
   return initBlock.execute();
}


} // namespace packages
} // namespace modules
} // namesapce session

