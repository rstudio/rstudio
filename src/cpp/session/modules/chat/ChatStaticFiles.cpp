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

#include <map>
#include <boost/algorithm/string.hpp>
#include <fmt/format.h>

#include <core/FileSerializer.hpp>
#include <core/http/Request.hpp>
#include <core/http/Response.hpp>
#include <core/http/URL.hpp>
#include <core/http/Util.hpp>
#include <core/system/System.hpp>

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

/**
 * Inject theme-related modifications into HTML content:
 * - CSS variables for --ui-background and --ui-foreground
 * - 'dark' class on html element if using a dark theme
 *
 * Injects a script immediately after <head> to ensure it runs before
 * any other scripts that may depend on these values.
 */
void injectThemeInfo(std::string* pContent)
{
   themes::ThemeColors colors = themes::getThemeColors();

   // Build script injection
   std::string script = fmt::format(R"(
<script>
(function() {{
   var root = document.documentElement;
   root.style.setProperty('--ui-background', '{background}');
   root.style.setProperty('--ui-foreground', '{foreground}');
   if ({isDark}) root.classList.add('dark');
}})();
</script>)",
      fmt::arg("background", colors.background),
      fmt::arg("foreground", colors.foreground),
      fmt::arg("isDark", colors.isDark ? "true" : "false"));

   // Insert immediately after <head>
   constexpr const char* kHeadTag = "<head>";
   size_t headPos = pContent->find(kHeadTag);
   if (headPos != std::string::npos)
   {
      pContent->insert(headPos + strlen(kHeadTag), script);
   }
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

   // Inject theme CSS variables into HTML files
   if (extension == ".html" || extension == ".htm")
   {
      injectThemeInfo(&content);
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

} // namespace staticfiles
} // namespace chat
} // namespace modules
} // namespace session
} // namespace rstudio
