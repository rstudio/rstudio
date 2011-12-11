/*
 * Win32RequestResponsePipe.hpp
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
#include <windows.h>

#include <boost/noncopyable.hpp>
#include <boost/shared_ptr.hpp>

#include <core/Error.hpp>

namespace core {
namespace system {

class Win32RequestResponsePipe : boost::noncopyable
{
public:
   Win32RequestResponsePipe();
   virtual ~Win32RequestResponsePipe();

   Error childInit();

   Error parentInit();
   Error onChildCreated();

   // Make request (to be called from parent process).
   // writeRequest and readResponse are private to make it harder
   // to inadvertently have concurrent requests being made at the
   // same time, which can create race conditions with the manual
   // reset event that could lead to hangs.
   Error makeRequest(const std::string& request,
                     std::vector<char>* pResponse);

   Error readRequest(std::vector<char>* pData);
   // Returns the handle to the manual reset event that gets signaled
   // when a request is written. The child can use this handle in
   // WaitForSingleObject calls (or similar) to know when readRequest
   // is likely to provide results.
   HANDLE requestEvent() { return hManualResetEvent_; }

   Error writeResponse(const std::string& data);

private:
   Error writeRequest(const std::string& data);
   Error readResponse(std::vector<char>* data);

   bool isChild_;
   HANDLE hRequestReadPipe_;
   HANDLE hRequestWritePipe_;
   HANDLE hResponseReadPipe_;
   HANDLE hResponseWritePipe_;
   HANDLE hManualResetEvent_;
};

} // namespace system
} // namespace core
