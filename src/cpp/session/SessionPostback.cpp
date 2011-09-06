/*
 * SessionPostback.cpp
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

/* Postback Handlers

Postback handlers are generally used for situations where R options call for
an external executable rather than a function. For example, the R action
for viewing PDFs, options("pdfviewer"), is handle using a postback handler
named 'pdfviewer'.

To create a new postback handler for an action 'foo' do the following:
 
1) Create a shell script named 'rpostback-foo' (based on rpostback-pdfviewer)
 
2) Ensure that the shell script is included in the installation and registered
   in the rsession apparmor profile
 
3) Call module_context::registerPostbackHandler with 'foo' as the name param
   and the function you want called during the postback. The function will
   be passed a single parameter corresponding to the first command line 
   argument passed by R to the shell script. 
 
4) The registration function uses the pShellCommand out param to provide you
   with the shell command which you in turn provide to R. 

*/

#include <string>
#include <map>

#include <boost/function.hpp>

#include <core/Error.hpp>
#include <core/SafeConvert.hpp>

#include <core/http/Request.hpp>
#include <core/http/Response.hpp>

#include <session/SessionModuleContext.hpp>

#include <session/SessionOptions.hpp>

using namespace core ;

namespace session {  
namespace module_context {

namespace {  
      
std::map<std::string,
         module_context::PostbackHandlerFunction> s_postbackHandlers;

// UriHandlerFunction wrapper for simple postbacks
void handlePostback(const PostbackHandlerFunction& handlerFunction,
                    const http::Request& request, 
                    http::Response* pResponse)
{
   // pass the body to the postback function
   int exitCode;
   std::string output;
   exitCode = handlerFunction(request.body(), &output);
   
   // send basic response
   pResponse->setStatusCode(http::status::Ok);
   pResponse->setContentType("text/plain");
   pResponse->setHeader(kPostbackExitCodeHeader,
                        boost::lexical_cast<std::string>(exitCode));
   pResponse->setBody(output);
}
   
} // anonymous namespace


Error registerPostbackHandler(const std::string& name,
                              const PostbackHandlerFunction& handlerFunction,
                              std::string* pShellCommand)
{
   // form postback uri fragment
   std::string postback = kPostbackUriScope + name;
   
   // register a uri handler for this prefix
   Error error = module_context::registerLocalUriHandler(
                    postback,
                    boost::bind(handlePostback, handlerFunction, _1, _2));
   if (error)
      return error ;
                                                    
   // compute the shell command required to invoke this handler and return it
   Options& options = session::options();
   *pShellCommand = options.rpostbackPath().absolutePath() + "-" + name ;
   
   // return success
   return Success();
}
   
   
   
} // namespace module_context
} // namespace session
