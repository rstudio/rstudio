#ifndef _H_Socket
#define _H_Socket
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
#include "Debug.h"

#include <string>

#ifdef _WINDOWS
#include <winsock2.h>
#include <ws2tcpip.h>
#else
#include <netdb.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <sys/time.h>
#endif

/**
 * Encapsulates platform dependencies regarding buffered sockets.
 */
class Socket {
private:
  // Buffer size, chosen to fit in a single packet after TCP/IP overhead.
  static const int BUF_SIZE = 1400;
  
  // Can't rely on a sentinel value for the socket descriptor
  bool connected;
  
  SOCKETTYPE sock;
  
  // Read buffer
  char* readBuf;
  
  // One bye past end of valid data in readBuf
  char* readValid;
  
  // Current read pointer
  char* readBufPtr;

  // Write buffer
  char* writeBuf;
  
  // Current write pointer
  char* writeBufPtr;

  // Stats
  unsigned long numReads;
  unsigned long long totReadBytes;
  size_t maxReadBytes;

  unsigned long numWrites;
  unsigned long long totWriteBytes;
  size_t maxWriteBytes;

private:
  void init();
  bool fillReadBuf();
  bool emptyWriteBuf();

public:
  Socket() : connected(false), readBuf(new char[BUF_SIZE]), writeBuf(new char[BUF_SIZE]) {
    readBufPtr = readValid = readBuf;
    writeBufPtr = writeBuf;
    numReads = numWrites = 0;
    maxReadBytes = maxWriteBytes = 0;
    totReadBytes = totWriteBytes = 0;
    init();
  }
  
  ~Socket() {
    disconnect();
#ifdef _WINDOWS
    if (0) WSACleanup();
#endif
    // TODO(jat): LEAK LEAK LEAK
    // delete[] readBuf;
    // delete[] writeBuf;
    Debug::log(Debug::Debugging) << "Socket: #r=" << numReads << ", bytes/read="
        << (numReads ? totReadBytes / numReads : 0) << ", maxr=" << maxReadBytes << "; #w="
        << numWrites << ", bytes/write=" << (numWrites ? totWriteBytes / numWrites : 0) << ", maxw="
        << maxWriteBytes << Debug::flush;
  }

  /**
   * Connects this socket to a specified port on a host.
   * 
   * @param host host name or IP address to connect to
   * @param port TCP port to connect to
   * @return true if the connection succeeds
   */ 
  bool connect(const char* host, int port);
  
  /**
   * Returns true if the socket is connected.
   */
  bool isConnected() const {
    return connected;
  }
  
  /**
   * Disconnect this socket.
   * 
   * @param doFlush true (the default value) if the socket should be flushed.
   * @return true if disconnect succeeds
   */
  bool disconnect(bool doFlush = true);
  
  /**
   * Read a single byte from the socket.
   *
   * @return -1 on error, otherwise unsigned byte read.
   */
  int readByte() {
    if (readBufPtr >= readValid) {
      if (!fillReadBuf()) {
        return -1;
      }
    }
    return static_cast<unsigned char>(*readBufPtr++);
  }
  
  /**
   * Write a single byte to the socket.
   * 
   * @return true on success.
   */
  bool writeByte(char c) {
    if (writeBufPtr >= writeBuf + BUF_SIZE) {
      if (!emptyWriteBuf()) {
        return false;
      }
    }
    *writeBufPtr++ = c;
    return true;
  }
  
  /**
   * Flush any pending writes on the socket.
   *
   * @return true on success
   */
  bool flush() {
    if (writeBufPtr > writeBuf) {
      if (!emptyWriteBuf()) {
        return false;
      }
    }
    return true;
  }
};

#endif
