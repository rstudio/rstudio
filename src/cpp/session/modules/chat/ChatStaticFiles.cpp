/*
 * ChatStaticFiles.cpp
 *
 * Copyright (C) 2025 by Posit Software, PBC
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

#include "ChatStaticFiles.hpp"
#include "ChatConstants.hpp"
#include "ChatInstallation.hpp"
#include "ChatLogging.hpp"
#include "session-config.h"

#include <atomic>
#include <map>
#include <mutex>
#include <string_view>
#include <boost/algorithm/string.hpp>
#include <boost/lexical_cast.hpp>
#include <fmt/format.h>

#include <core/FileSerializer.hpp>
#include <core/StringUtils.hpp>
#include <core/http/Cookie.hpp>
#include <core/http/Request.hpp>
#include <core/http/Response.hpp>
#include <core/http/URL.hpp>
#include <core/http/Util.hpp>
#include <core/system/System.hpp>
#include <shared_core/json/Json.hpp>

#include <session/SessionOptions.hpp>

#include "../SessionThemes.hpp"

using namespace rstudio::session::modules::chat::constants;
using namespace rstudio::session::modules::chat::installation;
using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace chat {
namespace staticfiles {

namespace {

// The AI Chat route, and the same route as a URI prefix. The bare form is what
// appears in a path when the route is followed by a segment boundary or ends
// the path; the prefix form is what a request for a file under the route
// starts with. Lengths come from the views rather than being written out
// beside them: a stale length would compile and cut a path at the wrong
// offset, which is the failure this module already had once (#18621).
constexpr std::string_view kAiChatRoute = "/ai-chat";
constexpr std::string_view kAiChatUriPrefix = "/ai-chat/";
static_assert(kAiChatUriPrefix.size() == kAiChatRoute.size() + 1 &&
              kAiChatUriPrefix.substr(0, kAiChatRoute.size()) == kAiChatRoute,
              "the AI chat URI prefix must be the route followed by a slash");

// Chat backend port, set by SessionChat.cpp when the backend starts.
// Used to build connect-src in the CSP header for desktop mode.
std::atomic<int> s_chatBackendPort{kChatBackendPortNone};

// Chat backend auth token, set by SessionChat.cpp when the backend starts.
// In server mode, this is delivered to the PA client via an HTTP-only cookie
// on the index.html response instead of as a URL query parameter.
//
// Both of these are only ever touched from the main session thread: uri
// handlers are dispatched synchronously from handleConnection, not from a
// pool of HTTP handler threads, and SessionChat sets them from the same
// thread. The atomic and the mutex are belt and braces, and the mutex is not
// what makes reading the token here safe.
std::mutex s_authTokenMutex;
std::string s_chatBackendAuthToken;

/**
 * Inject theme information into HTML content without inline scripts.
 *
 * Two modifications:
 * 1. Adds class="dark" to the <html> tag when the IDE uses a dark theme.
 *    This parser is intentionally simple and only handles the known Vite
 *    build output format (<html lang="en">). It assumes no `>` characters
 *    appear inside attribute values.
 * 2. Injects a <meta name="rstudio-theme"> tag in <head> carrying the
 *    background and foreground colors as data attributes. The databot
 *    frontend reads these on startup and applies them as CSS variables.
 *
 * This avoids inline scripts entirely, so no CSP nonce is needed.
 */
void injectThemeInfo(std::string* pContent)
{
   themes::ThemeColors colors = themes::getThemeColors();

   if (colors.isDark)
   {
      size_t htmlPos = pContent->find("<html");
      if (htmlPos != std::string::npos)
      {
         size_t gtPos = pContent->find(">", htmlPos);
         if (gtPos != std::string::npos)
         {
            std::string htmlTag =
               pContent->substr(htmlPos, gtPos - htmlPos);
            constexpr const char* kClassAttr = "class=\"";
            size_t classPos = htmlTag.find(kClassAttr);
            if (classPos != std::string::npos)
            {
               size_t insertAt =
                  htmlPos + classPos + strlen(kClassAttr);
               pContent->insert(insertAt, "dark ");
            }
            else
            {
               pContent->insert(gtPos, " class=\"dark\"");
            }
         }
      }
      else
      {
         DLOG("injectThemeInfo: <html tag not found, "
              "skipping dark class");
      }
   }

   std::string bg = string_utils::htmlEscape(colors.background, true);
   std::string fg = string_utils::htmlEscape(colors.foreground, true);

   std::string meta = fmt::format(
      R"(<meta name="rstudio-theme" data-background="{background}" data-foreground="{foreground}">)",
      fmt::arg("background", bg),
      fmt::arg("foreground", fg));

   constexpr const char* kHeadCloseTag = "</head>";
   size_t headPos = pContent->find(kHeadCloseTag);
   if (headPos != std::string::npos)
   {
      pContent->insert(headPos, meta + "\n");
   }
   else
   {
      DLOG("injectThemeInfo: </head> not found, "
           "skipping meta tag injection");
   }
}

