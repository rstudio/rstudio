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

#include <boost/asio/io_context.hpp>
#include <boost/asio/ip/address_v4.hpp>
#include <boost/asio/ip/address_v6.hpp>
#include <boost/asio/ip/tcp.hpp>

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

// Fixed sequence number stamped on each exact-match request; replies must
// echo it back (see readSingleDiagReply). Each query uses a fresh netlink
// socket for a single request/reply exchange, so a constant suffices.
constexpr uint32_t kNetlinkRequestSeq = 1;

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

// Sends a non-dump (exact-match) NETLINK_SOCK_DIAG request for the single TCP
// socket identified by (srcAddr, sport) <-> (dstAddr, dport). Without
// NLM_F_DUMP, the kernel performs a direct hash-table lookup for this 4-tuple
// rather than dumping and filtering every socket on the host -- this is the
// performance-critical difference from the old dump-based implementation,
// since this runs on every proxied request. states is an idiag_states bitmask
// (e.g. 1u << TCP_ESTABLISHED, 1u << TCP_LISTEN) describing the intended
// target; the kernel's exact-match lookup path does not itself filter by it
// (whatever socket matches the tuple is returned regardless of state), so
// queryOnce() re-enforces it client-side against the reply's actual
// idiag_state (see queryOnce()'s comment for why this matters).
Error sendExactMatchRequest(int fd,
                            const boost::asio::ip::address& srcAddr,
                            const boost::asio::ip::address& dstAddr,
                            uint16_t sport,
                            uint16_t dport,
                            uint32_t states)
{
   DiagRequest request;
   std::memset(&request, 0, sizeof(request));

   request.nlh.nlmsg_len = sizeof(request);
   request.nlh.nlmsg_type = SOCK_DIAG_BY_FAMILY;
   request.nlh.nlmsg_flags = NLM_F_REQUEST;
   request.nlh.nlmsg_seq = kNetlinkRequestSeq;
   request.nlh.nlmsg_pid = 0;

   request.req.sdiag_family = srcAddr.is_v4() ? AF_INET : AF_INET6;
   request.req.sdiag_protocol = IPPROTO_TCP;
   request.req.idiag_ext = 0;
   request.req.pad = 0;
   request.req.idiag_states = states;
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
   // A socket matched the 4-tuple, but not in one of the requested states
   // (#18439) -- see queryOnce()'s client-side state check below.
   // Treated the same as NotFound by callers: there is no matching socket in
   // the state they asked for, only a decoy.
   WrongState,
   Error
};

// Reads the single-message reply to an exact-match request (see
// sendExactMatchRequest): either one SOCK_DIAG_BY_FAMILY/inet_diag_msg record
// (Found, *pUid and *pState set), one NLMSG_ERROR with -ENOENT (NotFound -- no
// socket matches this exact 4-tuple, a normal outcome, not a transport
// failure), or any other NLMSG_ERROR / recvmsg() failure (Error, *pError set).
// Unlike the old dump reply, there is no NLMSG_DONE framing to loop for -- a
// single recvmsg() always suffices.
DiagLookupResult readSingleDiagReply(int fd, uid_t* pUid, uint8_t* pState, Error* pError)
{
   std::vector<char> buffer(kNetlinkRecvBufferSize);

   iovec iov;
   iov.iov_base = buffer.data();
   iov.iov_len = buffer.size();

   sockaddr_nl sender;
   std::memset(&sender, 0, sizeof(sender));

   msghdr msg;
   std::memset(&msg, 0, sizeof(msg));
   msg.msg_name = &sender;
   msg.msg_namelen = sizeof(sender);
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

   // Defense-in-depth for enforcement code: only trust replies sent by the
   // kernel (nl_pid == 0) that echo our request's sequence number. Unicasting
   // to another socket's netlink portid already requires CAP_NET_ADMIN, so a
   // spoofed reply shouldn't be possible in practice, but the check is cheap.
   if (msg.msg_namelen != sizeof(sender) ||
       sender.nl_family != AF_NETLINK ||
       sender.nl_pid != 0)
   {
      *pError = systemError(EIO, "NETLINK_SOCK_DIAG reply from unexpected sender", ERROR_LOCATION);
      return DiagLookupResult::Error;
   }

   auto* nlh = reinterpret_cast<nlmsghdr*>(buffer.data());
   auto remaining = static_cast<size_t>(received);

   if (!NLMSG_OK(nlh, remaining))
   {
      *pError = systemError(EIO, "malformed NETLINK_SOCK_DIAG reply", ERROR_LOCATION);
      return DiagLookupResult::Error;
   }

   if (nlh->nlmsg_seq != kNetlinkRequestSeq)
   {
      *pError = systemError(EIO, "NETLINK_SOCK_DIAG reply sequence mismatch", ERROR_LOCATION);
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
      *pUid = diag->idiag_uid;     // always populated by the kernel, independent of idiag_ext
      *pState = diag->idiag_state; // the actual state of whatever socket matched the tuple
      return DiagLookupResult::Found;
   }

   *pError = systemError(EIO, "unexpected NETLINK_SOCK_DIAG reply message type", ERROR_LOCATION);
   return DiagLookupResult::Error;
}

