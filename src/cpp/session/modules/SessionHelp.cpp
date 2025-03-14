/*
 * SessionHelp.cpp
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

#include "SessionHelp.hpp"

#include <algorithm>
#include <gsl/gsl-lite.hpp>

#include <boost/regex.hpp>
#include <boost/function.hpp>
#include <boost/format.hpp>
#include <boost/range/iterator_range.hpp>
#include <boost/algorithm/string/regex.hpp>
#include <boost/algorithm/string/replace.hpp>
#include <boost/algorithm/string.hpp>
#include <boost/iostreams/filter/aggregate.hpp>

#include <shared_core/Error.hpp>

#include <core/Algorithm.hpp>
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
#include <session/SessionPersistentState.hpp>

#include <session/prefs/UserPrefs.hpp>

#include "presentation/SlideRequestHandler.hpp"

#include "SessionHelpHome.hpp"
#include "session-config.h"

#ifdef RSTUDIO_SERVER
#include <server_core/UrlPorts.hpp>
#endif

// protect R against windows TRUE/FALSE defines
#undef TRUE
#undef FALSE

using namespace rstudio::core;
using namespace boost::placeholders;

namespace rstudio {
namespace session {
namespace modules { 
namespace help {

namespace {   

// save computed help url prefix for comparison in rHelpUrlHandler
const char * const kHelpLocation = "/help";
const std::string kPythonLocation = "/python";
const char * const kCustomLocation = "/custom";
const char * const kSessionLocation = "/session";

// are we handling custom urls internally or allowing them to
// show in an external browser
bool s_handleCustom = false;

std::string localURL(const std::string& address, const std::string& port)
{
   return "http://" + address + ":" + port + "/";
}

std::string replaceRPort(const std::string& url,
                         const std::string& rPort,
                         const std::string& scope)
{

   // avoid replacing port in query params in R help, as R uses this
   // for state management in its help server from R 3.6.0 and onwards
   if (scope.empty())
   {
      std::vector<std::string> splat = core::algorithm::split(url, "?");
      boost::algorithm::replace_last(splat[0], rPort, session::options().wwwPort());
      return core::algorithm::join(splat, "?");
   }
   else
   {
      return boost::algorithm::replace_last_copy(url, rPort, session::options().wwwPort());
   }
}

bool isLocalURL(const std::string& url,
                const std::string& scope,
                std::string* pLocalURLPath = nullptr)
{
   // first look for local ip prefix
   std::string rPort = module_context::rLocalHelpPort();
   std::string urlPrefix = localURL("127.0.0.1", rPort);
   size_t pos = url.find(urlPrefix + scope);
   if (pos != std::string::npos)
   {
      std::string relativeUrl = url.substr(urlPrefix.length());
      if (pLocalURLPath)
         *pLocalURLPath = replaceRPort(relativeUrl, rPort, scope);
      return true;
   }

   // next look for localhost
   urlPrefix = localURL("localhost", rPort);
   pos = url.find(urlPrefix + scope);
   if (pos != std::string::npos)
   {
      std::string relativeUrl = url.substr(urlPrefix.length());
      if (pLocalURLPath)
         *pLocalURLPath = replaceRPort(relativeUrl, rPort, scope);
      return true;
   }

   // none found
   return false;
}

std::string normalizeHttpdSearchContent(const std::string& content)
{
   return boost::regex_replace(
            content,
            boost::regex("(The search string was <b>\")(.*)(\"</b>)"),
            [](const boost::smatch& m)
   {
      std::string query = m[2];
      if (query.find('<') != std::string::npos)
         query = string_utils::htmlEscape(query);

      return m[1] + query + m[3];
   });
}

template <typename F>
bool isHttpdErrorPayload(SEXP payloadSEXP, F accessor)
{
   for (int i = 0; i < r::sexp::length(payloadSEXP); i++)
   {
      std::string line = r::sexp::asString(accessor(payloadSEXP, i));
      if (line.find("<title>R: httpd error</title>") != std::string::npos)
         return true;
   }

   return false;
}

bool isHttpdErrorPayload(SEXP payloadSEXP)
{
   switch (TYPEOF(payloadSEXP))
   {
   case STRSXP : return isHttpdErrorPayload(payloadSEXP, STRING_ELT);
   case VECSXP : return isHttpdErrorPayload(payloadSEXP, VECTOR_ELT);
   default     : return false;
   }
}


// hook the browseURL function to look for calls to the R internal http
// server. for custom URLs remap the address to remote and then fire
// the browse_url event. for help URLs fire the appropriate show_help event
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

#ifdef RSTUDIO_SERVER
   // other localhost URLs can benefit from port mapping -- we map them
   // all since if we don't do any mapping they'll just fail hard
   
   // see if we can form a portmap path for this url
   std::string path;
   if (options().programMode() == kSessionProgramModeServer &&
       server_core::portmapPathForLocalhostUrl(url, 
            persistentState().portToken(), &path))
   {
      module_context::enqueClientEvent(browseUrlEvent(path));
      return true;
   }
#endif

   // wasn't a url of interest
   return false;
}
   
// As of R 2.10 RShowDoc still uses the legacy file::// mechanism for
// displaying the manual. Redirect these to the appropriate help event
bool handleRShowDocFile(const core::FilePath& filePath)
{
   std::string absPath = filePath.getAbsolutePath();
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
const char * const kJsCallbacks = R"EOF(
<script type="text/javascript">

   if (window.parent.helpNavigated)
      window.parent.helpNavigated(document, window);

   if (window.parent.helpKeydown)
      window.onkeydown = function(e) { window.parent.helpKeydown(e); }

   if (window.parent.helpMousedown)
      window.onmousedown = function(e) { window.parent.helpMousedown(e); }

   if (window.parent.helpMouseover)
      window.onmouseover = function(e) { window.parent.helpMouseover(e); }

   if (window.parent.helpMouseout)
      window.onmouseout = function(e) { window.parent.helpMouseout(e); }

   if (window.parent.helpClick)
      window.onclick = function(e) { window.parent.helpClick(e); } 

   window.addEventListener("load", function(event) {

      // https://github.com/rstudio/rmarkdown/blob/de02c926371fdadc4d92f08e1ad7b77db069be49/inst/rmarkdown/templates/html_vignette/resources/vignette.css#L187-L201
      var classMap = {
         "at": "ace_keyword ace_operator",
         "ch": "ace_string",
         "co": "ace_comment",
         "cf": "ace_keyword",
         "cn": "ace_constant ace_language",
         "dt": "ace_identifier",
         "dv": "ace_constant ace_numeric",
         "er": "ace_keyword ace_operator",
         "fu": "ace_identifier",
         "kw": "ace_keyword",
         "ot": "ace_keyword ace_operator",
         "sc": "ace_keyword ace_operator",
         "st": "ace_string",
      };

      var els = document.querySelectorAll(".sourceCode span");
      for (el of els)
         el.className = classMap[el.className] || el.className;

   });

</script>
)EOF";


class HelpFontSizeFilter : public boost::iostreams::aggregate_filter<char>
{
public:
   typedef std::vector<char> Characters;

   void do_filter(const Characters& src, Characters& dest)
   {
      std::string cssValue(src.begin(), src.end());
      cssValue.append("body, td {\n   font-size:");
      cssValue.append(safe_convert::numberToString(prefs::userPrefs().helpFontSizePoints()));
      cssValue.append("pt;\n}");
      std::copy(cssValue.begin(), cssValue.end(), std::back_inserter(dest));
   }
};

class HelpContentsFilter : public boost::iostreams::aggregate_filter<char>
{
public:
   typedef std::vector<char> Characters;

   HelpContentsFilter(const http::Request& request)
   {
      requestUri_ = request.uri();
   }

   void do_filter(const Characters& src, Characters& dest)
   {
      std::string baseUrl = http::URL::uncomplete(
            requestUri_,
            kHelpLocation);

      // copy from src to dest
      dest = src;
      
      // fixup hard-coded hrefs
      boost::algorithm::replace_all(dest, "href=\"/", "href=\"" + baseUrl + "/");
      boost::algorithm::replace_all(dest, "href='/", "href='" + baseUrl + "/");
      
      // fixup hard-coded src=
      boost::algorithm::replace_all(dest, "src=\"/", "src=\"" + baseUrl + "/");
      boost::algorithm::replace_all(dest, "src='/", "src='" + baseUrl + "/");
      
      // add classes to headers
      boost::regex reHeader("<h3>Arguments</h3>");
      std::string reFormat("<h3 class=\"r-arguments-title\">Arguments</h3>");
      boost::algorithm::replace_all_regex(dest, reHeader, reFormat);
      
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
                             error.getMessage());
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
   // implementation of the function). The port was completed 10/28/2009 so 
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
   
   // fix up location header for redirects
   if (code == 302 && pResponse->containsHeader("Location"))
   {
      // check for a redirect to the R help server. the R help server hard-codes
      // navigation to the help server at 127.0.0.1 in a number of places
      std::string location = pResponse->headerValue("Location");
      std::string rPort = module_context::rLocalHelpPort();
      std::string rHelpPrefix = fmt::format("http://127.0.0.1:{}/", rPort);
      if (boost::algorithm::starts_with(location, rHelpPrefix))
      {
         // make sure we use the same base URL as the request, otherwise
         // we might not be allowed to display the redirected Help content in the frame
         //
         // for example, RStudio Server might've been loaded from 'http://localhost:8787', but the
         // R help server might try directing to 'http://127.0.0.1:8787'
         //
         // https://github.com/rstudio/rstudio/issues/13263
         std::string path = location.substr(rHelpPrefix.length());
         std::string ref = request.headerValue("Referer");
         std::string redirect = fmt::format("{}help/{}", ref, path);
         pResponse->setHeader("Location", redirect);
      }
   }
   
   // check payload
   SEXP payloadSEXP = VECTOR_ELT(httpdSEXP, 0);
   
   // payload = string
   if ((TYPEOF(payloadSEXP) == STRSXP || TYPEOF(payloadSEXP) == VECSXP) &&
        LENGTH(payloadSEXP) > 0)
   {
      // handle httpd errors (returned as specially constructed payload)
      if (isHttpdErrorPayload(payloadSEXP))
      {
         pResponse->setError(http::status::NotFound, "URL '" + request.uri() + "' not found");
         return;
      }

      // get the names and the content string
      SEXP namesSEXP = r::sexp::getNames(httpdSEXP);
      std::string content;
      if (TYPEOF(payloadSEXP) == STRSXP)
         content = r::sexp::asString(STRING_ELT(payloadSEXP, 0));
      else if (TYPEOF(payloadSEXP) == VECSXP)
         content = r::sexp::asString(VECTOR_ELT(payloadSEXP, 0));

      // normalize search result output
      if (boost::algorithm::iends_with(request.path(), "/search"))
         content = normalizeHttpdSearchContent(content);
      
      // check for special file returns
      std::string fileName;
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
      int contentLength = gsl::narrow_cast<int>(request.body().length());
      SEXP bodySEXP;
      pProtect->add(bodySEXP = Rf_allocVector(RAWSXP, contentLength));
      if (contentLength > 0)
         ::memcpy(RAW(bodySEXP), request.body().c_str(), contentLength);

      // content type
      if (!request.contentType().empty())
      {
         r::sexp::Protect protect;
         SEXP requestTypeSEXP;
         protect.add(requestTypeSEXP = Rf_mkString(request.contentType().c_str()));
         Rf_setAttrib(bodySEXP, Rf_install("content-type"), requestTypeSEXP);
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
   // use local protection for intermediate values
   r::sexp::Protect protect;
   
   // uri decode the path
   std::string decodedPath = http::util::urlDecode(path);

   // construct "try(httpd(url, query, body, headers), silent=TRUE)"
   SEXP queryStringSEXP = parseQuery(request.queryParams(), pProtect);
   SEXP requestBodySEXP = parseRequestBody(request, pProtect);
   SEXP headersSEXP = headersBuffer(request, pProtect);
   
   SEXP pathSEXP;
   protect.add(pathSEXP = Rf_mkString(path.c_str()));

   SEXP argsSEXP;
   protect.add(argsSEXP = Rf_list4(pathSEXP, queryStringSEXP, requestBodySEXP, headersSEXP));

   // form the call expression
   SEXP handlerSourceSEXP;
   protect.add(handlerSourceSEXP = handlerSource(path));
   
   SEXP argSEXP;
   protect.add(argSEXP = Rf_lcons(handlerSourceSEXP, argsSEXP));
   
   SEXP innerCallSEXP;
   protect.add(innerCallSEXP = Rf_lang3(Rf_install("try"), argSEXP, R_TrueValue));
   SET_TAG(CDDR(innerCallSEXP), Rf_install("silent"));
   
   // suppress warnings
   SEXP suppressWarningsSEXP;
   protect.add(suppressWarningsSEXP = r::sexp::findFunction("suppressWarnings", "base"));
   
   SEXP callSEXP;
   protect.add(callSEXP = Rf_lang2(suppressWarningsSEXP, innerCallSEXP));

   // get reference to tools namespace
   SEXP toolsSEXP = r::sexp::findNamespace("tools");
   
   // execute and return
   SEXP resultSEXP;
   pProtect->add(resultSEXP = Rf_eval(callSEXP, toolsSEXP));
   
   return resultSEXP;
}

r_util::RPackageInfo packageInfoForRd(const FilePath& rdFilePath)
{
   FilePath packageDir = rdFilePath.getParent().getParent();

   FilePath descFilePath = packageDir.completeChildPath("DESCRIPTION");
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

Error Rd2HTML(FilePath filePath, std::string* html)
{
   r::exec::RFunction fun(".rs.Rd2HTML");
   fun.addUtf8Param(filePath.getAbsolutePath());

   // add in package-specific information if available
   r_util::RPackageInfo pkgInfo = packageInfoForRd(filePath);
   if (!pkgInfo.empty())
   {
      if (!pkgInfo.name().empty())
         fun.addParam(pkgInfo.name());
   }

   return fun.call(html);
}

void handleDevFigure(const http::Request& request,
                     http::Response* pResponse)
{
   // read parameters
   std::string pkg = request.queryParamValue("pkg");
   std::string figure = request.queryParamValue("figure");
   
   if (pkg.empty() || figure.empty())
   {
      pResponse->setError(http::status::BadRequest, "Malformed dev-figure. Needs pkg and figure parameters");
      return;
   }

   r::exec::RFunction system_file("base:::system.file");
   system_file.addUtf8Param("package", pkg);
   system_file.addParam("man");
   system_file.addParam("figures");
   system_file.addUtf8Param(figure);

   std::string file;
   Error error = system_file.call(&file);
   if (error) 
   {
      pResponse->setError(http::status::InternalServerError, "figure not found");
      return;
   }
   pResponse->setFile(FilePath(file), request);
}

template <typename Filter>
bool handleDevRequest(const http::Request& request,
                      const Filter& filter,
                      http::Response* pResponse)
{
   std::string topic = request.queryParamValue("dev");
   
   r::exec::RFunction dev_topic_find("pkgload:::dev_topic_find", topic);
   r::sexp::Protect protect;
   SEXP res = R_NilValue;
   Error error = dev_topic_find.call(&res, &protect);
   if (error || res == R_NilValue)
   {
      return false;
   }
   std::string file;
   error = r::sexp::getNamedListElement(res, "path", &file);
   if (error)
   {
      return false;
   }
   
   // ensure file exists
   FilePath filePath = module_context::resolveAliasedPath(file);
   if (!filePath.exists())
   {
      return false;
   }
   std::string html;
   error = Rd2HTML(filePath, &html);
   
   if (error)
   {
      return false;
   }
   else 
   {
      pResponse->setContentType("text/html");
      pResponse->setNoCacheHeaders();
      pResponse->setBody(html, filter);
      return true;
   }
}

template <typename Filter>
void handleRdPreviewRequest(const http::Request& request,
                            const Filter& filter,
                            http::Response* pResponse)
{
   // read parameters
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
      pResponse->setNotFoundError(request);
      return;
   }
   std::string html;
   Error error = Rd2HTML(filePath, &html);
   if (error)
   {
      pResponse->setError(error);
   }
   else 
   {
      pResponse->setContentType("text/html");
      pResponse->setNoCacheHeaders();
      pResponse->setBody(html, filter);
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
      core::FilePath cssFile = options().rResourcesPath().completeChildPath("R.css");
      if (cssFile.exists())
      {
         // ignoring the filter parameter here because the only other possible filter 
         // is HelpContentsFilter which is for html
         pResponse->setFile(cssFile, request, HelpFontSizeFilter());
         return;
      }
   }

   // handle presentation url
   if (boost::algorithm::starts_with(path, "/presentation"))
   {
      presentation::handlePresentationHelpRequest(request, kJsCallbacks, pResponse);
      return;
   }

   if (path == "/dev-figure")
   {
      handleDevFigure(request, pResponse);
      return;   
   }

   // if there is a dev= parameter, then try to render dev documentation
   // dev= is added .rs.Rd2HTML when serving preview file
   if (!request.queryParamValue("dev").empty())
   {
      if (handleDevRequest(request, filter, pResponse))
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
      core::FilePath helpFile = options().rResourcesPath().completeChildPath(
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
      core::FilePath helpFile = options().rResourcesPath().completeChildPath("roxygen_help.html");
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

   // evaluate the handler
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
                          error.getMessage());
   }
   
   // error returned explicitly by httpd
   else if (TYPEOF(httpdSEXP) == STRSXP && LENGTH(httpdSEXP) > 0)
   {
      LOG_ERROR_MESSAGE("Handle httpd request error: " + r::sexp::asString(httpdSEXP));
      pResponse->setError(http::status::InternalServerError, 
                          "Internal Server Error");
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
SEXP s_customHandlersEnv = nullptr;
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
         SEXP toolsSEXP = r::sexp::findNamespace("tools");
         s_customHandlersEnv = Rf_eval(Rf_install(".httpd.handlers.env"), toolsSEXP);
      }

      // we only proceed if .httpd.handlers.env really exists
      if (TYPEOF(s_customHandlersEnv) == ENVSXP)
      {
         SEXP cl = Rf_findVarInFrame3(s_customHandlersEnv, Rf_install(handler.c_str()), TRUE);
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
      pResponse->setNotFoundError(request);
      return;
   }

   // form a path to the temporary file
   FilePath tempFilePath = r::session::utils::tempDir().completeChildPath(uri);

   // return the file
   pResponse->setCacheableFile(tempFilePath, request);
}

void handlePythonHelpRequest(const http::Request& request,
                             http::Response* pResponse)
{
   // get URL (everything after 'python/' bit
   std::string code = request.uri().substr(::strlen("/python/"));
   if (code.empty())
   {
      pResponse->setError(http::status::BadRequest, "Malformed URL");
      return;
   }

   // construct HTML help file from requested object
   std::string path;
   Error error = r::exec::RFunction(".rs.python.generateHtmlHelp")
         .addParam(code)
         .call(&path);
   if (error)
      LOG_ERROR(error);
   
   if (path.empty())
   {
      pResponse->setNotFoundError(request);
      return;
   }
   
   FilePath filePath(path);
   pResponse->setContentType("text/html");
   pResponse->setFile(filePath, request, HelpContentsFilter(request));
   
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

SEXP rs_showPythonHelp(SEXP codeSEXP)
{
   std::string code = r::sexp::safeAsString(codeSEXP);
   boost::format fmt("python/%1%.html");
   std::string url = boost::str(fmt % http::util::urlEncode(code, true));
   ClientEvent event(client_events::kShowHelp, url);
   module_context::enqueClientEvent(event);
   return R_NilValue;
}

} // anonymous namespace
   
Error initialize()
{
   RS_REGISTER_CALL_METHOD(rs_previewRd, 1);
   RS_REGISTER_CALL_METHOD(rs_showPythonHelp, 1);

   using boost::bind;
   using core::http::UriHandler;
   using namespace module_context;
   using namespace rstudio::r::function_hook;
   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerRBrowseUrlHandler, handleLocalHttpUrl))
      (bind(registerRBrowseFileHandler, handleRShowDocFile))
      (bind(registerUriHandler, kHelpLocation, handleHelpRequest))
      (bind(registerUriHandler, kPythonLocation, handlePythonHelpRequest))
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

#ifdef _WIN32
   // R's help server handler has issues with R 4.0.0; disable it explicitly
   // when that version of R is in use.
   // (see comments in module_context::sessionTempDirUrl)
   if (r::util::hasExactVersion("4.0.0"))
   {
      s_handleCustom = false;
   }
#endif

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


} // namespace help
} // namespace modules
} // namespace session
} // namespace rstudio