/**
 * Load CSP directives from dist/csp.json in the Posit Assistant installation.
 *
 * Reads the file once and caches the result. The file is emitted by the
 * databot build and contains the same defaults that DatabotServer uses
 * in its Express middleware.
 *
 * Once loaded (or once a failure is encountered), the result is cached
 * for the lifetime of the session. A missing or broken file will not be
 * retried.
 *
 * @return Directive map (e.g., {"default-src": "'self'", ...}), or empty
 *         map if the file is missing or unparseable.
 */
std::map<std::string, std::string> loadCspDirectives()
{
   static const auto s_cached = []()
   {
      std::map<std::string, std::string> result;

      FilePath positAiPath = locatePositAssistantInstallation();
      if (positAiPath.isEmpty())
         return result;

      FilePath cspFile = positAiPath.completeChildPath(kCspConfigPath);
      if (!cspFile.exists())
         return result;

      std::string content;
      Error error = readStringFromFile(cspFile, &content);
      if (error)
      {
         WLOG("Failed to read CSP config: {}", error.getMessage());
         return result;
      }

      json::Value jsonValue;
      if (jsonValue.parse(content))
      {
         WLOG("Failed to parse CSP config: {}",
              cspFile.getAbsolutePath());
         return result;
      }

      if (!jsonValue.isObject())
      {
         WLOG("CSP config must be a JSON object: {}",
              cspFile.getAbsolutePath());
         return result;
      }

      json::Object obj = jsonValue.getObject();
      for (auto it = obj.begin(); it != obj.end(); ++it)
      {
         json::Value val = (*it).getValue();
         if (val.isString())
         {
            result[(*it).getName()] = val.getString();
         }
         else
         {
            WLOG("Ignoring non-string CSP directive: {}",
                 (*it).getName());
         }
      }

      return result;
   }();

   return s_cached;
}

// Cached CSP header string, rebuilt when the backend port changes.
std::mutex s_cspMutex;
std::string s_cachedCspHeader;
bool s_cspHeaderBuilt = false;

/**
 * Rebuild the cached CSP header string from dist/csp.json directives.
 *
 * Called once lazily on the first HTML request and again whenever the
 * backend port changes via setChatBackendPort().
 */
void rebuildCspHeaderCache()
{
   std::map<std::string, std::string> directives = loadCspDirectives();

   // If csp.json was missing, use a restrictive fallback
   if (directives.empty())
      directives["default-src"] = "'self'";

   // Respect the server's www-frame-origin setting so the chat pane can
   // render inside a cross-origin iframe when configured.
   // Always apply the server option, overriding any csp.json value, so that
   // admin configuration is authoritative.
   {
      std::string frameOrigin = options().wwwFrameOrigin();

      // Sanitize: strip characters that could break the CSP header
      boost::algorithm::erase_all(frameOrigin, "\r");
      boost::algorithm::erase_all(frameOrigin, "\n");
      boost::algorithm::erase_all(frameOrigin, ";");
      boost::algorithm::trim(frameOrigin);

      if (frameOrigin == "any")
         directives["frame-ancestors"] = "*";
      else if (frameOrigin == "none" || frameOrigin == "same" || frameOrigin.empty())
         directives["frame-ancestors"] = "'self'";
      else
         directives["frame-ancestors"] = "'self' " + frameOrigin;
   }

   // In desktop mode, the WebSocket connects to a different port (different
   // origin), so connect-src must include it explicitly. In server mode the
   // WebSocket path is same-origin (relative), so 'self' suffices.
   //
   // Uses the same two-level check as buildWebSocketUrl() in SessionChat.cpp:
   // compile-time #ifdef for server-capable builds, then runtime programMode()
   // check, because Development builds define RSTUDIO_SERVER but can run in
   // either desktop or server mode.
   bool isServerMode = false;

#ifdef RSTUDIO_SERVER
   isServerMode = (options().programMode() == kSessionProgramModeServer);
#endif

   int port = s_chatBackendPort.load();
   if (!isServerMode && port >= 0)
   {
      std::string& connectSrc = directives["connect-src"];
      if (connectSrc.empty())
         connectSrc = "'self'";
      connectSrc += " ws://127.0.0.1:" +
                    boost::lexical_cast<std::string>(port);
   }

   // Serialize directives into the header string
   std::string header;
   for (const auto& pair : directives)
   {
      if (!header.empty())
         header += "; ";
      header += pair.first + " " + pair.second;
   }

   std::lock_guard<std::mutex> lock(s_cspMutex);
   s_cachedCspHeader = header;
   s_cspHeaderBuilt = true;
}

