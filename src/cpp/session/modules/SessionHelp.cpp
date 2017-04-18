/*
 * SessionHelp.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#include "SessionHelp.hpp"

#include <algorithm>

#include <boost/regex.hpp>
#include <boost/function.hpp>
#include <boost/format.hpp>
#include <boost/range/iterator_range.hpp>
#include <boost/algorithm/string/replace.hpp>
#include <boost/algorithm/string.hpp>
#include <boost/iostreams/filter/aggregate.hpp>

#include <core/Error.hpp>
#include <core/Exec.hpp>
#include <core/Log.hpp>

#include <core/http/Request.hpp>
#include <core/http/Response.hpp>
#include <core/http/URL.hpp>
#include <core/FileSerializer.hpp>
#include <core/system/Process.hpp>
#include <core/system/ShellUtils.hpp>
#include <core/r_util/RPackageInfo.hpp>

#define R_INTERNAL_FUNCTIONS
#include <r/RInternal.hpp>
#include <r/RSexp.hpp>
#include <r/RExec.hpp>
#include <r/RFunctionHook.hpp>
#include <r/ROptions.hpp>
#include <r/RUtil.hpp>
#include <r/RRoutines.hpp>
#include <r/session/RSessionUtils.hpp>

#include <session/SessionModuleContext.hpp>

#include "presentation/SlideRequestHandler.hpp"

#include "SessionHelpHome.hpp"

// protect R against windows TRUE/FALSE defines
#undef TRUE
#undef FALSE

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace help {

namespace {   

// save computed help url prefix for comparison in rHelpUrlHandler
const char * const kHelpLocation = "/help";
const char * const kCustomLocation = "/custom";
const char * const kSessionLocation = "/session";

// flag indicating whether we should send headers to custom handlers
// (only do this for 2.13 or higher)
bool s_provideHeaders = false;

// are we handling custom urls internally or allowing them to
// show in an external browser
bool s_handleCustom = false;

std::string localURL(const std::string& address, const std::string& port)
{
   return "http://" + address + ":" + port + "/";
}

std::string replaceRPort(const std::string& url, const std::string& rPort)
{
   std::string newUrl = url;
   boost::algorithm::replace_last(newUrl, rPort, session::options().wwwPort());
   return newUrl;
}

bool isLocalURL(const std::string& url,
                const std::string& scope,
                std::string* pLocalURLPath = NULL)
{
   // first look for local ip prefix
   std::string rPort = module_context::rLocalHelpPort();
   std::string urlPrefix = localURL("127.0.0.1", rPort);
   size_t pos = url.find(urlPrefix + scope);
   if (pos != std::string::npos)
   {
      std::string relativeUrl = url.substr(urlPrefix.length());
      if (pLocalURLPath)
         *pLocalURLPath = replaceRPort(relativeUrl, rPort);
      return true;
   }

   // next look for localhost
   urlPrefix = localURL("localhost", rPort);
   pos = url.find(urlPrefix + scope);
   if (pos != std::string::npos)
   {
      std::string relativeUrl = url.substr(urlPrefix.length());
      if (pLocalURLPath)
         *pLocalURLPath = replaceRPort(relativeUrl, rPort);
      return true;
   }

   // none found
   return false;
}


// hook the browseURL function to look for calls to the R internal http
// server. for custom URLs remap the address to remote and then fire
// the browse_url event. for help URLs fire the appropraite show_help event
bool handleLocalHttpUrl(const std::string& url)
{
   // check for custom
   std::string customPath;
   if (isLocalURL(url, "custom", &customPath))
   {
      if (s_handleCustom)
      {
         ClientEvent event = browseUrlEvent(customPath);
         module_context::enqueClientEvent(event);
         return true;
      }
      else // leave alone (show in external browser)
      {
         return false;
      }
   }

   // check for session
   std::string sessionPath;
   if (isLocalURL(url, "session", &sessionPath))
   {
      if (s_handleCustom)
      {
         ClientEvent event = browseUrlEvent(sessionPath);
         module_context::enqueClientEvent(event);
         return true;
      }
      else // leave alone (show in external browser)
      {
         return false;
      }
   }

   // leave portmapped urls alone
   if (isLocalURL(url, "p/"))
   {
      return false;
   }

   // otherwise look for help (which would be all other localhost urls)
   std::string helpPath;
   if (isLocalURL(url, "", &helpPath))
   {
      helpPath = "help/" + helpPath;
      ClientEvent helpEvent(client_events::kShowHelp, helpPath);
      module_context::enqueClientEvent(helpEvent);
      return true;
   }

   // other localhost URLs can benefit from port mapping -- we map them
   // all since if we don't do any mapping they'll just fail hard
   if (session::options().programMode() == kSessionProgramModeServer)
   {
      // see if we can form a portmap path for this url
      std::string path;
      if (module_context::portmapPathForLocalhostUrl(url, &path))
      {
         module_context::enqueClientEvent(browseUrlEvent(path));
         return true;
      }
   }

   // wasn't a url of interest
   return false;
}
   
// As of R 2.10 RShowDoc still uses the legacy file::// mechanism for
// displaying the manual. Redirect these to the appropriate help event
bool handleRShowDocFile(const core::FilePath& filePath)
{
   std::string absPath = filePath.absolutePath();
   boost::regex manualRegx(".*/lib/R/(doc/manual/[A-Za-z0-9_\\-]*\\.html)");
   boost::smatch match;
   if (regex_utils::match(absPath, match, manualRegx))
   {
      ClientEvent helpEvent(client_events::kShowHelp, match[1]);
      module_context::enqueClientEvent(helpEvent);
      return true;
   }
   else
   {
      return false;
   }
}

