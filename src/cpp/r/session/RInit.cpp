/*
 * RInit.cpp
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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

using namespace rstudio::core;

namespace rstudio {
namespace r {
namespace session {

namespace {

// is this R 3.0 or greator
bool s_isR3 = false;

// is this R 3.3 or greator
bool s_isR3_3 = false;

}

// one-time per session initialization
Error initialize()
{
   // ensure that the utils package is loaded (it might not be loaded
   // if R is attempting to recover from a library loading error which
   // occurs during .Rprofile)
   Error libError = r::exec::RFunction("library", "utils").call();
   if (libError)
      LOG_ERROR(libError);

   // check whether this is R 3.3 or greater
   Error r33Error = r::exec::evaluateString("getRversion() >= '3.3.0'", &s_isR3_3);
   if (r33Error)
      LOG_ERROR(r33Error);

   if (s_isR3_3)
   {
      s_isR3 = true;
   }
   else
   {
      // check whether this is R 3.0 or greater
      Error r3Error = r::exec::evaluateString("getRversion() >= '3.0.0'", &s_isR3);
      if (r3Error)
         LOG_ERROR(r3Error);
   }

   // initialize console history capacity
   r::session::consoleHistory().setCapacityFromRHistsize();

   // install R tools
   FilePath toolsFilePath = s_options.rSourcePath.complete("Tools.R");
   Error error = r::sourceManager().sourceTools(toolsFilePath);
   if (error)
      return error ;

   // install RStudio API
   FilePath apiFilePath = s_options.rSourcePath.complete("Api.R");
   error = r::sourceManager().sourceTools(apiFilePath);
   if (error)
      return error;

   // initialize graphics device -- use a stable directory for server mode
   // and temp directory for desktop mode (so that we can support multiple
   // concurrent processes using the same project)
   FilePath graphicsPath;
   if (s_options.serverMode)
   {
      std::string path = kGraphicsPath;
      if (utils::isR3())
         path += "-r3";
      graphicsPath = s_options.sessionScratchPath.complete(path);
   }
   else
   {
      graphicsPath = r::session::utils::tempDir().complete(
                              "rs-graphics-" + core::system::generateUuid());
   }

   error = graphics::device::initialize(graphicsPath,
                                        s_callbacks.locator);
   if (error) 
      return error;
   
   // restore client state
   session::clientState().restore(s_clientStatePath,
                                  s_projectClientStatePath);
      
   // restore suspended session if we have one
   bool wasResumed = false;
   
   // first check for a pending restart
   if (restartContext().hasSessionState())
   {
      // restore session
      std::string errorMessages ;
      restoreSession(restartContext().sessionStatePath(), &errorMessages);

      // show any error messages
      if (!errorMessages.empty())
         REprintf(errorMessages.c_str());

      // note we were resumed
      wasResumed = true;
   }
   else if (s_suspendedSessionPath.exists())
   {  
      // restore session
      std::string errorMessages ;
      restoreSession(s_suspendedSessionPath, &errorMessages);
      
      // show any error messages
      if (!errorMessages.empty())
         REprintf(errorMessages.c_str());

      // note we were resumed
      wasResumed = true;
   }  
   // new session
   else
   {  
      // restore console history
      FilePath historyPath = rHistoryFilePath();
      error = consoleHistory().loadFromFile(historyPath, false);
      if (error)
         reportHistoryAccessError("read history from", historyPath, error);

      // defer loading of global environment
      s_deferredDeserializationAction = deferredRestoreNewSession;
   }
   
   // initialize client
   RInitInfo rInitInfo(wasResumed);
   error = s_callbacks.init(rInitInfo);
   if (error)
      return error;

   // call resume hook if we were resumed
   if (wasResumed)
      s_callbacks.resumed();
   
   // now that all initialization code has had a chance to run we 
   // can register all external routines which were added to r::routines
   // during the init sequence
   r::routines::registerAll();
   
   // set default repository if requested
   if (!s_options.rCRANRepos.empty())
   {
      error = r::exec::RFunction(".rs.setCRANReposAtStartup",
                                 s_options.rCRANRepos).call();
      if (error)
         return error;
   }

   // initialize profile resources
   error = r::exec::RFunction(".rs.profileResources").call();
   if (error)
      return error;

   // complete embedded r initialization
   error = r::session::completeEmbeddedRInitialization(s_options.useInternet2);
   if (error)
      return error;

   // set global R options
   FilePath optionsFilePath = s_options.rSourcePath.complete("Options.R");
   error = r::sourceManager().sourceLocal(optionsFilePath);
   if (error)
      return error;

   // server specific R options options
   if (s_options.serverMode)
   {
#ifndef __APPLE__
      FilePath serverOptionsFilePath =  s_options.rSourcePath.complete(
                                                         "ServerOptions.R");
      return r::sourceManager().sourceLocal(serverOptionsFilePath);
#else
      return Success();
#endif
   }
   else
   {
      return Success();
   }
}

namespace utils {

bool isR3()
{
   return s_isR3;
}

bool isR3_3()
{
   return s_isR3_3;
}

}

} // namespace session
} // namespace r
} // namespace rstudio

