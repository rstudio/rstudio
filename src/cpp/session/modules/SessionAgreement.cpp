/*
 * SessionAgreement.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include "SessionAgreement.hpp"

#include <string>

#include <boost/function.hpp>

#include <core/Log.hpp>
#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/Hash.hpp>
#include <core/Exec.hpp>
#include <core/FileSerializer.hpp>

#include <core/http/Request.hpp>
#include <core/http/Response.hpp>

#include <session/SessionOptions.hpp>
#include <session/SessionUserSettings.hpp>
#include <session/SessionModuleContext.hpp>

using namespace core ;

namespace session {  
namespace modules {
namespace agreement {

namespace {
   
struct Agreement
{
   Agreement() : updated(false) {}
   Agreement(const std::string& title, 
             const std::string& contents,
             const std::string& hash,
             bool updated)
      : title(title), contents(contents), hash(hash), updated(updated)
   {
   }
   
   bool empty() const { return hash.empty(); }
   
   const std::string title;
   const std::string contents;
   const std::string hash;
   const bool updated ;
};
   
Error agreementFileContents(std::string* pContents, std::string* pContentType)
{
   FilePath agreementFilePath = session::options().agreementFilePath();
   *pContentType = agreementFilePath.mimeContentType();
   return readStringFromFile(agreementFilePath, pContents);
}

Agreement checkForPendingAgreement()
{
   // get hash of any previously agreed to agreement
   std::string agreedToHash = userSettings().agreementHash();
   
   // read agreement file contents
   std::string contents, contentType ;
   Error error = agreementFileContents(&contents, &contentType);
   if (error)
   {
      LOG_ERROR(error);
      return Agreement();
   }
   
   // hash: filename + crc32 checksum of contents
   std::string hash = session::options().agreementFilePath().filename() +
                      hash::crc32Hash(contents);
   
   // see if we have not yet agreed to this agreement
   if (hash != agreedToHash)
   {
      // set updated flag based on whether there was a previous agreement
      bool updated = !agreedToHash.empty();
      
      // return the pending agreement
      return Agreement("RStudio Agreement",
                       contents,
                       hash,
                       updated);
   }
   else
   {
      // no agreement pending, return empty Agreement
      return Agreement();
   }
}
   
Error handleAcceptAgreement(const json::JsonRpcRequest& request,
                            json::JsonRpcResponse* pResponse)
{
   // read hash param
   std::string hash ;
   Error paramError = json::readParam(request.params, 0, &hash);
   if (paramError)
      return paramError;
   
   // set it
   userSettings().setAgreementHash(hash);
   
   // return success
   return Success();
}
   
void handleAgreementRequest(const http::Request& request,
                            http::Response* pResponse)
{
   // attempt to read the agreement
   std::string agreementContents, contentType;
   Error error = agreementFileContents(&agreementContents, &contentType);
   if (error)
   {
      pResponse->setError(error);
      return;
   }
   
   // set it as our response
   pResponse->setNoCacheHeaders();
   pResponse->setContentType(contentType);
   pResponse->setBody(agreementContents);
}   
   

} // anonymous namespace

bool hasAgreement()
{
   return !session::options().agreementFilePath().empty();
}

json::Value pendingAgreement()
{
   if ( hasAgreement() &&
        (session::options().programMode() == kSessionProgramModeServer) )
   {
      Agreement agreement = checkForPendingAgreement();
      if (!agreement.empty())
      {
         json::Object jsonAgreement;
         jsonAgreement["title"] = agreement.title;
         jsonAgreement["contents"] = agreement.contents;
         jsonAgreement["hash"] = agreement.hash;
         jsonAgreement["updated"] = agreement.updated;
         return jsonAgreement;
      }
      else
      {
         return json::Value();
      }
   }
   else
   {
      return json::Value();
   }
}
   
Error initialize()
{
   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "accept_agreement", handleAcceptAgreement))
      (bind(registerUriHandler, "/agreement", handleAgreementRequest))
   ;

   return initBlock.execute();
}


} // namespace agreement
} // namespace modules
} // namespace session
