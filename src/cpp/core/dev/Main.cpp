/*
 * Main.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <iostream>

#include <boost/test/minimal.hpp>

#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/Log.hpp>
#include <core/system/System.hpp>

#include <core/http/NamedPipeAsyncClient.hpp>
#include <core/http/NamedPipeBlockingClient.hpp>
#include <core/http/NamedPipeAsyncServer.hpp>

using namespace core ;

int test_main(int argc, char * argv[])
{
   try
   { 
      // initialize log
      initializeSystemLog("coredev", core::system::kLogLevelWarning);

      boost::asio::io_service ioService;
      http::NamedPipeAsyncClient client(ioService, "MyPipe");

      // client.request().assign(myRequest);

      // client.execute();

      http::Request request;
      http::Response response;
      Error error = http::sendRequest("MyPipe", request, &response);
      if (error)
         LOG_ERROR(error);

      http::NamedPipeAsyncServer asyncServer("RStudio");

      return EXIT_SUCCESS;
   }
   CATCH_UNEXPECTED_EXCEPTION
   
   // if we got this far we had an unexpected exception
   return EXIT_FAILURE ;
}

