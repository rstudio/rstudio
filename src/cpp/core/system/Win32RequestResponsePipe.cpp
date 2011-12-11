/*
 * Win32RequestResponsePipe.cpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
#include <core/system/Win32RequestResponsePipe.hpp>

#include <core/system/Environment.hpp>
#include <core/SafeConvert.hpp>

namespace core {
namespace system {

namespace {

const int MAX_PACKET_SIZE = 1000000;
const std::string ENV_REQ_READ("RS_RRPIPE_REQ_READ");
const std::string ENV_REQ_WRITE("RS_RRPIPE_REQ_WRITE");
const std::string ENV_RESP_READ("RS_RRPIPE_RESP_READ");
const std::string ENV_RESP_WRITE("RS_RRPIPE_RESP_WRITE");
const std::string ENV_EVENT("RS_RRPIPE_EVENT");

void closeHandle(HANDLE* pHandle)
{
   if (*pHandle != INVALID_HANDLE_VALUE)
   {
      ::CloseHandle(*pHandle);
      *pHandle = INVALID_HANDLE_VALUE;
   }
}

bool writeToPipe(HANDLE hPipeWrite,
                 const char* pBuffer,
                 uint32_t bytes)
{
   DWORD bytesToWrite = bytes;
   while (bytesToWrite > 0)
   {
      DWORD bytesWritten;
      if (!::WriteFile(hPipeWrite,
                       pBuffer,
                       bytesToWrite,
                       &bytesWritten,
                       NULL))
      {
         return false;
      }
      bytesToWrite -= bytesWritten;
      pBuffer += bytesWritten;
   }
   return true;
}

bool readFromPipe(HANDLE hPipeRead,
                  std::vector<char>* pBuffer,
                  uint32_t bytes)
{
   pBuffer->resize(bytes);
   char* pBuf = &((*pBuffer)[0]);

   DWORD bytesToRead = bytes;
   while (bytesToRead > 0)
   {
      DWORD bytesRead;
      if (!::ReadFile(hPipeRead,
                      pBuf,
                      bytesToRead,
                      &bytesRead,
                      NULL))
      {
         return false;
      }
      bytesToRead -= bytesRead;
      pBuffer += bytesRead;
   }
   return true;
}

bool writePacket(HANDLE hPipeWrite,
                 const char* pBuffer,
                 uint32_t bytes)
{
   return writeToPipe(hPipeWrite,
                      reinterpret_cast<char*>(&bytes),
                      sizeof(bytes))
         && writeToPipe(hPipeWrite, pBuffer, bytes);
}

bool readPacket(HANDLE hPipeRead,
                std::vector<char>* pBuffer)
{
   std::vector<char> numBuff;
   numBuff.resize(sizeof(uint32_t));
   if (!readFromPipe(hPipeRead, &numBuff, sizeof(uint32_t)))
      return false;

   uint32_t packetSize = *(reinterpret_cast<uint32_t*>(&(numBuff[0])));
   if (packetSize > MAX_PACKET_SIZE)
   {
      ::SetLastError(ERROR_BUFFER_OVERFLOW);
      return false;
   }

   pBuffer->resize(packetSize);
   return readFromPipe(hPipeRead, pBuffer, packetSize);
}

void setEnvHandle(const std::string& name,
                  HANDLE handle)
{
   setenv(name,
          boost::lexical_cast<std::string>(handle));
}

HANDLE getEnvHandle(const std::string& name)
{
   std::string value = getenv(name);
   if (value.empty())
      return INVALID_HANDLE_VALUE;
   return safe_convert::stringTo<HANDLE>(value, INVALID_HANDLE_VALUE);
}

} // namespace

Win32RequestResponsePipe::Win32RequestResponsePipe()
   : hRequestReadPipe_(INVALID_HANDLE_VALUE),
     hRequestWritePipe_(INVALID_HANDLE_VALUE),
     hResponseReadPipe_(INVALID_HANDLE_VALUE),
     hResponseWritePipe_(INVALID_HANDLE_VALUE),
     hManualResetEvent_(INVALID_HANDLE_VALUE),
     isChild_(false)
{
}

Win32RequestResponsePipe::~Win32RequestResponsePipe()
{
   closeHandle(&hRequestReadPipe_);
   closeHandle(&hRequestWritePipe_);
   closeHandle(&hResponseReadPipe_);
   closeHandle(&hResponseWritePipe_);
   closeHandle(&hManualResetEvent_);
}

Error Win32RequestResponsePipe::childInit()
{
   hRequestReadPipe_ = getEnvHandle(ENV_REQ_READ);
   hRequestWritePipe_ = getEnvHandle(ENV_REQ_WRITE);
   hResponseReadPipe_ = getEnvHandle(ENV_RESP_READ);
   hResponseWritePipe_ = getEnvHandle(ENV_RESP_WRITE);
   hManualResetEvent_ = getEnvHandle(ENV_EVENT);
   isChild_ = true;

   if (hRequestReadPipe_ == INVALID_HANDLE_VALUE ||
       hRequestWritePipe_ == INVALID_HANDLE_VALUE ||
       hResponseReadPipe_ == INVALID_HANDLE_VALUE ||
       hResponseWritePipe_ == INVALID_HANDLE_VALUE ||
       hManualResetEvent_ == INVALID_HANDLE_VALUE)
   {
      return systemError(ERROR_INVALID_HANDLE, ERROR_LOCATION);
   }

   closeHandle(&hRequestWritePipe_);
   closeHandle(&hResponseReadPipe_);

   return Success();
}

Error Win32RequestResponsePipe::parentInit()
{
   SECURITY_ATTRIBUTES sa = { sizeof(SECURITY_ATTRIBUTES) };
   sa.bInheritHandle = true;

   if (!::CreatePipe(&hRequestReadPipe_, &hRequestWritePipe_, &sa, 64))
   {
      return systemError(::GetLastError(), ERROR_LOCATION);
   }

   if (!::CreatePipe(&hResponseReadPipe_, &hResponseWritePipe_, &sa, 8192))
   {
      return systemError(::GetLastError(), ERROR_LOCATION);
   }

   hManualResetEvent_ = ::CreateEvent(&sa, true, false, NULL);
   if (hManualResetEvent_ == INVALID_HANDLE_VALUE)
   {
      return systemError(::GetLastError(), ERROR_LOCATION);
   }

   isChild_ = false;

   setEnvHandle(ENV_REQ_READ, hRequestReadPipe_);
   setEnvHandle(ENV_REQ_WRITE, hRequestWritePipe_);
   setEnvHandle(ENV_RESP_READ, hResponseReadPipe_);
   setEnvHandle(ENV_RESP_WRITE, hResponseWritePipe_);
   setEnvHandle(ENV_EVENT, hManualResetEvent_);

   return Success();
}

Error Win32RequestResponsePipe::onChildCreated()
{
   closeHandle(&hRequestReadPipe_);
   closeHandle(&hResponseWritePipe_);
}

Error Win32RequestResponsePipe::makeRequest(const std::string& request,
                                            std::vector<char>* pResponse)
{
   Error error = writeRequest(request);
   if (error)
      return error;
   return readRequest(pResponse);
}


Error Win32RequestResponsePipe::writeRequest(const std::string& data)
{
   if (!writePacket(hRequestWritePipe_, data.c_str(), data.size()))
      return systemError(::GetLastError(), ERROR_LOCATION);

   if (!::SetEvent(hManualResetEvent_))
      return systemError(::GetLastError(), ERROR_LOCATION);

   return Success();
}

Error Win32RequestResponsePipe::readRequest(std::vector<char>* pData)
{
   if (!::ResetEvent(hManualResetEvent_))
      return systemError(::GetLastError(), ERROR_LOCATION);
   if (!readPacket(hRequestReadPipe_, pData))
      return systemError(::GetLastError(), ERROR_LOCATION);
   return Success();
}

Error Win32RequestResponsePipe::writeResponse(const std::string& data)
{
   if (!writePacket(hResponseWritePipe_, data.c_str(), data.size()))
      return systemError(::GetLastError(), ERROR_LOCATION);
   return Success();
}

Error Win32RequestResponsePipe::readResponse(std::vector<char>* pData)
{
   if (!readPacket(hResponseReadPipe_, pData))
      return systemError(::GetLastError(), ERROR_LOCATION);
   return Success();
}

} // namespace system
} // namespace core