// javascript callbacks to inject into page
const char * const kJsCallbacks =
      "<script type=\"text/javascript\">\n"
      "if (window.parent.helpNavigated)\n"
      "   window.parent.helpNavigated(document, window);\n"
      "if (window.parent.helpKeydown)\n"
      "   window.onkeydown = function(e) {window.parent.helpKeydown(e);}\n"
      "</script>\n";


   
class HelpContentsFilter : public boost::iostreams::aggregate_filter<char>
{
public:
   typedef std::vector<char> Characters ;

   HelpContentsFilter(const http::Request& request)
   {
      requestUri_ = request.uri();
   }

   void do_filter(const Characters& src, Characters& dest)
   {
      std::string baseUrl = http::URL::uncomplete(
            requestUri_,
            kHelpLocation);

      // fixup hard-coded hrefs
      Characters tempDest1;
      boost::algorithm::replace_all_copy(
            std::back_inserter(tempDest1),
            boost::make_iterator_range(src.begin(), src.end()),
            "href=\"/",
            "href=\"" + baseUrl + "/");
      Characters tempDest2;
      boost::algorithm::replace_all_copy(
            std::back_inserter(tempDest2),
            boost::make_iterator_range(tempDest1.begin(), tempDest1.end()),
            "href='/",
            "href='" + baseUrl + "/");
      
      // fixup hard-coded src=
      Characters tempDest3;
      boost::algorithm::replace_all_copy(
            std::back_inserter(tempDest3),
            boost::make_iterator_range(tempDest2.begin(), tempDest2.end()),
            "src=\"/",
            "src=\"" + baseUrl + "/");
      boost::algorithm::replace_all_copy(
            std::back_inserter(dest),
            boost::make_iterator_range(tempDest3.begin(), tempDest3.end()),
            "src='/",
            "src='" + baseUrl + "/");
      
      // append javascript callbacks
      std::string js(kJsCallbacks);
      std::copy(js.begin(), js.end(), std::back_inserter(dest));
   }
private:
   std::string requestUri_;
};


