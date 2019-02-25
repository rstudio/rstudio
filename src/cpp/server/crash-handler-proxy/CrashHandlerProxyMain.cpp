/*
 * CrashHandlerProxyMain.cpp
 *
 * Copyright (C) 2019 by RStudio, Inc.
 *
 */

#include <core/CrashHandler.hpp>
#include <core/system/Environment.hpp>
#include <core/system/PosixSystem.hpp>

using namespace rstudio::core;
using namespace rstudio::core::system;

void runCrashHandler(const char* argv[])
{
   FilePath exePath;
   Error error = executablePath(nullptr, &exePath);
   if (error)
      LOG_ERROR(error);

   FilePath handlerPath;
   std::string crashpadHandlerPath = rstudio::core::system::getenv(kCrashpadHandlerEnvVar);
   if (!crashpadHandlerPath.empty())
      handlerPath = FilePath(crashpadHandlerPath);
   else
      handlerPath = exePath.parent().childPath("crashpad_handler");

   std::string handlerPathStr = handlerPath.absolutePath();
   const char* handlerExe = handlerPathStr.c_str();
   argv[0] = handlerExe;

   ::execvp(handlerExe, const_cast<char* const*>(argv));

   // if we get here, we failed to run the crash handler
   // log an error indicating why
   LOG_ERROR(systemError(errno, ERROR_LOCATION));
}

int main(int argc, const char* argv[])
{
   // note: we log all errors and attempt to launch the crashpad handler
   // regardless, as this is a best effort proxy attempt

   initializeStderrLog("crash-handler-proxy", kLogLevelWarning);

   Error error = ignoreSignal(SigPipe);
   if (error)
      LOG_ERROR(error);

   if (realUserIsRoot() && !effectiveUserIsRoot())
   {
      error = restoreRoot();
      if (error)
         LOG_ERROR(error);
   }

   runCrashHandler(argv);

   // if we get here, we failed to run the crash handler
   return EXIT_FAILURE;
}