// Issues one exact-match query/reply round trip on fd and reports the result.
// The kernel's exact-match lookup (tcp_diag_find_one_icsk() -> inet_lookup())
// returns whatever socket matches the 4-tuple regardless of state -- it does
// not filter by the request's idiag_states, and the ehash chain it walks can
// even transiently hold two entries for the same tuple (e.g. a stale
// TIME_WAIT record not yet unlinked alongside a freshly-inserted ESTABLISHED
// one: see __inet_check_established()'s insert-before-unlink ordering under
// the bucket lock, which a lockless RCU reader like this query can observe
// mid-transition) -- so which one comes back is not guaranteed to be the one
// we asked for (#18439). Enforce the states filter ourselves here
// against the reply's actual idiag_state: a match outside the requested
// states is reported as WrongState rather than Found, so callers don't trust
// a decoy's uid (a TIME_WAIT/NEW_SYN_RECV socket's idiag_uid is always 0;
// this generalizes beyond that to also cover a stray hit landing on some
// other unrelated state).
DiagLookupResult queryOnce(int fd,
                          const boost::asio::ip::address& srcAddr,
                          const boost::asio::ip::address& dstAddr,
                          uint16_t sport,
                          uint16_t dport,
                          uint32_t states,
                          uid_t* pUid,
                          Error* pError)
{
   Error sendError = sendExactMatchRequest(fd, srcAddr, dstAddr, sport, dport, states);
   if (sendError)
   {
      *pError = sendError;
      return DiagLookupResult::Error;
   }

   uint8_t state = 0;
   DiagLookupResult result = readSingleDiagReply(fd, pUid, &state, pError);
   if (result == DiagLookupResult::Found && ((1u << state) & states) == 0)
      return DiagLookupResult::WrongState;

   return result;
}

// Wildcard address of the same family as addr (0.0.0.0 or ::), for querying a
// listening socket that has not been bound to any specific dest.
boost::asio::ip::address wildcardOfSameFamily(const boost::asio::ip::address& addr)
{
   if (addr.is_v4())
      return boost::asio::ip::address_v4::any();
   return boost::asio::ip::address_v6::any();
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
      WLOGF("Failed to set NETLINK_SOCK_DIAG receive timeout ({}); proceeding "
            "without a receive timeout for this query (rstudio-pro#11470).",
            timeoutError.getSummary());
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
                                      (1u << TCP_ESTABLISHED),
                                      pUid,
                                      &error);

   ::close(fd);

   if (result == DiagLookupResult::Error)
      return error;

   if (result == DiagLookupResult::NotFound || result == DiagLookupResult::WrongState)
   {
      // WrongState means the tuple matched something (#18439's TIME_WAIT/
      // NEW_SYN_RECV decoy, or a transient double-entry race -- see
      // queryOnce()) that isn't actually the ESTABLISHED socket we asked for;
      // report it identically to NotFound so verifyPeerUid()'s fallback
      // treats both as "the established path couldn't confirm this."
      return systemError(boost::system::errc::no_such_file_or_directory,
                         "No established socket for requested 4-tuple",
                         ERROR_LOCATION);
   }

   return Success();
}