template <typename Filter>
void setDynamicContentResponse(const std::string& content,
                               const http::Request& request,
                               const Filter& filter,
                               http::Response* pResponse)
{
   // always attempt gzip
   if (request.acceptsEncoding(http::kGzipEncoding))
      pResponse->setContentEncoding(http::kGzipEncoding);
   
   // if the response doesn't already have Cache-Control then send an eTag back
   // and force revalidation (not for desktop mode since it doesn't handle
   // eTag-based caching)
   if (!pResponse->containsHeader("Cache-Control") &&
       options().programMode() == kSessionProgramModeServer)
   {
      // force cache revalidation since this is dynamic content
      pResponse->setCacheWithRevalidationHeaders();

      // set as cacheable content (uses eTag/If-None-Match)
      Error error = pResponse->setCacheableBody(content, request, filter);
      if (error)
      {
         pResponse->setError(http::status::InternalServerError,
                             error.code().message());
      }
   }
   // otherwise just leave it alone
   else
   {
      pResponse->setBody(content, filter);
   }
}
   

void setDynamicContentResponse(const std::string& content,
                               const http::Request& request,
                               http::Response* pResponse)
{
   http::NullOutputFilter nullFilter;
   setDynamicContentResponse(content, request, nullFilter, pResponse);
}
   

template <typename Filter>
void handleHttpdResult(SEXP httpdSEXP, 
                       const http::Request& request, 
                       const Filter& htmlFilter,
                       http::Response* pResponse)
{
   // NOTE: this function is a port of process_request in Rhttpd.c
   // (that function is coupled to sending its results via the R http daemon, 
   // since we need to send the results via our daemon we need our own
   // implemetnation of the function). The port was completed 10/28/2009 so 
   // diffs in this function subsequent to that should be accounted for
   
   // defaults
   int code = 200;
   const char * const kTextHtml = "text/html";
   std::string contentType(kTextHtml);
   std::vector<std::string> headers;
   
   // if present, second element is content type
   if (LENGTH(httpdSEXP) > 1) 
   {
      SEXP ctSEXP = VECTOR_ELT(httpdSEXP, 1);     
      if (TYPEOF(ctSEXP) == STRSXP && LENGTH(ctSEXP) > 0)
         contentType = CHAR(STRING_ELT(ctSEXP, 0));
   }
   
   // if present, third element is headers vector
   if (LENGTH(httpdSEXP) > 2) 
   { 
      SEXP headersSEXP = VECTOR_ELT(httpdSEXP, 2);
      if (TYPEOF(headersSEXP) == STRSXP)
         r::sexp::extract(headersSEXP, &headers);
   }
   
   // if present, fourth element is HTTP code
   if (LENGTH(httpdSEXP) > 3) 
   {
      code = r::sexp::asInteger(VECTOR_ELT(httpdSEXP, 3));
   }
   
   // setup response
   pResponse->setStatusCode(code);
   pResponse->setContentType(contentType);

   // set headers
   std::for_each(headers.begin(), 
                 headers.end(),
                 boost::bind(&http::Response::setHeaderLine, pResponse, _1));
   
   // check payload
   SEXP payloadSEXP = VECTOR_ELT(httpdSEXP, 0);
   
   // payload = string
   if ((TYPEOF(payloadSEXP) == STRSXP || TYPEOF(payloadSEXP) == VECSXP) &&
        LENGTH(payloadSEXP) > 0)
   { 
      // get the names and the content string
      SEXP namesSEXP = r::sexp::getNames(httpdSEXP);
      std::string content;
      if (TYPEOF(payloadSEXP) == STRSXP)
         content = r::sexp::asString(STRING_ELT(payloadSEXP, 0));
      else if (TYPEOF(payloadSEXP) == VECSXP)
         content = r::sexp::asString(VECTOR_ELT(payloadSEXP, 0));
      
      // check for special file returns
      std::string fileName ;
      if (TYPEOF(namesSEXP) == STRSXP && LENGTH(namesSEXP) > 0 &&
          !std::strcmp(CHAR(STRING_ELT(namesSEXP, 0)), "file"))
      {
         fileName = content;
      }
      else if (LENGTH(payloadSEXP) > 1 && content == "*FILE*")
      {
         fileName = CHAR(STRING_ELT(payloadSEXP, 1));
      }
      
      // set the body
      if (!fileName.empty()) // from file
      {
         // get file path
         FilePath filePath(fileName);
         
         // read file contents
         std::string contents;
         Error error = readStringFromFile(filePath, &contents);
         if (error)
         {
            pResponse->setError(error);
            return;
         }

         if (options().programMode() == kSessionProgramModeServer)
            pResponse->setCacheWithRevalidationHeaders();

         // set body (apply filter to html)
         if (pResponse->contentType() == kTextHtml)
         {
            if (options().programMode() == kSessionProgramModeServer)
               pResponse->setCacheableBody(contents, request, htmlFilter);
            else
               pResponse->setBody(contents, htmlFilter);
         }
         else
         {
            if (options().programMode() == kSessionProgramModeServer)
               pResponse->setCacheableBody(contents, request);
            else
               pResponse->setBody(contents);
         }
      }
      else // from dynamic content
      {
         if (code == http::status::Ok)
         {
            // set body (apply filter to html)
            if (pResponse->contentType() == kTextHtml)
            {
               setDynamicContentResponse(content, 
                                         request, 
                                         htmlFilter, 
                                         pResponse);
            }
            else
            {
               setDynamicContentResponse(content, request, pResponse);
            }
         }
         else // could be a redirect or something else, don't interfere
         {
            pResponse->setBodyUnencoded(content);
         }
      }
   }
   
   // payload = raw buffer
   else if (TYPEOF(payloadSEXP) == RAWSXP)
   {
      std::string bytes((char*)(RAW(payloadSEXP)), LENGTH(payloadSEXP));
      setDynamicContentResponse(bytes, request, pResponse);
   }
   
   // payload = unexpected type
   else 
   {
      pResponse->setError(http::status::InternalServerError,
                          "Invalid response from R");
   }
}
 
   
// mirrors parse_query in Rhttpd.c
SEXP parseQuery(const http::Fields& fields, r::sexp::Protect* pProtect)
{
   if (fields.empty())
      return R_NilValue;
   else
      return r::sexp::create(fields, pProtect);
}

