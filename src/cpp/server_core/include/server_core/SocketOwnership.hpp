/*
 * SocketOwnership.hpp
 *
 * Copyright (C) 2026 by Posit Software, PBC
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

#ifndef SERVER_CORE_SOCKET_OWNERSHIP_HPP
#define SERVER_CORE_SOCKET_OWNERSHIP_HPP

#include <sys/types.h>
#include <boost/asio/ip/address.hpp>
#include <shared_core/Error.hpp>

namespace rstudio {
namespace server_core {
namespace socket_utils {

// Error property set (to "1") on any Error that verifyPeerUid() returns
// (rstudio-pro#11470). Callers that surface the error over HTTP (see
// ServerSessionProxy.cpp / SessionProxy.cpp's handleLocalhostError()) check
// for this property to map the rejection to a 403 Forbidden response instead
// of the generic 500 Internal Server Error that an arbitrary connection
// failure would produce -- 403 is the accurate status for "the ownership
// check declined this request," while leaving unrelated connection errors
// (target app down, timeout, etc.) on the existing 500 path.
constexpr const char* kPortOwnershipRejectedProperty = "port-ownership-rejected";

// Look up the UID owning the *established*, server-side TCP socket for a loopback
// connection identified by its reversed 4-tuple: the server-side socket has
// source port == appPort (the listening/target port) and dest port ==
// ephemeralPort (our client's local port from getsockname()). localAddress and
// remoteAddress are our own connected socket's local/remote endpoint addresses
// (i.e. from the caller's point of view, not the server-side socket's) -- they
// seed an exact-match NETLINK_SOCK_DIAG query for the reversed 4-tuple. This
// also correctly finds a dual-stack (IPV6_V6ONLY=0) listener's accepted socket
// even when dialed via plain IPv4: although such a socket's sk_family is
// AF_INET6, the kernel hashes it (and matches lookups against it) using its
// IPv4-mapped address in the same v4-style hash used for plain AF_INET
// sockets, and the established-socket comparator never checks sk_family --
// only the dump path filters by family, which this exact-match query avoids.
// Returns Success and sets *pUid on an exact match; returns an error if the query
// fails or no matching ESTABLISHED socket exists (caller must fail closed).
core::Error lookupEstablishedSocketUid(const boost::asio::ip::address& localAddress,
                                       const boost::asio::ip::address& remoteAddress,
                                       int appPort,
                                       int ephemeralPort,
                                       uid_t* pUid);

// Look up the UID owning the *listening* socket bound to (listenAddress, listenPort).
// Unlike lookupEstablishedSocketUid(), this targets the listener itself (dest port/
// address wildcarded), which always has sk_socket set and therefore reports the
// correct uid on every kernel version -- see OwnershipCheckMode::Listener below.
// Returns Success and sets *pUid on an exact match; returns an error if the query
// fails or no matching listening socket exists (caller must fail closed).
core::Error lookupListeningSocketUid(const boost::asio::ip::address& listenAddress,
                                     int listenPort,
                                     uid_t* pUid);

// Verify the peer of a just-established localhost proxy hop is owned by
// expectedUid, using the verification strategy selected by probeOwnershipCheckMode()
// (#18439). Returns Success only when the owning UID equals expectedUid.
core::Error verifyPeerUid(const boost::asio::ip::address& localAddress,
                          const boost::asio::ip::address& remoteAddress,
                          int appPort,
                          int ephemeralPort,
                          uid_t expectedUid);

// Per-request verification strategy selected once by the startup probe (see
// probeOwnershipCheckMode() below). Established-socket uid lookups are unreliable
// on kernels lacking upstream commit c51da3f7a161 ("net: remove sock_i_uid()",
// first in v6.17): on those kernels, an accepted TCP child socket that the target
// application hasn't yet accept()'d reports owner uid 0 (#18439), which this
// process's own startup probe below can detect deterministically.
enum class OwnershipCheckMode
{
   Disabled,    // NETLINK_SOCK_DIAG unusable; enforcement skipped entirely
   Listener,    // query the listening socket (safe on every kernel version)
   Established  // query the established socket (kernel confirmed reliable)
};

// One-time capability probe (#18439): determines which
// OwnershipCheckMode this process should use for per-request ownership checks.
// The result is computed once on first call (using a deterministic repro: an
// ephemeral loopback listener that is connect()'d to but never accept()'d,
// exercising the same pre-accept race that production traffic can hit) and
// cached for the process lifetime; a warning is logged once if enforcement ends
// up Disabled. Enforcement sites call this to decide both whether to enforce at
// all and, if so, which query strategy to use. It does NOT relax per-request
// fail-closed behavior: in Listener or Established mode, verifyPeerUid() errors
// still reject.
OwnershipCheckMode probeOwnershipCheckMode();

} // namespace socket_utils
} // namespace server_core
} // namespace rstudio

#endif
