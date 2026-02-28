/*
 * ChatStaticFiles.hpp
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

#ifndef SESSION_CHAT_STATIC_FILES_HPP
#define SESSION_CHAT_STATIC_FILES_HPP

#include <string>
#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

namespace rstudio {

namespace core {
namespace http {
   class Request;
   class Response;
}
}

namespace session {
namespace modules {
namespace chat {
namespace staticfiles {

// ============================================================================
// Content Type Detection
// ============================================================================

/**
 * Get the MIME content type for a file extension.
 *
 * Supports common web file types including HTML, CSS, JavaScript, images, fonts.
 *
 * @param extension File extension including the dot (e.g., ".html", ".js")
 * @return MIME content type string (e.g., "text/html; charset=utf-8")
 *         Returns "application/octet-stream" for unknown extensions
 */
std::string getContentType(const std::string& extension);

// ============================================================================
// Path Validation
// ============================================================================

/**
 * Validate and resolve a request path against a client root directory.
 *
 * Security features:
 * - Removes query strings and fragments
 * - URL decodes the path
 * - Resolves symlinks and ".." via realPath canonicalization
 * - Ensures resolved path is within clientRoot (prevents path traversal)
 *
 * @param clientRoot Root directory for serving files (e.g., dist/client)
 * @param requestPath Requested file path (may include query/fragment)
 * @param pResolvedPath Output parameter for the validated absolute path
 * @return Success() if valid, error if path traversal detected or resolution fails
 */
core::Error validateAndResolvePath(const core::FilePath& clientRoot,
                                   const std::string& requestPath,
                                   core::FilePath* pResolvedPath);

// ============================================================================
// HTTP Request Handler
// ============================================================================

/**
 * Handle HTTP requests for AI Chat static files.
 *
 * Serves files from the Posit AI installation's client directory.
 * URI format: /ai-chat/<path>
 * Defaults to index.html for "/" requests.
 *
 * Security:
 * - Uses validateAndResolvePath to prevent directory traversal
 * - Only serves files from verified installation directory
 * - Sets Content-Security-Policy header on HTML responses
 *
 * Caching:
 * - HTML/JS/CSS: no-cache (for development)
 * - Other assets: 1 year cache
 *
 * @param request HTTP request object
 * @param pResponse HTTP response object to populate
 * @return Success() on normal handling (including 4xx responses), or an
 *         Error if file I/O fails
 */
core::Error handleAIChatRequest(const core::http::Request& request,
                                core::http::Response* pResponse);

// ============================================================================
// Chat Backend Port
// ============================================================================

/**
 * Set the chat backend (databot) port.
 *
 * Called by SessionChat when the backend process starts or stops. Used to
 * build the connect-src CSP directive in desktop mode, where the WebSocket
 * connects to a different origin than the page. Pass -1 to indicate the
 * backend is not running.
 */
void setChatBackendPort(int port);

} // namespace staticfiles
} // namespace chat
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_CHAT_STATIC_FILES_HPP