// mirrors parse_request_body in Rhttpd.c
SEXP parseRequestBody(const http::Request& request, r::sexp::Protect* pProtect)
{
   if (request.body().empty())
   {
      return R_NilValue;
   }
   else if (request.contentType() == "application/x-www-form-urlencoded")
   {
      return parseQuery(request.formFields(), pProtect);
   }
   else
   {
      // body bytes
      int contentLength = request.body().length();
      SEXP bodySEXP;
      pProtect->add(bodySEXP = Rf_allocVector(RAWSXP, contentLength));
      if (contentLength > 0)
         ::memcpy(RAW(bodySEXP), request.body().c_str(), contentLength);

      // content type
      if (!request.contentType().empty())
      {
         Rf_setAttrib(bodySEXP,
                      Rf_install("content-type"),
                      Rf_mkString(request.contentType().c_str()));
      }

      return bodySEXP;
   }
}

// mirrors collect_buffers in Rhttpd.c
SEXP headersBuffer(const http::Request& request, r::sexp::Protect* pProtect)
{
   // get headers
   std::string headers;
   for(http::Headers::const_iterator it = request.headers().begin();
       it != request.headers().end();
       ++it)
   {
      headers.append(it->name);
      headers.append(": ");
      headers.append(it->value);
      headers.append("\n");
   }

   // append Request-Method
   headers.append("Request-Method: " + request.method() + "\n");

   // allocate RAWSXP and copy headers to it
   SEXP headersSEXP = Rf_allocVector(RAWSXP, headers.length());
   pProtect->add(headersSEXP);
   char* headersBuffer = (char*) RAW(headersSEXP);
   headers.copy(headersBuffer, headers.length());

   // return
   return headersSEXP;
}

typedef boost::function<SEXP(const std::string&)> HandlerSource;


