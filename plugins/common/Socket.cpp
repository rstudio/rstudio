/*
 * Copyright 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

#include "Platform.h"

#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <cerrno>

#include "Socket.h"

void Socket::init() {
#ifdef _WINDOWS
  // version 2.2 supported on Win95OSR2/WinNT4 and up
  WORD winsockVers = MAKEWORD(2, 2);
  WSADATA wsaData;
  int err = WSAStartup(winsockVers, &wsaData);
  if (err) {
    // TODO(jat): report error
    Debug::log(Debug::Error) << "WSAStartup(vers=2.2): err=" << err << Debug::flush;
  }
#endif
}

bool Socket::connect(const char* host, int port) {
  Debug::log(Debug::Debugging) << "Socket::connect(host=" << host << ",port=" << port << ")"
      << Debug::flush;
  if (isConnected()) {
    Debug::log(Debug::Error) << "Socket::connect - already connected" << Debug::flush;
    return false;
  }

  SOCKETTYPE fd = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
  if (fd < 0) {
    Debug::log(Debug::Error) << "Socket::connect - can't get socket" << Debug::flush;
    return false;
  }
#ifdef SO_NOSIGPIPE
  // On BSD, we need to suppress the SIGPIPE if the remote end disconnects.
  int option_value = 1;
  if (setsockopt(fd, SOL_SOCKET, SO_NOSIGPIPE, &option_value, sizeof(int))) {
    Debug::log(Debug::Error) << "Socket::connect - can't set NOSIGPIPE option" << Debug::flush;
    return false;
  }
#endif

  struct sockaddr_in sockAddr;
  memset(&sockAddr, 0, static_cast<int>(sizeof(sockAddr)));
  // check for numeric IP4 addresses first
  // TODO(jat): handle IPv6 addresses
  unsigned long numericAddr = inet_addr(host);
  if (numericAddr != 0xFFFFFFFF) {
    sockAddr.sin_addr.s_addr = numericAddr;
    sockAddr.sin_family = AF_INET;
  } else {
    struct hostent* hent = gethostbyname(host);
    if (!hent || !hent->h_addr_list[0]) {
      Debug::log(Debug::Error) << "Unable to get address for " << host << Debug::flush;
      return false;
    }
    memcpy(&(sockAddr.sin_addr), hent->h_addr_list[0], hent->h_length);
    sockAddr.sin_family = hent->h_addrtype;
  }
  sockAddr.sin_port = htons(port);

  if (::connect(fd, (struct sockaddr*) &sockAddr, sizeof(sockAddr)) < 0) {
#ifdef _WINDOWS
    char buf[256];
    DWORD dwLastError = ::GetLastError();
    strerror_s(buf, sizeof(buf), dwLastError);
    Debug::log(Debug::Error) << "Failed to connect to " << host << ":" << port << " -- error code "
        << dwLastError << Debug::flush;
    closesocket(fd);
    ::SetLastError(dwLastError);
#else
    Debug::log(Debug::Error) << "Can't connect to " << host << ":" << port << " -- "
        << strerror(errno) << Debug::flush;
    close(fd);
#endif
    return false;
  }
  sock = fd;
  connected = true;
  readBufPtr = readValid = readBuf;
  writeBufPtr = writeBuf;
#ifdef _WINDOWS
  Debug::log(Debug::Spam) << "  connected" << Debug::flush;
#else
  Debug::log(Debug::Spam) << "  connected, fd=" << fd << Debug::flush;
#endif
  return true;
}

bool Socket::disconnect(bool doFlush) {
  if (connected) {
    Debug::log(Debug::Debugging) << "Disconnecting socket" << Debug::flush;
    if (doFlush) {
      flush();
    }
    connected = false;
#ifdef _WINDOWS
    closesocket(sock);
#else
    shutdown(sock, SHUT_RDWR);
    close(sock);
#endif
  }
  return true;
}

bool Socket::emptyWriteBuf() {
  size_t len = writeBufPtr - writeBuf;
  Debug::log(Debug::Spam) << "Socket::emptyWriteBuf: len=" << len << Debug::flush;
  ++numWrites;
  totWriteBytes += len;
  if (len > maxWriteBytes) {
    maxWriteBytes = len;
  }
  for (char* ptr = writeBuf; len > 0; ) {
    ssize_t n = send(sock, ptr, len, 0);
    if (n <= 0) {
      if (errno == EPIPE) {
        Debug::log(Debug::Warning) << "Other end of socket disconnected" << Debug::flush;
        disconnect(false);
        return false;
      }
      Debug::log(Debug::Error) << "Error " << errno << " writing " << len << " bytes to socket"
          << Debug::flush;
      return false;
    }
    ptr += n;
    len -= n;
  }
  writeBufPtr = writeBuf;
  return true;
}

bool Socket::fillReadBuf() {
  readBufPtr = readBuf;
  errno = 0;
  ssize_t n = recv(sock, readBuf, BUF_SIZE, 0);
  if (n <= 0) {
    // EOF results in no error
    if (!errno || errno == EPIPE) {
      Debug::log(Debug::Warning) << "Other end of socket disconnected" << Debug::flush;
      disconnect(false);
      return false;
    }
    Debug::log(Debug::Error) << "Error " << errno << " reading " << BUF_SIZE << " bytes from socket"
        << Debug::flush;
    return false;
  }
  ++numReads;
  totReadBytes += n;
  if (static_cast<size_t>(n) > maxReadBytes) {
    maxReadBytes = n;
  }
  readValid = readBuf + n;
  return true;
}
