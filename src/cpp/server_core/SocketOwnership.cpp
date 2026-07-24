/*
 * SocketOwnership.cpp
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

#include <server_core/SocketOwnership.hpp>

#include <core/BoostErrors.hpp>
#include <core/Log.hpp>

// NETLINK_SOCK_DIAG is Linux-specific; there is no portable equivalent for
// looking up the owning uid of an established loopback socket. rserver is
// dev-only on non-Linux platforms, so this degrades to "enforcement
// unavailable" there rather than failing to build (rstudio-pro#11470).
#ifdef __linux__

#include <cstring>
#include <string>
#include <vector>

#include <boost/asio/ip/address_v4.hpp>

#include <sys/socket.h>
#include <sys/time.h>
#include <arpa/inet.h>
#include <netinet/tcp.h>
#include <linux/netlink.h>
#include <linux/sock_diag.h>
#include <linux/inet_diag.h>
#include <unistd.h>

#endif // __linux__

using namespace rstudio::core;

namespace rstudio {
namespace server_core {
namespace socket_utils {

#ifdef __linux__

namespace {

// nlmsghdr + inet_diag_req_v2/inet_diag_msg + a small margin for the kernel's
// NLMSG_ALIGN padding -- an exact-match reply carries at most one
// inet_diag_msg record (idiag_ext is left at 0, so no extended attributes are
// requested), unlike the old NLM_F_DUMP path which could return many.
constexpr size_t kNetlinkRecvBufferSize = 512;

// This query runs synchronously on every proxied request; SO_RCVTIMEO bounds
// how long a stalled/unresponsive kernel reply can block that request before
// we fail closed rather than hang indefinitely.
constexpr int kNetlinkRecvTimeoutSeconds = 3;

// Sets SO_RCVTIMEO on fd so a subsequent recvmsg() cannot block longer than
// kNetlinkRecvTimeoutSeconds -- this applies generically to any socket family
// (handled by the kernel's common sock_setsockopt(), not netlink-specific
// code) and is honored by netlink_recvmsg() via the same sock_rcvtimeo()
// datagram-wait path used by other socket types.
Error setRecvTimeout(int fd)
{
   timeval tv;
   tv.tv_sec = kNetlinkRecvTimeoutSeconds;
   tv.tv_usec = 0;
   if (::setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, &tv, sizeof(tv)) < 0)
      return systemError(errno, "setsockopt(NETLINK_SOCK_DIAG, SO_RCVTIMEO)", ERROR_LOCATION);
   return Success();
}

struct DiagRequest
{
   nlmsghdr nlh;
   inet_diag_req_v2 req;
};

// Builds the exact-match inet_diag_sockid for the *server-side* (application)
// end of the proxy hop: source port == appPort (the app's listening port),
// dest port == ephemeralPort (our own outbound connect's local port), with
// srcAddr/dstAddr in the corresponding positions. srcAddr and dstAddr must
// already be the same family (both v4, or both v6/v4-mapped-v6); family is
// taken from srcAddr.
void fillSockId(inet_diag_sockid* pId,
                const boost::asio::ip::address& srcAddr,
                const boost::asio::ip::address& dstAddr,
                uint16_t sport,
                uint16_t dport)
{
   std::memset(pId, 0, sizeof(*pId));
   pId->idiag_sport = htons(sport);
   pId->idiag_dport = htons(dport);
   pId->idiag_if = 0; // loopback sockets are not bound to a specific device

   // INET_DIAG_NOCOOKIE: match by tuple alone, ignore the socket cookie.
   pId->idiag_cookie[0] = 0xFFFFFFFF;
   pId->idiag_cookie[1] = 0xFFFFFFFF;

   if (srcAddr.is_v4())
   {
      auto srcBytes = srcAddr.to_v4().to_bytes();
      auto dstBytes = dstAddr.to_v4().to_bytes();
      std::memcpy(&pId->idiag_src[0], srcBytes.data(), srcBytes.size());
      std::memcpy(&pId->idiag_dst[0], dstBytes.data(), dstBytes.size());
   }
   else
   {
      auto srcBytes = srcAddr.to_v6().to_bytes();
      auto dstBytes = dstAddr.to_v6().to_bytes();
      std::memcpy(&pId->idiag_src[0], srcBytes.data(), srcBytes.size());
      std::memcpy(&pId->idiag_dst[0], dstBytes.data(), dstBytes.size());
   }
}

// Sends a non-dump (exact-match) NETLINK_SOCK_DIAG request for the single
// established TCP socket identified by (srcAddr, sport) <-> (dstAddr, dport).
// Without NLM_F_DUMP, the kernel performs a direct hash-table lookup for this
// 4-tuple rather than dumping and filtering every established socket on the
// host -- this is the performance-critical difference from the old
// dump-based implementation, since this runs on every proxied request.
Error sendExactMatchRequest(int fd,
                            const boost::asio::ip::address& srcAddr,
                            const boost::asio::ip::address& dstAddr,
                            uint16_t sport,
                            uint16_t dport)
{
   DiagRequest request;
   std::memset(&request, 0, sizeof(request));

   request.nlh.nlmsg_len = sizeof(request);
   request.nlh.nlmsg_type = SOCK_DIAG_BY_FAMILY;
   request.nlh.nlmsg_flags = NLM_F_REQUEST;
   request.nlh.nlmsg_seq = 1;
   request.nlh.nlmsg_pid = 0;

   request.req.sdiag_family = srcAddr.is_v4() ? AF_INET : AF_INET6;
   request.req.sdiag_protocol = IPPROTO_TCP;
   request.req.idiag_ext = 0;
   request.req.pad = 0;
   request.req.idiag_states = (1u << TCP_ESTABLISHED);
   fillSockId(&request.req.id, srcAddr, dstAddr, sport, dport);

   sockaddr_nl dest;
   std::memset(&dest, 0, sizeof(dest));
   dest.nl_family = AF_NETLINK;
   dest.nl_pid = 0;    // destined for the kernel
   dest.nl_groups = 0;

   ssize_t sent = ::sendto(fd,
                            &request,
                            sizeof(request),
                            0,
                            reinterpret_cast<sockaddr*>(&dest),
                            sizeof(dest));
   if (sent < 0)
      return systemError(errno, "sendto(NETLINK_SOCK_DIAG exact-match request)", ERROR_LOCATION);
   if (static_cast<size_t>(sent) != sizeof(request))
      return systemError(EIO, "short write sending NETLINK_SOCK_DIAG exact-match request", ERROR_LOCATION);

   return Success();
}

enum class DiagLookupResult
{
   Found,
   NotFound,
   Error
};

// Reads the single-message reply to an exact-match request (see
// sendExactMatchRequest): either one SOCK_DIAG_BY_FAMILY/inet_diag_msg record
// (Found, *pUid set), one NLMSG_ERROR with -ENOENT (NotFound -- no socket
// matches this exact 4-tuple, a normal outcome, not a transport failure), or
// any other NLMSG_ERROR / recvmsg() failure (Error, *pError set). Unlike the
// old dump reply, there is no NLMSG_DONE framing to loop for -- a single
// recvmsg() always suffices.
DiagLookupResult readSingleDiagReply(int fd, uid_t* pUid, Error* pError)
{
   std::vector<char> buffer(kNetlinkRecvBufferSize);

   iovec iov;
   iov.iov_base = buffer.data();
   iov.iov_len = buffer.size();

   msghdr msg;
   std::memset(&msg, 0, sizeof(msg));
   msg.msg_iov = &iov;
   msg.msg_iovlen = 1;

   ssize_t received;
   do
   {
      received = ::recvmsg(fd, &msg, 0);
   } while (received < 0 && errno == EINTR);

   if (received < 0)
   {
      if (errno == EAGAIN) // == EWOULDBLOCK on Linux; SO_RCVTIMEO expiring surfaces as this
      {
         *pError = systemError(boost::system::errc::timed_out,
                               "recvmsg(NETLINK_SOCK_DIAG) timed out after " +
                                  std::to_string(kNetlinkRecvTimeoutSeconds) + "s",
                               ERROR_LOCATION);
      }
      else
      {
         *pError = systemError(errno, "recvmsg(NETLINK_SOCK_DIAG)", ERROR_LOCATION);
      }
      return DiagLookupResult::Error;
   }
   if (received == 0)
   {
      *pError = systemError(EIO, "NETLINK_SOCK_DIAG socket closed unexpectedly", ERROR_LOCATION);
      return DiagLookupResult::Error;
   }
   if (msg.msg_flags & MSG_TRUNC)
   {
      *pError = systemError(EMSGSIZE,
                            "NETLINK_SOCK_DIAG reply truncated (buffer too small)",
                            ERROR_LOCATION);
      return DiagLookupResult::Error;
   }

   auto* nlh = reinterpret_cast<nlmsghdr*>(buffer.data());
   auto remaining = static_cast<size_t>(received);

   if (!NLMSG_OK(nlh, remaining))
   {
      *pError = systemError(EIO, "malformed NETLINK_SOCK_DIAG reply", ERROR_LOCATION);
      return DiagLookupResult::Error;
   }

   if (nlh->nlmsg_type == NLMSG_ERROR)
   {
      auto* err = reinterpret_cast<nlmsgerr*>(NLMSG_DATA(nlh));
      if (err->error == -ENOENT)
         return DiagLookupResult::NotFound;

      *pError = systemError(-err->error,
                            "NETLINK_SOCK_DIAG exact-match query rejected by kernel",
                            ERROR_LOCATION);
      return DiagLookupResult::Error;
   }

   if (nlh->nlmsg_type == SOCK_DIAG_BY_FAMILY)
   {
      auto* diag = reinterpret_cast<inet_diag_msg*>(NLMSG_DATA(nlh));
      *pUid = diag->idiag_uid; // populated unconditionally (research Q8)
      return DiagLookupResult::Found;
   }

   *pError = systemError(EIO, "unexpected NETLINK_SOCK_DIAG reply message type", ERROR_LOCATION);
   return DiagLookupResult::Error;
}

// Issues one exact-match query/reply round trip on fd and reports the result.
DiagLookupResult queryOnce(int fd,
                          const boost::asio::ip::address& srcAddr,
                          const boost::asio::ip::address& dstAddr,
                          uint16_t sport,
                          uint16_t dport,
                          uid_t* pUid,
                          Error* pError)
{
   Error sendError = sendExactMatchRequest(fd, srcAddr, dstAddr, sport, dport);
   if (sendError)
   {
      *pError = sendError;
      return DiagLookupResult::Error;
   }

   return readSingleDiagReply(fd, pUid, pError);
}

} // anonymous namespace

Error lookupEstablishedSocketUid(const boost::asio::ip::address& localAddress,
                                 const boost::asio::ip::address& remoteAddress,
                                 int appPort,
                                 int ephemeralPort,
                                 uid_t* pUid)
{
   int fd = ::socket(AF_NETLINK, SOCK_RAW | SOCK_CLOEXEC, NETLINK_SOCK_DIAG);
   if (fd < 0)
      return systemError(errno, "socket(NETLINK_SOCK_DIAG)", ERROR_LOCATION);

   Error timeoutError = setRecvTimeout(fd);
   if (timeoutError)
   {
      // Non-fatal: proceed without a receive timeout rather than fail closed
      // over what is expected to be an untaken code path (SO_RCVTIMEO is a
      // universally-supported SOL_SOCKET option) -- worst case we're back to
      // the unbounded-wait behavior this timeout is meant to improve upon.
      LOG_WARNING_MESSAGE(
         "Failed to set NETLINK_SOCK_DIAG receive timeout (" + timeoutError.getSummary() +
         "); proceeding without a receive timeout for this query (rstudio-pro#11470).");
   }

   // The server-side (application) socket has source port == appPort, dest
   // port == ephemeralPort -- the reverse of our own connect()'d socket. This
   // single exact-match query also finds a dual-stack (IPV6_V6ONLY=0)
   // listener's accepted socket despite being dialed via plain IPv4: the
   // kernel's established-socket comparator (inet_match()) never checks
   // sk_family, and such a socket is hashed under its IPv4-mapped address
   // using the same v4-style hash as a plain AF_INET socket -- only the
   // NLM_F_DUMP path filters strictly by sk_family, and this query doesn't
   // use it.
   Error error;
   DiagLookupResult result = queryOnce(fd,
                                      remoteAddress,
                                      localAddress,
                                      static_cast<uint16_t>(appPort),
                                      static_cast<uint16_t>(ephemeralPort),
                                      pUid,
                                      &error);

   ::close(fd);

   if (result == DiagLookupResult::Error)
      return error;

   if (result == DiagLookupResult::NotFound)
   {
      return systemError(boost::system::errc::no_such_file_or_directory,
                         "No established socket for requested 4-tuple",
                         ERROR_LOCATION);
   }

   return Success();
}

Error verifyPeerUid(const boost::asio::ip::address& localAddress,
                    const boost::asio::ip::address& remoteAddress,
                    int appPort,
                    int ephemeralPort,
                    uid_t expectedUid)
{
   uid_t ownerUid = 0;
   Error error = lookupEstablishedSocketUid(localAddress, remoteAddress, appPort, ephemeralPort, &ownerUid);
   if (error)
   {
      // could not verify -> caller rejects; tag so the HTTP layer can map this
      // to 403 rather than a generic 500 (rstudio-pro#11470)
      error.addProperty(kPortOwnershipRejectedProperty, "1");
      return error;
   }

   if (ownerUid != expectedUid)
   {
      Error mismatchError = systemError(boost::system::errc::permission_denied,
                                        "Port owner uid " + std::to_string(ownerUid) +
                                           " does not match expected uid " + std::to_string(expectedUid),
                                        ERROR_LOCATION);
      mismatchError.addProperty(kPortOwnershipRejectedProperty, "1");
      return mismatchError;
   }

   return Success();
}

bool probeSockDiagAvailable()
{
   // Computed once, cached for the process lifetime. A function-local static
   // initialized from a lambda gives the required thread-safe one-time init.
   static const bool available = []() -> bool {
      int fd = ::socket(AF_NETLINK, SOCK_RAW | SOCK_CLOEXEC, NETLINK_SOCK_DIAG);
      if (fd < 0)
      {
         LOG_WARNING_MESSAGE(
            "NETLINK_SOCK_DIAG unavailable (" + std::string(::strerror(errno)) +
            "); port-proxy ownership enforcement is DISABLED for this process "
            "(rstudio-pro#11470). Cross-user /p/ and /p6/ isolation will not be "
            "enforced in this environment.");
         return false;
      }

      Error timeoutError = setRecvTimeout(fd);
      if (timeoutError)
      {
         // Non-fatal, matching lookupEstablishedSocketUid(): proceed with the
         // probe (and thus with enforcement) even without a receive timeout.
         LOG_WARNING_MESSAGE(
            "Failed to set NETLINK_SOCK_DIAG receive timeout (" + timeoutError.getSummary() +
            "); proceeding without a receive timeout for this query (rstudio-pro#11470).");
      }

      // Probe with a deliberately absent 4-tuple, exercising the same
      // exact-match code path production traffic uses. Either a Found or
      // NotFound (-ENOENT) reply means the kernel accepted and answered the
      // query -- the mechanism works. Anything else (e.g. EPERM/EOPNOTSUPP
      // from a restrictive seccomp/capability profile) means it doesn't.
      uid_t unusedUid = 0;
      Error probeError;
      DiagLookupResult result = queryOnce(fd,
                                         boost::asio::ip::address_v4::loopback(),
                                         boost::asio::ip::address_v4::loopback(),
                                         0,
                                         0,
                                         &unusedUid,
                                         &probeError);

      ::close(fd);

      if (result == DiagLookupResult::Found || result == DiagLookupResult::NotFound)
         return true;

      if (probeError.getCode() == boost::system::errc::timed_out)
      {
         // A timeout here reflects a transient hiccup (kernel/scheduling
         // delay), not a capability/permission problem -- unlike an
         // EPERM/EOPNOTSUPP rejection, it says nothing about whether future
         // queries will succeed. Since this result is cached for the process
         // lifetime, treat it as inconclusive rather than permanently
         // disabling enforcement over what should be a one-off: assume the
         // capability is available and let per-request timeouts (which do
         // fail closed) handle any recurrence.
         LOG_WARNING_MESSAGE(
            "NETLINK_SOCK_DIAG probe timed out; assuming the capability is available "
            "(rstudio-pro#11470). If per-request queries error or time out, localhost "
            "proxy requests will fail.");
         return true;
      }

      LOG_WARNING_MESSAGE(
         "NETLINK_SOCK_DIAG query rejected; port-proxy ownership enforcement is "
         "DISABLED for this process (rstudio-pro#11470).");
      return false;
   }();

   return available;
}

#else // !__linux__

Error lookupEstablishedSocketUid(const boost::asio::ip::address&,
                                 const boost::asio::ip::address&,
                                 int,
                                 int,
                                 uid_t*)
{
   return systemError(boost::system::errc::not_supported,
                      "NETLINK_SOCK_DIAG socket ownership lookup is only supported on Linux",
                      ERROR_LOCATION);
}

Error verifyPeerUid(const boost::asio::ip::address& localAddress,
                    const boost::asio::ip::address& remoteAddress,
                    int appPort,
                    int ephemeralPort,
                    uid_t /*expectedUid*/)
{
   uid_t ownerUid = 0;
   Error error = lookupEstablishedSocketUid(localAddress, remoteAddress, appPort, ephemeralPort, &ownerUid);
   error.addProperty(kPortOwnershipRejectedProperty, "1");
   return error;
}

bool probeSockDiagAvailable()
{
   return false;
}

#endif // __linux__

} // namespace socket_utils
} // namespace server_core
} // namespace rstudio