// NOTE: this emulates the calling portion of process_request in Rhttpd.c,
// to do this it uses low-level R functions and therefore must be wrapped
// in executeSafely
SEXP callHandler(const std::string& path,
                 const http::Request& request,
                 const HandlerSource& handlerSource,
                 r::sexp::Protect* pProtect)
{
   // uri decode the path
   std::string decodedPath = http::util::urlDecode(path, false);

   // construct "try(httpd(url, query, body, headers), silent=TRUE)"

   SEXP trueSEXP;
   pProtect->add(trueSEXP = Rf_ScalarLogical(TRUE));
   SEXP queryStringSEXP = parseQuery(request.queryParams(), pProtect);
   SEXP requestBodySEXP = parseRequestBody(request, pProtect);
   SEXP headersSEXP = headersBuffer(request, pProtect);

   // only provide headers if appropriate
   SEXP argsSEXP;
   if (s_provideHeaders)
   {
      argsSEXP = Rf_list4(Rf_mkString(path.c_str()),
                          queryStringSEXP,
                          requestBodySEXP,
                          headersSEXP);
   }
   else
   {
      argsSEXP = Rf_list3(Rf_mkString(path.c_str()),
                          queryStringSEXP,
                          requestBodySEXP);
   }
   pProtect->add(argsSEXP);

   // form the call expression
   SEXP innerCallSEXP;
   pProtect->add(innerCallSEXP = Rf_lang3(
         Rf_install("try"),
         Rf_lcons( (handlerSource(path)), argsSEXP),
         trueSEXP));
   SET_TAG(CDR(CDR(innerCallSEXP)), Rf_install("silent"));
   
   // suppress warnings
   SEXP suppressWarningsSEXP;
   pProtect->add(suppressWarningsSEXP = r::sexp::findFunction("suppressWarnings", "base"));
   
   SEXP callSEXP;
   pProtect->add(callSEXP = Rf_lang2(suppressWarningsSEXP, innerCallSEXP));

   // execute and return
   SEXP resultSEXP;
   pProtect->add(resultSEXP = Rf_eval(callSEXP,
                                      R_FindNamespace(Rf_mkString("tools"))));
   return resultSEXP;
}

r_util::RPackageInfo packageInfoForRd(const FilePath& rdFilePath)
{
   FilePath packageDir = rdFilePath.parent().parent();

   FilePath descFilePath = packageDir.childPath("DESCRIPTION");
   if (!descFilePath.exists())
      return r_util::RPackageInfo();

   r_util::RPackageInfo pkgInfo;
   Error error = pkgInfo.read(packageDir);
   if (error)
   {
      LOG_ERROR(error);
      return r_util::RPackageInfo();
   }
   else
   {
      return pkgInfo;
   }
}

template <typename Filter>
void handleRdPreviewRequest(const http::Request& request,
                            const Filter& filter,
                            http::Response* pResponse)
{
   // read parmaeters
   std::string file = request.queryParamValue("file");
   if (file.empty())
   {
      pResponse->setError(http::status::BadRequest, "No file parameter");
      return;
   }

   // ensure file exists
   FilePath filePath = module_context::resolveAliasedPath(file);
   if (!filePath.exists())
   {
      pResponse->setNotFoundError(request.uri());
      return;
   }

   // build command used to convert to HTML
   FilePath rHomeBinDir;
   Error error = module_context::rBinDir(&rHomeBinDir);
   if (error)
   {
      pResponse->setError(error);
      return;
   }
   shell_utils::ShellCommand rCmd = module_context::rCmd(rHomeBinDir);
   rCmd << "Rdconv";
   rCmd << "--type=html";
   r_util::RPackageInfo pkgInfo = packageInfoForRd(filePath);
   if (!pkgInfo.empty())
      rCmd << "--package=" + pkgInfo.name();

   rCmd << filePath;

   // run the converstion and return it
   core::system::ProcessOptions options;
   core::system::ProcessResult result;
   error = core::system::runCommand(rCmd, options, &result);
   if (error)
   {
      pResponse->setError(error);
   }
   else if (result.exitStatus != EXIT_SUCCESS)
   {
      pResponse->setError(http::status::InternalServerError, result.stdErr);
   }
   else
   {
      pResponse->setContentType("text/html");
      pResponse->setNoCacheHeaders();
      std::istringstream istr(result.stdOut);
      pResponse->setBody(istr, filter);
   }
}

