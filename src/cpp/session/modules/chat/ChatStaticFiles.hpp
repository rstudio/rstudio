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

/**
 * Should a response serving a file with this extension be kept out of caches?
 *
 * True for HTML, JavaScript, and CSS: these change with each Posit Assistant
 * build, and the index.html response also carries the assistant auth token
 * cookie, which must never be stored by a shared cache. Callers must pass the
 * extension of the *resolved* file -- the requested path is URL-decoded during
 * resolution, so it can spell the same file in more than one way.
 *
 * @param extension File extension including the dot (e.g., ".html")
 * @return true if the response should be served no-store
 */
bool isNoStoreExtension(const std::string& extension);

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
// Auth Cookie Path
// ============================================================================

/**
 * Compute the Path attribute for the posit-assistant-auth cookie.
 *
 * The cookie is scoped to the session prefix (e.g. "/s/{id}/" or the
 * configured root path) so multi-session deployments don't collide. The
 * server-known prefix is taken from proxiedUri with "/ai-chat" onward
 * stripped, matching the route only at a path-segment boundary so that a
 * root path which contains the route name (www-root-path=/ai-chat-hub) is
 * not truncated at the wrong offset. When the browser reaches the server
 * through a path-prefixing
 * reverse proxy the server was never told about, that prefix is recovered
 * from the client-reported base URL (the clientBaseUrl query parameter added
 * by the IDE frontend).
 *
 * This request is a plain GET, so its query string is not trusted on its own:
 * a crafted link could otherwise scope the auth token to an attacker's route
 * on the same host. clientBaseUrl is honored only when it yields the same
 * path as activeClientUrl -- the base recorded by the CSRF-protected
 * client_init request, which an attacker cannot set.
 *
 * Keeping the value request-scoped means the cookie is scoped to the path the
 * requesting tab actually uses, rather than to whichever tab initialized the
 * session last. activeClientUrl records only the most recent client_init, so
 * when two IDE tabs reach the server by different external paths, the tab that
 * did not initialize last falls back to the server-known prefix and its chat
 * cannot connect -- a fallback that is wrong for nobody rather than a cookie
 * scoped to a path the requesting tab never uses.
 *
 * Returns empty when no path can be scoped safely, in which case the caller
 * must not set the cookie: either proxiedUri yielded something unusable in a
 * Set-Cookie header, or the fallback described above would be the origin root
 * while the session reported something narrower, which would share the token
 * with every application on the host. Both cases are logged.
 *
 * @param proxiedUri The server's view of the request URL (request.proxiedUri())
 * @param clientBaseUrl The browser-visible base URL reported by the IDE
 *                      frontend, or empty if not provided
 * @param activeClientUrl The base URL recorded at client_init
 *                        (persistentState().activeClientUrl())
 * @return Cookie path ending with "/", or empty if the cookie must not be
 *         set; equals the server-known prefix unless both reported base URLs
 *         agree on an external prefix
 */
std::string authCookiePath(const std::string& proxiedUri,
                           const std::string& clientBaseUrl,
                           const std::string& activeClientUrl);

// ============================================================================
// HTTP Request Handler
// ============================================================================

/**
 * Handle HTTP requests for Posit Assistant Chat static files.
 *
 * Serves files from the Posit Assistant installation's client directory.
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

/**
 * Set the chat backend auth token.
 *
 * Called by SessionChat when the backend process starts or stops. In server
 * mode, this token is delivered to the PA client via an HTTP-only cookie on
 * the index.html response (rather than as a URL query parameter) to avoid
 * leaking credentials in browser history, server logs, and the Referer header.
 * Pass an empty string to clear.
 */
void setChatBackendAuthToken(const std::string& token);

} // namespace staticfiles
} // namespace chat
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_CHAT_STATIC_FILES_HPP
