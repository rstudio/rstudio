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

#include <cstring>
#include <functional>
#include <string>
#include <vector>

#include <sys/socket.h>
#include <arpa/inet.h>
#include <netinet/tcp.h>
#include <linux/netlink.h>
#include <linux/sock_diag.h>
#include <linux/inet_diag.h>
#include <unistd.h>

#include <core/BoostErrors.hpp>
#include <core/Log.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace server_core {
namespace socket_utils {

namespace {

// Large enough to hold a batch of inet_diag_msg dump records. If a single
// recvmsg() call returns MSG_TRUNC (the kernel's batch exceeded this buffer),
// runDumpLoop() treats it as an explicit error rather than silently
// continuing with a partial batch.
constexpr size_t kNetlinkRecvBufferSize = 8192;

struct DiagDumpRequest
{
   nlmsghdr nlh;
   inet_diag_req_v2 req;
};

// Sends a NETLINK_SOCK_DIAG NLM_F_DUMP request for TCP sockets in the
// TCPF_ESTABLISHED state, for the given address family.
Error sendDumpRequest(int fd, bool ipv6)
{
   DiagDumpRequest request;
   std::memset(&request, 0, sizeof(request));

   request.nlh.nlmsg_len = sizeof(request);
   request.nlh.nlmsg_type = SOCK_DIAG_BY_FAMILY;
   request.nlh.nlmsg_flags = NLM_F_REQUEST | NLM_F_DUMP;
   request.nlh.nlmsg_seq = 1;
   request.nlh.nlmsg_pid = 0;

   request.req.sdiag_family = ipv6 ? AF_INET6 : AF_INET;
   request.req.sdiag_protocol = IPPROTO_TCP;
   request.req.idiag_ext = 0;
   request.req.pad = 0;
   request.req.idiag_states = (1u << TCP_ESTABLISHED);

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
      return systemError(errno, "sendto(NETLINK_SOCK_DIAG dump request)", ERROR_LOCATION);
   if (static_cast<size_t>(sent) != sizeof(request))
      return systemError(EIO, "short write sending NETLINK_SOCK_DIAG dump request", ERROR_LOCATION);

   return Success();
}

// Reads and parses the NETLINK_SOCK_DIAG dump reply, invoking onDiagMsg for each
// inet_diag_msg record encountered. Loops recvmsg() until NLMSG_DONE is seen (or
// onDiagMsg returns true to stop early). Returns an error for a genuine kernel
// NLMSG_ERROR (e.g. EPERM/EOPNOTSUPP from a restrictive profile), a recvmsg()
// failure, or a truncated dump message (MSG_TRUNC) -- the latter is treated as
// an explicit failure rather than silently truncating record parsing, since it
// would otherwise be indistinguishable from "socket not found".
Error runDumpLoop(int fd, const std::function<bool(const inet_diag_msg*)>& onDiagMsg)
{
   std::vector<char> buffer(kNetlinkRecvBufferSize);
   bool done = false;

   while (!done)
   {
      iovec iov;
      iov.iov_base = buffer.data();
      iov.iov_len = buffer.size();

      msghdr msg;
      std::memset(&msg, 0, sizeof(msg));
      msg.msg_iov = &iov;
      msg.msg_iovlen = 1;

      ssize_t received = ::recvmsg(fd, &msg, 0);
      if (received < 0)
      {
         if (errno == EINTR)
            continue;
         return systemError(errno, "recvmsg(NETLINK_SOCK_DIAG)", ERROR_LOCATION);
      }
      if (received == 0)
         return systemError(EIO, "NETLINK_SOCK_DIAG socket closed unexpectedly", ERROR_LOCATION);
      if (msg.msg_flags & MSG_TRUNC)
      {
         // The kernel's dump batch exceeded our buffer; the trailing record(s)
         // in this datagram were silently discarded by the kernel. Treat this as
         // an explicit error rather than letting NLMSG_OK/NLMSG_NEXT parsing stop
         // early and be indistinguishable from "socket not found" (fail closed).
         return systemError(EMSGSIZE,
                            "NETLINK_SOCK_DIAG dump message truncated (buffer too small)",
                            ERROR_LOCATION);
      }

      auto* nlh = reinterpret_cast<nlmsghdr*>(buffer.data());
      auto remaining = static_cast<size_t>(received);

      while (NLMSG_OK(nlh, remaining))
      {
         if (nlh->nlmsg_type == NLMSG_DONE)
         {
            done = true;
            break;
         }
         else if (nlh->nlmsg_type == NLMSG_ERROR)
         {
            auto* err = reinterpret_cast<nlmsgerr*>(NLMSG_DATA(nlh));
            done = true;
            if (err->error != 0)
            {
               return systemError(-err->error,
                                  "NETLINK_SOCK_DIAG dump rejected by kernel",
                                  ERROR_LOCATION);
            }
            break;
         }
         else if (nlh->nlmsg_type == SOCK_DIAG_BY_FAMILY)
         {
            auto* diag = reinterpret_cast<inet_diag_msg*>(NLMSG_DATA(nlh));
            if (onDiagMsg(diag))
            {
               done = true;
               break;
            }
         }

         nlh = NLMSG_NEXT(nlh, remaining);
      }
   }

   return Success();
}

// Returns true if the given inet_diag_sockid address field (idiag_src or
// idiag_dst, a __be32[4] in network byte order) represents a loopback address
// for the given address family. For IPv4 this is 127.0.0.0/8 (checked against
// the first 4 bytes, which hold the IPv4 address per inet_diag conventions);
// for IPv6 this is exactly ::1.
bool isLoopbackDiagAddr(bool ipv6, const __be32 addr[4])
{
   if (!ipv6)
   {
      return (ntohl(addr[0]) >> 24) == 127;
   }

   static const unsigned char kLoopbackV6[16] =
      {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1};
   return std::memcmp(addr, kLoopbackV6, sizeof(kLoopbackV6)) == 0;
}

} // anonymous namespace

Error lookupEstablishedSocketUid(bool ipv6, int appPort, int ephemeralPort, uid_t* pUid)
{
   int fd = ::socket(AF_NETLINK, SOCK_RAW | SOCK_CLOEXEC, NETLINK_SOCK_DIAG);
   if (fd < 0)
      return systemError(errno, "socket(NETLINK_SOCK_DIAG)", ERROR_LOCATION);

   Error error = sendDumpRequest(fd, ipv6);
   if (error)
   {
      ::close(fd);
      return error;
   }

   bool found = false;
   error = runDumpLoop(fd, [&](const inet_diag_msg* diag) -> bool {
      const inet_diag_sockid& id = diag->id;
      if (ntohs(id.idiag_sport) == static_cast<uint16_t>(appPort) &&
          ntohs(id.idiag_dport) == static_cast<uint16_t>(ephemeralPort) &&
          isLoopbackDiagAddr(ipv6, id.idiag_src) &&
          isLoopbackDiagAddr(ipv6, id.idiag_dst))
      {
         *pUid = diag->idiag_uid; // populated unconditionally (research Q8)
         found = true;
         return true; // stop the dump loop, we found our 4-tuple
      }
      return false;
   });

   ::close(fd);

   if (error)
      return error;

   if (!found)
   {
      return systemError(boost::system::errc::no_such_file_or_directory,
                         "No established socket for requested 4-tuple",
                         ERROR_LOCATION);
   }

   return Success();
}

Error verifyPeerUid(bool ipv6, int appPort, int ephemeralPort, uid_t expectedUid)
{
   uid_t ownerUid = 0;
   Error error = lookupEstablishedSocketUid(ipv6, appPort, ephemeralPort, &ownerUid);
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

      Error sendError = sendDumpRequest(fd, false /* AF_INET */);
      bool ok = false;
      if (!sendError)
      {
         // A single recv() is sufficient to distinguish "the kernel accepted and
         // answered the query" (NLMSG_DONE or an inet_diag_msg record) from a
         // rejection (NLMSG_ERROR with EPERM/EOPNOTSUPP/etc from a restrictive
         // seccomp/capability profile) or a transport failure.
         Error dumpError = runDumpLoop(fd, [](const inet_diag_msg*) { return true; });
         ok = !dumpError;
      }

      ::close(fd);

      if (!ok)
      {
         LOG_WARNING_MESSAGE(
            "NETLINK_SOCK_DIAG query rejected; port-proxy ownership enforcement is "
            "DISABLED for this process (rstudio-pro#11470).");
      }

      return ok;
   }();

   return available;
}

} // namespace socket_utils
} // namespace server_core
} // namespace rstudio