template <typename Filter>
void handleHttpdRequest(const std::string& location,
                        const HandlerSource& handlerSource,
                        const http::Request& request, 
                        const Filter& filter,
                        http::Response* pResponse)
{
   // get the requested path
   std::string path = http::util::pathAfterPrefix(request, location);

   // server custom css file if necessary
   if (boost::algorithm::ends_with(path, "/R.css"))
   {
      core::FilePath cssFile = options().rResourcesPath().childPath("R.css");
      if (cssFile.exists())
      {
         pResponse->setFile(cssFile, request, filter);
         return;
      }
   }

   // handle presentation url
   if (boost::algorithm::starts_with(path, "/presentation"))
   {
      presentation::handlePresentationHelpRequest(request, kJsCallbacks, pResponse);
      return;
   }

   // handle Rd file preview
   if (boost::algorithm::starts_with(path, "/preview"))
   {
      handleRdPreviewRequest(request, filter, pResponse);
      return;
   }

   // markdown help is also a special case
   if (path == "/doc/markdown_help.html")
   {
      core::FilePath helpFile = options().rResourcesPath().childPath(
                                                      "markdown_help.html");
      if (helpFile.exists())
      {
         pResponse->setFile(helpFile, request, filter);
         return;
      }
   }
   
   // roxygen help
   if (path == "/doc/roxygen_help.html")
   {
      core::FilePath helpFile = options().rResourcesPath().childPath("roxygen_help.html");
      if (helpFile.exists())
      {
         pResponse->setFile(helpFile, request, filter);
         return;
      }
   }

   if (boost::algorithm::starts_with(path, "/doc/home/"))
   {
      handleHelpHomeRequest(request, kJsCallbacks, pResponse);
      return;
   }

   // evalute the handler
   r::sexp::Protect rp;
   SEXP httpdSEXP;
   Error error = r::exec::executeSafely<SEXP>(
         boost::bind(callHandler,
                        path,
                        boost::cref(request),
                        handlerSource,
                        &rp),
         &httpdSEXP);

   // error calling the function
   if (error)
   {
      pResponse->setError(http::status::InternalServerError,
                          error.code().message());
   }
   
   // error returned explicitly by httpd
   else if (TYPEOF(httpdSEXP) == STRSXP && LENGTH(httpdSEXP) > 0)
   {
      pResponse->setError(http::status::InternalServerError, 
                          r::sexp::asString(httpdSEXP));
   }
   
   // content returned from httpd
   else if (TYPEOF(httpdSEXP) == VECSXP && LENGTH(httpdSEXP) > 0)
   {
      handleHttpdResult(httpdSEXP, request, filter, pResponse);
   }
   
   // unexpected SEXP type returned from httpd
   else
   {
      pResponse->setError(http::status::InternalServerError,
                          "Invalid response from R");
   }
}

// this mirrors handler_for_path in Rhttpd.c. They cache the custom handlers
// env (not sure why). do the same for consistency
SEXP s_customHandlersEnv = NULL;
SEXP lookupCustomHandler(const std::string& uri)
{
   // pick name of handler out of uri
   boost::regex customRegx(".*/custom/([A-Za-z0-9_\\-]*).*");
   boost::smatch match;
   if (regex_utils::match(uri, match, customRegx))
   {
      std::string handler = match[1];

      // load .httpd.handlers.env
      if (!s_customHandlersEnv)
      {
         s_customHandlersEnv = Rf_eval(Rf_install(".httpd.handlers.env"),
                                       R_FindNamespace(Rf_mkString("tools")));
      }

      // we only proceed if .httpd.handlers.env really exists
      if (TYPEOF(s_customHandlersEnv) == ENVSXP)
      {
         SEXP cl = Rf_findVarInFrame3(s_customHandlersEnv,
                                      Rf_install(handler.c_str()),
                                      TRUE);
         if (cl != R_UnboundValue && TYPEOF(cl) == CLOSXP) // need a closure
            return cl;
      }
   }

   // if we didn't find a handler then return handler lookup error
   return r::sexp::findFunction(".rs.handlerLookupError");
}

   
// .httpd.handlers.env
void handleCustomRequest(const http::Request& request, 
                         http::Response* pResponse)
{
   handleHttpdRequest("",
                      lookupCustomHandler,
                      request,
                      http::NullOutputFilter(),
                      pResponse);
}

