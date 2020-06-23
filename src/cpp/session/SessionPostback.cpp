/*
 * SessionPostback.cpp
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

/* Postback Handlers

Postback handlers are generally used for situations where R options call for
an external executable rather than a function. For example, the R action
for viewing PDFs, options("pdfviewer"), is handle using a postback handler
named 'pdfviewer'.

To create a new postback handler for an action 'foo' do the following:
 
1) Create a shell script named 'rpostback-foo' (based on rpostback-pdfviewer)
 
2) Ensure that the shell script is included in the installation
 
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

#include <shared_core/Error.hpp>
#include <shared_core/SafeConvert.hpp>

#include <core/http/Request.hpp>
#include <core/http/Response.hpp>

#include <session/SessionModuleContext.hpp>

#include <session/SessionOptions.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {  
namespace module_context {

namespace {  
      
std::map<std::string,
         module_context::PostbackHandlerFunction> s_postbackHandlers;

void endHandlePostback(const http::UriHandlerFunctionContinuation& cont,
                       int exitCode,
                       const std::string& output)
{
   http::Response response;
   // send basic response
   response.setStatusCode(http::status::Ok);
   response.setContentType("text/plain");
   response.setHeader(kPostbackExitCodeHeader,
                        safe_convert::numberToString(exitCode));
   response.setBody(output);
   cont(&response);
}

// UriHandlerFunction wrapper for simple postbacks
void handlePostback(const PostbackHandlerFunction& handlerFunction,
                    const http::Request& request, 
                    const http::UriHandlerFunctionContinuation& cont)
{
   // pass the body to the postback function
   handlerFunction(request.body(), boost::bind(endHandlePostback, cont, _1, _2));
}
   
} // anonymous namespace


Error registerPostbackHandler(const std::string& name,
                              const PostbackHandlerFunction& handlerFunction,
                              std::string* pShellCommand)
{
   // form postback uri fragment
   std::string postback = kPostbackUriScope + name;
   
   // register a uri handler for this prefix
   Error error = module_context::registerAsyncLocalUriHandler(
                    postback,
                    boost::bind(handlePostback, handlerFunction, _1, _2));
   if (error)
      return error;
                                                    
   // compute the shell command required to invoke this handler and return it
   Options& options = session::options();
   *pShellCommand = options.rpostbackPath().getAbsolutePath() + "-" + name;
   
   // return success
   return Success();
}
   
   
   
} // namespace module_context
} // namespace session
} // namespace rstudio
