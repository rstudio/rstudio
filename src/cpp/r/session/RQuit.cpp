/*
 * RQuit.cpp
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

#define R_INTERNAL_FUNCTIONS

#include <boost/bind.hpp>
#include <gsl/gsl>

#include <r/RErrorCategory.hpp>
#include <r/RExec.hpp>

#include <r/session/RSession.hpp>

#include "REmbedded.hpp"
#include "RQuit.hpp"
#include "RStdCallbacks.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace r {
namespace session {

// forward declare win32 quit handler and provide string based quit
// handler that parses the quit command from the console
#ifdef _WIN32
bool win32Quit(const std::string& saveAction,
               int status,
               bool runLast,
               std::string* pErrMsg);

bool win32Quit(const std::string& command, std::string* pErrMsg)
{
   // default values
   std::string saveAction = "default";
   double status = 0;
   bool runLast = true;

   // parse quit arguments
   SEXP argsSEXP;
   r::sexp::Protect rProtect;
   Error error = r::exec::RFunction(".rs.parseQuitArguments", command).call(
                                                                     &argsSEXP,
                                                                     &rProtect);
   if (!error)
   {
      error = r::sexp::getNamedListElement(argsSEXP,
                                           "save",
                                           &saveAction,
                                           saveAction);
      if (error)
         LOG_ERROR(error);

      error = r::sexp::getNamedListElement(argsSEXP,
                                           "status",
                                           &status,
                                           status);
      if (error)
         LOG_ERROR(error);

      error = r::sexp::getNamedListElement(argsSEXP,
                                           "runLast",
                                           &runLast,
                                           runLast);
      if (error)
         LOG_ERROR(error);
   }
   else
   {
      *pErrMsg = r::endUserErrorMessage(error);
      return false;
   }

   return win32Quit(saveAction, gsl::narrow_cast<int>(status), runLast, pErrMsg);
}

#endif


void quit(bool saveWorkspace, int status)
{
   // invoke quit
   std::string save = saveWorkspace ? "yes" : "no";
 #ifdef _WIN32
   std::string quitErr;
   bool didQuit = win32Quit(save, 0, true, &quitErr);
   if (!didQuit)
   {
      REprintf("%s\n", quitErr.c_str());
      LOG_ERROR_MESSAGE(quitErr);
   }
 #else
   Error error = r::exec::RFunction("base:::q", save, status, true).call();
   if (error)
   {
      std::string message = r::endUserErrorMessage(error);
      REprintf("%s\n", message.c_str());
      LOG_ERROR(error);
   }
 #endif
}
   
// Replace the quit function so we can call our R_CleanUp hook. Note
// that we need to take special measures to code this safely visa-vi
// Rf_error long jmps and C++ exceptions. Currently, the RCleanUp
// function can still long jump if runLast encounters an error. We
// should re-write the combination of this function and RCleanUp to
// be fully "error-safe" (not doing this now due to regression risk)
#ifdef _WIN32
bool win32Quit(const std::string& saveAction,
               int status,
               bool runLast,
               std::string* pErrMsg)
{
   if (r::session::browserContextActive())
   {
      *pErrMsg = "unable to quit when browser is active";
      return false;
   }

   // determine save action
   SA_TYPE action = SA_DEFAULT;
   if (saveAction == "ask")
      action = SA_SAVEASK;
   else if (saveAction == "no")
      action = SA_NOSAVE;
   else if (saveAction == "yes")
      action = SA_SAVE;
   else if (saveAction == "default")
      action = SA_DEFAULT;
   else
   {
      *pErrMsg = "Unknown save action: " + saveAction;
      return false;
   }

   // clean up
   Error error = r::exec::executeSafely(
                  boost::bind(&RCleanUp, action, status, runLast));
   if (error)
   {
      *pErrMsg = r::endUserErrorMessage(error);
      return false;
   }

   // failsafe in case we don't actually quit as a result of cleanup
   ::exit(0);

   // keep compiler happy
   return true;
}
#endif


} // namespace session
} // namespace r
} // namespace rstudio