// handle requests for session temporary directory
void handleSessionRequest(const http::Request& request, http::Response* pResponse)
{
   // get the raw uri & strip its location prefix
   std::string sessionPrefix = std::string(kSessionLocation) + "/";
   std::string uri = request.uri();
   if (!uri.compare(0, sessionPrefix.length(), sessionPrefix))
      uri = uri.substr(sessionPrefix.length());

   // remove query parameters and anchor
   std::size_t pos = uri.find("?");
   if (pos != std::string::npos)
      uri.erase(pos);
   pos = uri.find("#");
   if (pos != std::string::npos)
      uri.erase(pos);

   // ensure that this path does not contain ..
   if (uri.find("..") != std::string::npos)
   {
      pResponse->setNotFoundError(request.uri());
      return;
   }

   // form a path to the temporary file
   FilePath tempFilePath = r::session::utils::tempDir().childPath(uri);

   // return the file
   pResponse->setCacheWithRevalidationHeaders();
   if (tempFilePath.mimeContentType() == "text/html")
   {
      pResponse->setCacheableFile(tempFilePath, request);
   }
   else
   {
      pResponse->setCacheableFile(tempFilePath, request);
   }
}

// the ShowHelp event will result in the Help pane requesting the specified
// help url. we handle this request directly by calling the R httpd function
// to dynamically form the correct http response
void handleHelpRequest(const http::Request& request, http::Response* pResponse)
{
   handleHttpdRequest(kHelpLocation,
                      boost::bind(r::sexp::findFunction, "httpd", "tools"),
                      request,
                      HelpContentsFilter(request),
                      pResponse);
}

SEXP rs_previewRd(SEXP rdFileSEXP)
{
   std::string rdFile = r::sexp::safeAsString(rdFileSEXP);
   boost::format fmt("help/preview?file=%1%");
   std::string url = boost::str(fmt % http::util::urlEncode(rdFile, true));
   ClientEvent event(client_events::kShowHelp, url);
   module_context::enqueClientEvent(event);
   return R_NilValue;
}


} // anonymous namespace
   
Error initialize()
{
   // determine whether we should provide headers to custom handlers
   s_provideHeaders = r::util::hasRequiredVersion("2.13");


   // register previewRd function
   R_CallMethodDef previewRdMethodDef ;
   previewRdMethodDef.name = "rs_previewRd" ;
   previewRdMethodDef.fun = (DL_FUNC)rs_previewRd ;
   previewRdMethodDef.numArgs = 1;
   r::routines::addCallMethod(previewRdMethodDef);

   using boost::bind;
   using core::http::UriHandler;
   using namespace module_context;
   using namespace rstudio::r::function_hook ;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRBrowseUrlHandler, handleLocalHttpUrl))
      (bind(registerRBrowseFileHandler, handleRShowDocFile))
      (bind(registerUriHandler, kHelpLocation, handleHelpRequest))
      (bind(sourceModuleRFile, "SessionHelp.R"));
   Error error = initBlock.execute();
   if (error)
      return error;

   // init help
   bool isDesktop = options().programMode() == kSessionProgramModeDesktop;
   int port = safe_convert::stringTo<int>(session::options().wwwPort(), 0);
   error = r::exec::RFunction(".rs.initHelp", port, isDesktop).call(
                                                            &s_handleCustom);
   if (error)
      LOG_ERROR(error);

   // handle /custom and /session urls internally if necessary (always in
   // server mode, in desktop mode if the internal http server can't
   // bind to a port)
   if (s_handleCustom)
   {
      ExecBlock serverInitBlock;
      serverInitBlock.addFunctions()
         (bind(registerUriHandler, kCustomLocation, handleCustomRequest))
         (bind(registerUriHandler, kSessionLocation, handleSessionRequest));
      error = serverInitBlock.execute();
      if (error)
         return error;
   }

   return Success();
}


} // namepsace help
} // namespace modules
} // namespace session
} // namespace rstudio

