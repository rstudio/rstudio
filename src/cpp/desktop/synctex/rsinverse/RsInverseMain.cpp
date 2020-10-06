/*
 * RsInverseMain.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

#include <windows.h>

#include <string>

#include <core/Log.hpp>
#include <shared_core/Error.hpp>
#include <core/StringUtils.hpp>
#include <shared_core/SafeConvert.hpp>
#include <core/ProgramStatus.hpp>
#include <core/ProgramOptions.hpp>
#include <core/system/System.hpp>
#include <core/system/Environment.hpp>

#include <core/http/Util.hpp>
#include <core/http/Request.hpp>
#include <core/http/Response.hpp>
#include <core/http/NamedPipeBlockingClient.hpp>

// NOTE: this is a cut and paste job from SessionConstants.hpp so these
// should not be changed unless also changed there
#define kLocalUriLocationPrefix           "/rsession-local/"
#define kPostbackUriScope                 "postback/"
#define kPostbackExitCodeHeader           "X-Postback-ExitCode"

using namespace rstudio;
using namespace rstudio::core;

int main(int argc, char** argv)
{
   try
   {
      // initialize log
      core::log::setProgramId("rsinverse");
      core::system::initializeSystemLog("rsinverse", log::LogLevel::WARN);

      // ignore SIGPIPE
      Error error = core::system::ignoreSignal(core::system::SigPipe);
      if (error)
         LOG_ERROR(error);

      // read options
      using namespace boost::program_options;
      options_description rsinverseOptions("rsinverse");
      unsigned int windowHandle;
      std::string port, sharedSecret, sourceFile;
      int line;
      rsinverseOptions.add_options()
         ("hwnd",
            value<unsigned int>(&windowHandle),
            "hwnd of rstudio instance")
         ("port",
            value<std::string>(&port),
            "port of rstudio instance")
         ("secret",
            value<std::string>(&sharedSecret),
            "rstudio shared secret")
         ("source-file",
            value<std::string>(&sourceFile),
            "source file to navigate to")
         ("line",
            value<int>(&line),
            "line of code to navigate to");

      // define program options (allow positional specification)
      core::program_options::OptionsDescription optDesc("rsinverse");
      optDesc.commandLine.add(rsinverseOptions);
      optDesc.positionalOptions.add("hwnd", 1);
      optDesc.positionalOptions.add("port", 1);
      optDesc.positionalOptions.add("secret", 1);
      optDesc.positionalOptions.add("source-file", 1);
      optDesc.positionalOptions.add("line", 1);

      // read options
      ProgramStatus status = core::program_options::read(optDesc, argc, argv);
      if (status.exit())
         return status.exitCode();

      // activate the window
      HWND hRStudioWnd = reinterpret_cast<HWND>(windowHandle);
      if (::IsWindow(hRStudioWnd))
      {
         HWND hwndPopup = ::GetLastActivePopup(hRStudioWnd);
         if (::IsWindow(hwndPopup))
            hRStudioWnd = hwndPopup;
         ::SetForegroundWindow(hRStudioWnd);
         if (::IsIconic(hRStudioWnd))
            ::ShowWindow(hRStudioWnd, SW_RESTORE);
      }

      // we presume that the path is passed to us in the system encoding
      sourceFile = string_utils::systemToUtf8(sourceFile);

      // enocde the source file and line as a query string
      std::string requestBody;
      core::http::Fields args;
      args.push_back(std::make_pair("source-file", sourceFile));
      args.push_back(std::make_pair("line",
                                     safe_convert::numberToString(line)));
      http::util::buildQueryString(args, &requestBody);


      // determine postback uri
      std::string uri = std::string(kLocalUriLocationPrefix kPostbackUriScope) +
                       "rsinverse";

      // build postback request
      http::Request request;
      request.setMethod("POST");
      request.setUri(uri);
      request.setHeader("Accept", "*/*");
      request.setHeader("X-Shared-Secret", sharedSecret);
      request.setHeader("Connection", "close");
      request.setBody(requestBody);

      // send it
      http::Response response;
      std::string pipeName = core::system::getenv("RS_LOCAL_PEER");
      error = http::sendRequest(pipeName,
                                request,
                                http::ConnectionRetryProfile(
                                     boost::posix_time::seconds(10),
                                     boost::posix_time::milliseconds(50)),
                                &response);

      std::string exitCode = response.headerValue(kPostbackExitCodeHeader);
      return safe_convert::stringTo<int>(exitCode, EXIT_FAILURE);
   }
   CATCH_UNEXPECTED_EXCEPTION

   // if we got this far we had an unexpected exception
   return EXIT_FAILURE;
}

#ifdef _WIN32
int WINAPI WinMain(HINSTANCE hInstance,
                   HINSTANCE hPrevInstance,
                   LPSTR lpCmdLine,
                   int nShowCmd)
{
   return main(__argc, __argv);
}
#endif