Error lookupListeningSocketUid(const boost::asio::ip::address& listenAddress,
                               int listenPort,
                               uid_t* pUid)
{
   int fd = ::socket(AF_NETLINK, SOCK_RAW | SOCK_CLOEXEC, NETLINK_SOCK_DIAG);
   if (fd < 0)
      return systemError(errno, "socket(NETLINK_SOCK_DIAG)", ERROR_LOCATION);

   Error timeoutError = setRecvTimeout(fd);
   if (timeoutError)
   {
      // Non-fatal, matching lookupEstablishedSocketUid(): proceed without a
      // receive timeout rather than fail closed over an untaken code path.
      WLOGF("Failed to set NETLINK_SOCK_DIAG receive timeout ({}); proceeding "
            "without a receive timeout for this query.",
            timeoutError.getSummary());
   }

   // A listening socket is not connected to a peer, so its dest is wildcarded
   // (address any, port 0) -- this is how the kernel hashes/matches listeners.
   Error error;
   DiagLookupResult result = queryOnce(fd,
                                      listenAddress,
                                      wildcardOfSameFamily(listenAddress),
                                      static_cast<uint16_t>(listenPort),
                                      0,
                                      (1u << TCP_LISTEN),
                                      pUid,
                                      &error);

   // A dual-stack (IPV6_V6ONLY=0) listener bound to "::" is registered under
   // AF_INET6, so a plain AF_INET exact-match query (the case when we dialed
   // it via 127.0.0.1) can come back NotFound even though the listener
   // exists -- unlike the established-socket path, where inet_match() is
   // documented not to check sk_family, listener lookup does dispatch by
   // sdiag_family. Retry once with the AF_INET6 wildcard address/port to
   // catch this case (mirrors the dual-stack coverage in
   // LooksUpUidOfEstablishedDualStackLoopbackSocket for the established path).
   if ((result == DiagLookupResult::NotFound || result == DiagLookupResult::WrongState) && listenAddress.is_v4())
   {
      Error retryError;
      DiagLookupResult retryResult = queryOnce(fd,
                                              boost::asio::ip::address_v6::any(),
                                              boost::asio::ip::address_v6::any(),
                                              static_cast<uint16_t>(listenPort),
                                              0,
                                              (1u << TCP_LISTEN),
                                              pUid,
                                              &retryError);
      if (retryResult != DiagLookupResult::Error)
      {
         result = retryResult;
         error = retryError;
      }
   }

   ::close(fd);

   if (result == DiagLookupResult::Error)
      return error;

   if (result == DiagLookupResult::NotFound || result == DiagLookupResult::WrongState)
   {
      return systemError(boost::system::errc::no_such_file_or_directory,
                         "No listening socket for requested address/port",
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
   // remoteAddress/appPort are, from our own connected socket's point of
   // view, the address and port we dialed -- i.e. exactly the listening
   // socket's own address and port, usable directly for a Listener-mode query.
   OwnershipCheckMode mode = probeOwnershipCheckMode();

   uid_t ownerUid = 0;
   Error error;

   if (mode == OwnershipCheckMode::Listener)
   {
      error = lookupListeningSocketUid(remoteAddress, appPort, &ownerUid);
   }
   else
   {
      error = lookupEstablishedSocketUid(localAddress, remoteAddress, appPort, ephemeralPort, &ownerUid);

      // lookupEstablishedSocketUid() reports "no confirmed ESTABLISHED match"
      // (errc::no_such_file_or_directory) both when nothing matched the tuple
      // at all and when something did but queryOnce() rejected it as
      // WrongState -- i.e. a TIME_WAIT/NEW_SYN_RECV decoy (idiag_uid always
      // 0), or -- on a kernel lacking c51da3f7a161 -- a not-yet-accept()'d
      // socket that IS truly established but misreports uid 0 (exactly the
      // race probeOwnershipCheckMode() should already have steered this
      // process away from via Listener mode, but a transient TIME_WAIT hit,
      // or even a genuine race where the ehash transiently holds both a stale
      // TIME_WAIT and the new ESTABLISHED entry for the same tuple -- see
      // queryOnce()'s comment -- is possible even in Established mode).
      //
      // A related, kernel-version-independent race lands here too: we're
      // called as soon as OUR side of the handshake completes (the connect()
      // completion handler, right after we send the final ACK), which does
      // not wait for the peer to receive and process that ACK. So the app's
      // TCP stack may not have finished its own hash-dance yet -- the only
      // thing that exists there could still be the half-open request socket
      // (NEW_SYN_RECV), or nothing at all -- even on a kernel where
      // established-socket uid reporting is otherwise fully reliable. Both
      // outcomes are WrongState/NotFound here, same as the cases above.
      //
      // Re-check the listener (unambiguous on every kernel, and unaffected by
      // handshake timing since it has existed since well before this
      // connection attempt) rather than failing closed on an inconclusive
      // established-path result.
      if (error && error.getCode() == boost::system::errc::no_such_file_or_directory)
         error = lookupListeningSocketUid(remoteAddress, appPort, &ownerUid);
   }

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

namespace {

// Deterministic repro of the pre-accept uid-0 race (#18439): binds
// an ephemeral loopback listener, connects a client to it, and deliberately
// never accepts the connection -- reproducing, without any timing dependency,
// the window in which a kernel lacking upstream commit c51da3f7a161 ("net:
// remove sock_i_uid()", first in v6.17) reports the accepted-side socket's
// owner as uid 0 instead of the listener's real owner. Returns Found with the
// (possibly wrong) uid the kernel currently reports for that socket, or Error
// if the query itself failed.
DiagLookupResult probeEstablishedUidReporting(uid_t* pUid, Error* pError)
{
   boost::system::error_code ec;
   boost::asio::io_context io;
   boost::asio::ip::tcp::acceptor acceptor(
      io, boost::asio::ip::tcp::endpoint(boost::asio::ip::address_v4::loopback(), 0));
   int listenPort = acceptor.local_endpoint().port();

   boost::asio::ip::tcp::socket client(io);
   client.connect(boost::asio::ip::tcp::endpoint(boost::asio::ip::address_v4::loopback(), listenPort), ec);
   if (ec)
   {
      *pError = systemError(ec, ERROR_LOCATION);
      return DiagLookupResult::Error;
   }

   // Deliberately do not accept() -- the client's socket is now connect()'d
   // and the server-side child socket is hashed and ESTABLISHED, but not yet
   // accepted, exactly like the race window this probe exists to detect.
   int fd = ::socket(AF_NETLINK, SOCK_RAW | SOCK_CLOEXEC, NETLINK_SOCK_DIAG);
   if (fd < 0)
   {
      *pError = systemError(errno, "socket(NETLINK_SOCK_DIAG)", ERROR_LOCATION);
      return DiagLookupResult::Error;
   }
   setRecvTimeout(fd); // best-effort; an unbounded probe query is acceptable at startup

   DiagLookupResult result = queryOnce(fd,
                                      client.remote_endpoint().address(),
                                      client.local_endpoint().address(),
                                      static_cast<uint16_t>(listenPort),
                                      client.local_endpoint().port(),
                                      (1u << TCP_ESTABLISHED),
                                      pUid,
                                      pError);
   ::close(fd);
   return result;
}

} // anonymous namespace

OwnershipCheckMode probeOwnershipCheckMode()
{
   // Computed once, cached for the process lifetime. A function-local static
   // initialized from a lambda gives the required thread-safe one-time init.
   static const OwnershipCheckMode mode = []() -> OwnershipCheckMode {
      int fd = ::socket(AF_NETLINK, SOCK_RAW | SOCK_CLOEXEC, NETLINK_SOCK_DIAG);
      if (fd < 0)
      {
         WLOGF("NETLINK_SOCK_DIAG unavailable ({}); port-proxy ownership "
               "enforcement is DISABLED for this process (rstudio-pro#11470). "
               "Cross-user /p/ and /p6/ isolation will not be enforced in this "
               "environment.",
               ::strerror(errno));
         return OwnershipCheckMode::Disabled;
      }

      Error timeoutError = setRecvTimeout(fd);
      if (timeoutError)
      {
         // Non-fatal, matching lookupEstablishedSocketUid(): proceed with the
         // probe (and thus with enforcement) even without a receive timeout.
         WLOGF("Failed to set NETLINK_SOCK_DIAG receive timeout ({}); proceeding "
               "without a receive timeout for this query (rstudio-pro#11470).",
               timeoutError.getSummary());
      }

      // Probe with a deliberately absent 4-tuple, exercising the same
      // exact-match code path production traffic uses. A Found, NotFound
      // (-ENOENT), or WrongState reply all mean the kernel accepted and
      // answered the query -- the mechanism works. Anything else (e.g.
      // EPERM/EOPNOTSUPP from a restrictive seccomp/capability profile) means
      // it doesn't.
      uid_t unusedUid = 0;
      Error probeError;
      DiagLookupResult result = queryOnce(fd,
                                         boost::asio::ip::address_v4::loopback(),
                                         boost::asio::ip::address_v4::loopback(),
                                         0,
                                         0,
                                         (1u << TCP_ESTABLISHED),
                                         &unusedUid,
                                         &probeError);

      ::close(fd);

      bool queryMechanismWorks = (result == DiagLookupResult::Found ||
                                  result == DiagLookupResult::NotFound ||
                                  result == DiagLookupResult::WrongState);
      bool probeTimedOut = false;

      if (!queryMechanismWorks)
      {
         if (probeError.getCode() == boost::system::errc::timed_out)
         {
            // A timeout here reflects a transient hiccup (kernel/scheduling
            // delay), not a capability/permission problem -- unlike an
            // EPERM/EOPNOTSUPP rejection, it says nothing about whether
            // future queries will succeed. Since this result is cached for
            // the process lifetime, don't permanently disable enforcement
            // over what should be a one-off. Don't assume Established is
            // safe either -- fall through to the shared listener-validation
            // logic below (do NOT return Listener directly here: it must
            // still be confirmed to actually work before we commit to it,
            // same as any other route to Listener mode -- otherwise a
            // persistently timing-out environment would lock enforcement
            // into a mode whose per-request queries also time out, instead
            // of correctly degrading to Disabled).
            LOG_WARNING_MESSAGE(
               "NETLINK_SOCK_DIAG probe timed out; attempting listener-based "
               "port-proxy ownership checks (rstudio-pro#11470). If per-request "
               "queries also time out, localhost proxy requests will fail.");
            probeTimedOut = true;
         }
         else
         {
            LOG_WARNING_MESSAGE(
               "NETLINK_SOCK_DIAG query rejected; port-proxy ownership enforcement is "
               "DISABLED for this process. Cross-user /p/ and /p6/ isolation will not "
               "be enforced in this environment (rstudio-pro#11470).");
            return OwnershipCheckMode::Disabled;
         }
      }

      // Now determine whether this kernel reports trustworthy uids for
      // not-yet-accepted ESTABLISHED sockets (fixed upstream by
      // c51da3f7a161, first in v6.17), using a deterministic repro rather
      // than production-traffic timing luck. Skipped entirely if the
      // capability probe above already timed out -- that query alone was
      // inconclusive about the mechanism's general health, so there's little
      // point spending another query attempt that may just time out too;
      // go straight to the listener-validation fallback below.
      //
      // Deliberately geteuid(), not getuid(): the kernel attributes a newly
      // created socket's ownership to the creating task's effective/fs uid
      // (sock_i_uid() reads SOCK_INODE(sk->sk_socket)->i_uid, populated from
      // current_fsuid() at socket() time), not its real uid. rserver itself
      // is exactly the case where these diverge -- its privilege-separated
      // main process runs with a real uid of 0 but an effective uid of the
      // dedicated service account -- so every probe socket created below is
      // actually owned by that effective uid. Comparing against getuid()
      // instead would make this self-test spuriously believe its own probe
      // sockets belong to someone else, permanently disabling enforcement
      // that Listener mode could otherwise provide (#18439).
      //
      // When this process's effective uid is 0, a buggy kernel's uid-0
      // answer is indistinguishable from a correct one (root-owned listeners
      // are a real, legitimate uid-0 case) -- so an rserver actually running
      // with effective uid 0 can never trust this probe to select
      // Established, and always falls back to Listener.
      uid_t probeSelfUid = ::geteuid();
      OwnershipCheckMode establishedOrListener = OwnershipCheckMode::Listener;

      if (!probeTimedOut)
      {
         uid_t establishedProbeUid = 0;
         Error establishedProbeError;
         DiagLookupResult establishedProbeResult =
            probeEstablishedUidReporting(&establishedProbeUid, &establishedProbeError);

         if (probeSelfUid != 0 &&
             establishedProbeResult == DiagLookupResult::Found &&
             establishedProbeUid == probeSelfUid)
         {
            establishedOrListener = OwnershipCheckMode::Established;
         }
      }

      // Whichever mode we're about to select (or fell back to above),
      // validate that a listener query actually works before committing to
      // Listener -- if it doesn't, there's no safe enforcing mode left.
      if (establishedOrListener == OwnershipCheckMode::Listener)
      {
         boost::asio::io_context io;
         boost::asio::ip::tcp::acceptor listenerProbe(
            io, boost::asio::ip::tcp::endpoint(boost::asio::ip::address_v4::loopback(), 0));
         uid_t listenerProbeUid = 0;
         Error listenerProbeError = lookupListeningSocketUid(
            boost::asio::ip::address_v4::loopback(), listenerProbe.local_endpoint().port(), &listenerProbeUid);

         if (listenerProbeError || listenerProbeUid != probeSelfUid)
         {
            LOG_WARNING_MESSAGE(
               "NETLINK_SOCK_DIAG established-socket uid reporting is unreliable on this "
               "kernel and the listener-socket fallback also failed to verify; port-proxy "
               "ownership enforcement is DISABLED for this process (rstudio-pro#11470).");
            return OwnershipCheckMode::Disabled;
         }

         LOG_WARNING_MESSAGE(
            "NETLINK_SOCK_DIAG established-socket uid reporting is unreliable on this "
            "kernel (or this process runs as root); using listener-socket ownership "
            "checks instead (#18439). See https://github.com/torvalds/linux/"
            "commit/c51da3f7a161 for the upstream kernel fix (first in v6.17).");
      }

      return establishedOrListener;
   }();

   return mode;
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

Error lookupListeningSocketUid(const boost::asio::ip::address&,
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

OwnershipCheckMode probeOwnershipCheckMode()
{
   return OwnershipCheckMode::Disabled;
}

#endif // __linux__

} // namespace socket_utils
} // namespace server_core
} // namespace rstudio