/**
 * Get the Content-Security-Policy header value.
 *
 * Returns a cached string built from dist/csp.json directives, augmented
 * with RStudio-specific additions. The cache is rebuilt lazily on first
 * call and whenever the backend port changes.
 *
 * @return CSP header string
 */
std::string buildCspHeader()
{
   {
      std::lock_guard<std::mutex> lock(s_cspMutex);
      if (s_cspHeaderBuilt)
         return s_cachedCspHeader;
   }
   rebuildCspHeaderCache();
   std::lock_guard<std::mutex> lock(s_cspMutex);
   return s_cachedCspHeader;
}

/**
 * Find the "/ai-chat" route within a request path.
 *
 * The match is anchored at a path-segment boundary, so a root or session
 * prefix that merely contains the route name -- www-root-path=/ai-chat-hub,
 * say -- is not mistaken for the route itself and does not truncate the
 * prefix at the wrong offset. The last whole-segment occurrence wins, since
 * the route is the final segment of the URIs this module serves.
 *
 * @return Offset of the route, or std::string::npos if the path does not
 *         contain it as a whole segment.
 */
size_t findAiChatRoute(const std::string& path)
{
   size_t pos = path.rfind(kAiChatRoute);
   while (pos != std::string::npos)
   {
      size_t end = pos + kAiChatRoute.size();
      if (end == path.length() || path[end] == '/')
         return pos;

      if (pos == 0)
         break;
      pos = path.rfind(kAiChatRoute, pos - 1);
   }

   return std::string::npos;
}

/**
 * The session prefix the server believes it is serving this request under.
 *
 * That is the path of the proxied request URI with the "/ai-chat" route and
 * everything after it removed, e.g. "/s/{id}/" or a configured root path,
 * always ending in "/".
 *
 * @return The prefix, or empty if none could be derived. Empty is not the same
 *         as "/": it means the proxied URI told us nothing about where this
 *         request is served from, so there is no prefix to trust rather than a
 *         prefix that happens to be the origin root.
 */
std::string serverKnownPrefix(const std::string& proxiedUri)
{
   // URL::path() keeps the query string and fragment, which must go first:
   // the IDE puts URLs in the query, and a literal "/ai-chat" there would
   // otherwise be taken for the route
   std::string prefix = http::URL(proxiedUri).path();
   size_t cutPos = prefix.find_first_of("?#");
   if (cutPos != std::string::npos)
      prefix = prefix.substr(0, cutPos);

   // No path at all is not the same as a path of "/". proxiedUri() returns the
   // X-RStudio-Request header verbatim when it is set, so a proxy that puts a
   // bare path or a bare origin there yields a value URL cannot parse, and the
   // path comes back empty. Reporting "/" for that would scope the auth token
   // to every application on the host on the strength of a header we failed to
   // read, so refuse instead -- before the route is stripped, which produces an
   // empty string of its own.
   if (prefix.empty())
      return std::string();

   size_t routePos = findAiChatRoute(prefix);
   if (routePos != std::string::npos)
      prefix = prefix.substr(0, routePos);

   // whereas nothing left after stripping the route means the route was the
   // first segment, i.e. this server really is served from the origin root
   if (prefix.empty())
      return kRequestDefaultRootPath;

   if (prefix.back() != '/')
      prefix += "/";

   return prefix;
}

} // anonymous namespace

std::string authCookiePath(const std::string& proxiedUri)
{
   std::string cookiePath = serverKnownPrefix(proxiedUri);

   // proxiedUri is reconstructed from request headers, and nothing escapes
   // the Set-Cookie header on the way out, so a derived path that could break
   // out of that header -- or that the browser could never match -- must not
   // be used at all. There is nothing narrower to retreat to here: scoping the
   // token to the origin root would hand it to every application on the host,
   // which is worse than the chat pane failing to connect.
   if (!http::util::isValidCookiePath(cookiePath))
   {
      WLOG("Not setting the assistant auth cookie: the path derived from the "
           "request headers cannot be used as a cookie path");
      return std::string();
   }

   return cookiePath;
}

