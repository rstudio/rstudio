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
 * - 'dark' class on body element if using a dark theme
 */
void injectThemeInfo(std::string* pContent)
{
   themes::ThemeColors colors = themes::getThemeColors();

   // Build CSS injection
   std::string css = fmt::format(R"(
<style>
:root {{
   --ui-background: {background};
   --ui-foreground: {foreground};
}}
</style>)",
      fmt::arg("background", colors.background),
      fmt::arg("foreground", colors.foreground));

   // Find </head> tag and inject CSS (case-insensitive search)
   std::string lowerContent = boost::to_lower_copy(*pContent);
   size_t headPos = lowerContent.find("</head>");
   if (headPos != std::string::npos)
   {
      pContent->insert(headPos, css);
      // Update lower content to reflect the insertion
      lowerContent = boost::to_lower_copy(*pContent);
   }

   // Add 'dark' class to body if theme is dark
   if (colors.isDark)
   {
      size_t bodyPos = lowerContent.find("<body");
      if (bodyPos != std::string::npos)
      {
         size_t bodyEnd = pContent->find('>', bodyPos);
         if (bodyEnd != std::string::npos)
         {
            // Check if there's an existing class attribute
            std::string bodyTag = pContent->substr(bodyPos, bodyEnd - bodyPos);
            std::string lowerBodyTag = boost::to_lower_copy(bodyTag);
            size_t classPos = lowerBodyTag.find("class=");

            if (classPos != std::string::npos)
            {
               // Find the opening quote of the class attribute
               size_t quotePos = bodyTag.find_first_of("\"'", classPos + 6);
               if (quotePos != std::string::npos)
               {
                  // Insert 'dark ' after the opening quote
                  pContent->insert(bodyPos + quotePos + 1, "dark ");
               }
            }
            else
            {
               // No class attribute, add one before the closing >
               pContent->insert(bodyEnd, " class=\"dark\"");
            }
         }
      }
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
