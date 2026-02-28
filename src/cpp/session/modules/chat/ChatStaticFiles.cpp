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
#include <boost/algorithm/string.hpp>
#include <boost/lexical_cast.hpp>
#include <fmt/format.h>

#include <core/FileSerializer.hpp>
#include <core/StringUtils.hpp>
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

// URI prefix for AI Chat requests
constexpr const char* kAiChatUriPrefix = "/ai-chat/";
constexpr size_t kAiChatUriPrefixLength = 9; // Length of "/ai-chat/"

// Chat backend port, set by SessionChat.cpp when the backend starts.
// Used to build connect-src in the CSP header for desktop mode.
// Atomic because it is written from the main thread and read from HTTP
// handler threads.
std::atomic<int> s_chatBackendPort{kChatBackendPortNone};

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
 * Load CSP directives from dist/csp.json in the Posit AI installation.
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

      FilePath positAiPath = locatePositAiInstallation();
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

   // Prevent clickjacking: the chat pane should never be framed by
   // external origins.
   if (directives.find("frame-ancestors") == directives.end())
      directives["frame-ancestors"] = "'self'";

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

} // anonymous namespace

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
   FilePath positAiPath = locatePositAiInstallation();
   if (positAiPath.isEmpty())
   {
      pResponse->setStatusCode(http::status::NotFound);
      pResponse->setBody("Posit AI not installed.");
      return Success();
   }

   FilePath clientRoot = positAiPath.completeChildPath(kClientDirPath);

   // Parse requested path from URI
   // URI format: /ai-chat/<path>
   std::string uri = request.uri();
   size_t pos = uri.find(kAiChatUriPrefix);
   if (pos == std::string::npos)
   {
      pResponse->setStatusCode(http::status::BadRequest);
      return Success();
   }

   std::string requestPath = uri.substr(pos + kAiChatUriPrefixLength);

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

   // Set content type
   std::string extension = resolvedPath.getExtension();

   // For HTML files: set CSP header; inject theme info only into index.html
   if (extension == ".html" || extension == ".htm")
   {
      if (resolvedPath.getFilename() == kIndexFileName)
         injectThemeInfo(&content);
      pResponse->setHeader("Content-Security-Policy", buildCspHeader());
   }
   pResponse->setContentType(getContentType(extension));

   // Set caching headers
   if (boost::ends_with(requestPath, kIndexFileName) ||
       boost::ends_with(requestPath, ".js") ||
       boost::ends_with(requestPath, ".css"))
   {
      // Don't cache HTML, JS, or CSS files to avoid stale cache issues during development
      // Use multiple headers to ensure cache is disabled across all browsers and proxies
      pResponse->setHeader("Cache-Control", "no-cache, no-store, must-revalidate, max-age=0");
      pResponse->setHeader("Pragma", "no-cache");  // HTTP/1.0 compatibility
      pResponse->setHeader("Expires", "0");        // Proxy cache control
   }
   else if (requestPath.find(".") != std::string::npos)
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

} // namespace staticfiles
} // namespace chat
} // namespace modules
} // namespace session
} // namespace rstudio