bool isNoStoreExtension(const std::string& extension)
{
   return extension == ".html" || extension == ".htm" ||
          extension == ".js" || extension == ".mjs" ||
          extension == ".css";
}

std::string getContentType(const std::string& extension)
{
   static std::map<std::string, std::string> contentTypes = {
      {".html", "text/html; charset=utf-8"},
      {".js", "application/javascript; charset=utf-8"},
      {".mjs", "application/javascript; charset=utf-8"},
      {".css", "text/css; charset=utf-8"},
      {".json", "application/json; charset=utf-8"},
      {".svg", "image/svg+xml"},
      {".png", "image/png"},
      {".jpg", "image/jpeg"},
      {".jpeg", "image/jpeg"},
      {".gif", "image/gif"},
      {".ico", "image/x-icon"},
      {".woff", "font/woff"},
      {".woff2", "font/woff2"},
      {".ttf", "font/ttf"},
      {".eot", "application/vnd.ms-fontobject"}
   };

   auto it = contentTypes.find(extension);
   if (it != contentTypes.end())
      return it->second;

   return "application/octet-stream";
}

Error validateAndResolvePath(const FilePath& clientRoot,
                             const std::string& requestPath,
                             FilePath* pResolvedPath)
{
   // Remove query string and fragment
   std::string cleanPath = requestPath;
   size_t queryPos = cleanPath.find('?');
   if (queryPos != std::string::npos)
      cleanPath = cleanPath.substr(0, queryPos);

   size_t fragmentPos = cleanPath.find('#');
   if (fragmentPos != std::string::npos)
      cleanPath = cleanPath.substr(0, fragmentPos);

   // URL decode
   cleanPath = http::util::urlDecode(cleanPath);

   // Build full path (use alternative that returns Error instead of requiring output param)
   FilePath resolved;
   Error error = clientRoot.completeChildPath(cleanPath, resolved);
   if (error)
   {
      // Path traversal or invalid path in completeChildPath
      return systemError(boost::system::errc::permission_denied,
                        "Invalid or forbidden path",
                        ERROR_LOCATION);
   }

   // CRITICAL: Canonicalize both paths to resolve symlinks and ".." before security check
   FilePath canonicalRoot;
   error = system::realPath(clientRoot, &canonicalRoot);
   if (error)
      return error;

   // For resolved path, canonicalize if it exists, otherwise check parent
   FilePath canonicalResolved;
   error = system::realPath(resolved, &canonicalResolved);
   if (error)
   {
      // File doesn't exist - canonicalize parent and append filename
      FilePath parent = resolved.getParent();
      FilePath canonicalParent;
      error = system::realPath(parent, &canonicalParent);
      if (error)
      {
         // Parent doesn't exist either - this is suspicious, deny it
         return systemError(boost::system::errc::permission_denied,
                           "Path traversal attempt detected",
                           ERROR_LOCATION);
      }
      canonicalResolved = canonicalParent.completeChildPath(resolved.getFilename());
   }

   // Security: Ensure resolved path is within canonicalized clientRoot
   std::string resolvedStr = canonicalResolved.getAbsolutePath();
   std::string rootStr = canonicalRoot.getAbsolutePath();

   if (!boost::starts_with(resolvedStr, rootStr))
   {
      return systemError(boost::system::errc::permission_denied,
                        "Path traversal attempt detected",
                        ERROR_LOCATION);
   }

   *pResolvedPath = canonicalResolved;
   return Success();
}

Error handleAIChatRequest(const http::Request& request,
                          http::Response* pResponse)
{
   // Locate installation
   FilePath positAiPath = locatePositAssistantInstallation();
   if (positAiPath.isEmpty())
   {
      pResponse->setStatusCode(http::status::NotFound);
      pResponse->setBody("Posit Assistant not installed.");
      return Success();
   }

   FilePath clientRoot = positAiPath.completeChildPath(kClientDirPath);

   // Parse requested path from URI
   // URI format: /ai-chat/<path>
   //
   // Match on request.path(), which is the URI truncated at "?", and anchor
   // the match at the start. The handler is registered for the bare "/ai-chat"
   // prefix, so searching the whole URI let a request for "/ai-chat" serve a
   // file named in its own query string, and let junk-suffixed spellings of
   // the route ("/ai-chatXYZ/ai-chat/index.html") through. Both served real
   // files, so any cache rule or WAF reasoning about the URI path saw
   // something other than what was served.
   std::string path = request.path();
   if (!boost::starts_with(path, kAiChatUriPrefix))
   {
      pResponse->setStatusCode(http::status::BadRequest);
      return Success();
   }

   std::string requestPath = path.substr(kAiChatUriPrefix.size());

   // request.path() truncates at "?" but not at "#". Browsers never transmit
   // a fragment, but strip it so that a hand-written request still resolves
   // to a file instead of falling through to the client root directory.
   size_t fragmentPos = requestPath.find('#');
   if (fragmentPos != std::string::npos)
      requestPath = requestPath.substr(0, fragmentPos);

   // Default to index.html
   if (requestPath.empty() || requestPath == "/")
      requestPath = kIndexFileName;

   // Validate and resolve path
   FilePath resolvedPath;
   Error error = validateAndResolvePath(clientRoot, requestPath, &resolvedPath);
   if (error)
   {
      pResponse->setStatusCode(http::status::Forbidden);
      return Success();
   }

   // Check if file exists
   if (!resolvedPath.exists())
   {
      pResponse->setStatusCode(http::status::NotFound);
      return Success();
   }

   // Read file content
   std::string content;
   error = readStringFromFile(resolvedPath, &content);
   if (error)
   {
      pResponse->setStatusCode(http::status::InternalServerError);
      return error;
   }

   // Set content type. Fold the case: on Windows realPath is the purely
   // lexical GetFullPathNameW, which never reaches the filesystem and so does
   // not correct the case of the requested name. An uppercase extension would
   // otherwise miss every rule below, taking the long-lived cache branch and
   // skipping the CSP header.
   std::string extension = resolvedPath.getExtensionLowerCase();

   // For HTML files: set CSP header; inject theme info only into index.html
   if (extension == ".html" || extension == ".htm")
   {
      if (resolvedPath.getFilename() == kIndexFileName)
      {
         injectThemeInfo(&content);

         // In server mode, deliver the auth token via an HTTP-only cookie
         // instead of a URL query parameter. This prevents the token from
         // leaking in browser history, server logs, and the Referer header.
         // Desktop mode continues to use the URL parameter since it's
         // localhost-only.
         bool isServerMode = false;
#ifdef RSTUDIO_SERVER
         isServerMode = (options().programMode() == kSessionProgramModeServer);
#endif
         if (isServerMode)
         {
            std::lock_guard<std::mutex> lock(s_authTokenMutex);
            if (!s_chatBackendAuthToken.empty())
            {
               // Scope the cookie to the session prefix (e.g. "/s/{id}/") so
               // multi-session deployments don't collide.
               std::string cookiePath = authCookiePath(request.proxiedUri());

               // an empty path means no path can be scoped safely for this
               // request; authCookiePath has already logged why
               if (!cookiePath.empty())
               {
                  LOG_DEBUG_MESSAGE("Cookie path for posit-assistant-auth: '" +
                                    cookiePath + "'");

                  http::Cookie cookie(
                     request, "posit-assistant-auth", s_chatBackendAuthToken,
                     cookiePath, options().sameSite(), true /* httpOnly */,
                     options().useSecureCookies());
                  pResponse->addCookie(cookie);
               }
            }
         }
      }
      pResponse->setHeader("Content-Security-Policy", buildCspHeader());
   }
   pResponse->setContentType(getContentType(extension));

   // Set caching headers, classifying by the resolved file rather than by the
   // requested path. The request path is URL-decoded during resolution, so a
   // request for "%69ndex.html" resolves to index.html and is served the auth
   // token cookie; classifying by the raw path would then miss the no-store
   // rule and invite shared caches to store that response.
   if (isNoStoreExtension(extension))
   {
      // Don't cache HTML, JS, or CSS files to avoid stale cache issues during development
      // Use multiple headers to ensure cache is disabled across all browsers and proxies
      pResponse->setHeader("Cache-Control", "no-cache, no-store, must-revalidate, max-age=0");
      pResponse->setHeader("Pragma", "no-cache");  // HTTP/1.0 compatibility
      pResponse->setHeader("Expires", "0");        // Proxy cache control
   }
   else if (!extension.empty())
   {
      // Cache other assets like images, fonts, etc.
      pResponse->setHeader("Cache-Control", "public, max-age=31536000");
   }

   pResponse->setStatusCode(http::status::Ok);
   pResponse->setBody(content);

   return Success();
}

void setChatBackendPort(int port)
{
   s_chatBackendPort = port;
   rebuildCspHeaderCache();
}

void setChatBackendAuthToken(const std::string& token)
{
   std::lock_guard<std::mutex> lock(s_authTokenMutex);
   s_chatBackendAuthToken = token;
}

} // namespace staticfiles
} // namespace chat
} // namespace modules
} // namespace session
} // namespace rstudio
