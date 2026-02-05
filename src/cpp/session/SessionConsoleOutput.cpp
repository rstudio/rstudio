/*
 * SessionConsoleOutput.cpp
 *
 * Copyright (C) 2025 by Posit Software, PBC
 *
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

#include <boost/regex.hpp>

#include <fmt/format.h>

#include <core/AnsiEscapes.hpp>
#include <core/regex/RegexDebug.hpp>

#include <r/RExec.hpp>
#include <r/RRoutines.hpp>

#include <session/prefs/UserPrefs.hpp>
#include <session/SessionConsoleOutput.hpp>
#include <session/SessionModuleContext.hpp>


#define kNeverMatch "^(?!)$"


using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace console_output {

namespace {

#ifndef RSTUDIO_PACKAGE_BUILD
bool s_simulateLatency;
#endif

PendingOutputType s_pendingOutputType;

std::atomic<bool> s_isErrorAnnotationEnabled;
std::atomic<bool> s_isWarningAnnotationEnabled;
std::atomic<bool> s_isMessageAnnotationEnabled;

boost::regex s_reErrorPrefix(kNeverMatch);
boost::regex s_reWarningPrefix(kNeverMatch);
boost::regex s_reInAdditionPrefix(kNeverMatch);

void synchronize()
{
   std::string highlightConditionsPref = prefs::userPrefs().consoleHighlightConditions();
   if (highlightConditionsPref != kConsoleHighlightConditionsNone)
   {
      Error error;

      std::string reErrorPrefix;
      error = r::exec::RFunction(".rs.globalCallingHandlers.reErrorPrefix")
            .call(&reErrorPrefix);
      if (error)
         LOG_ERROR(error);

      std::string reWarningPrefix;
      error = r::exec::RFunction(".rs.globalCallingHandlers.reWarningPrefix")
            .call(&reWarningPrefix);
      if (error)
         LOG_ERROR(error);

      std::string reInAdditionPrefix;
      error = r::exec::RFunction(".rs.globalCallingHandlers.reInAdditionPrefix")
            .call(&reInAdditionPrefix);
      if (error)
         LOG_ERROR(error);

      s_isErrorAnnotationEnabled =
            highlightConditionsPref == kConsoleHighlightConditionsErrorsWarningsMessages ||
            highlightConditionsPref == kConsoleHighlightConditionsErrorsWarnings ||
            highlightConditionsPref == kConsoleHighlightConditionsErrors;

      s_isWarningAnnotationEnabled =
            highlightConditionsPref == kConsoleHighlightConditionsErrorsWarningsMessages ||
            highlightConditionsPref == kConsoleHighlightConditionsErrorsWarnings;

      s_isMessageAnnotationEnabled =
            highlightConditionsPref == kConsoleHighlightConditionsErrorsWarningsMessages;

      s_reErrorPrefix      = boost::regex(reErrorPrefix, boost::regex::icase);
      s_reWarningPrefix    = boost::regex(reWarningPrefix, boost::regex::icase);
      s_reInAdditionPrefix = boost::regex(reInAdditionPrefix, boost::regex::icase);
   }
}

void onBusy(bool busy)
{
   setPendingOutputType(PendingOutputTypeUnknown);
}

void onConsoleOutput(module_context::ConsoleOutputType type,
                     const std::string& output)
{
   if (s_pendingOutputType == PendingOutputTypeUnknown)
   {
      // When 'options(warn = 0)' is set, warning output will be printed
      // if any uncaught warnings were emitted during code execution.
      if (s_isWarningAnnotationEnabled)
      {
         if (boost::regex_search(output, s_reWarningPrefix))
         {
            setPendingOutputType(PendingOutputTypeWarning);
         }
      }
   }
   else if (s_pendingOutputType == PendingOutputTypeError)
   {
      // If we're currently processing error output, R may also
      // print warning output as part of the error.
      if (s_pendingOutputType == PendingOutputTypeError)
      {
         if (boost::regex_search(output, s_reInAdditionPrefix))
         {
            setPendingOutputType(PendingOutputTypeWarning);
         }
      }
   }
}

void onPreferencesSaved()
{
   synchronize();
}

SEXP rs_errorOutputPending()
{
   clientEventQueue().flush();
   setPendingOutputType(PendingOutputTypeError);
   return Rf_ScalarLogical(1);
}

} // end anonymous namespace

void simulateLatency()
{
#ifndef RSTUDIO_PACKAGE_BUILD
   if (s_simulateLatency)
   {
      boost::this_thread::sleep_for(boost::chrono::milliseconds(200));
   }
#endif
}

PendingOutputType getPendingOutputType()
{
   return s_pendingOutputType;
}

void setPendingOutputType(PendingOutputType type)
{
   s_pendingOutputType = type;
}

boost::regex reErrorPrefix()
{
   return s_reErrorPrefix;
}

boost::regex reWarningPrefix()
{
   return s_reWarningPrefix;
}

boost::regex reInAdditionPrefix()
{
   return s_reInAdditionPrefix;
}


bool isErrorAnnotationEnabled()
{
   return s_isErrorAnnotationEnabled;
}

bool isWarningAnnotationEnabled()
{
   return s_isWarningAnnotationEnabled;
}

bool isMessageAnnotationEnabled()
{
   return s_isMessageAnnotationEnabled;
}

namespace {

void writeImpl(OutputStream stream,
               const std::string& message,
               OutputType type,
               const char* suffix)
{
   // Check if annotation is enabled for this type
   bool annotate = false;
   const char* groupStart = nullptr;

   switch (type)
   {
   case OutputTypeError:
      annotate = isErrorAnnotationEnabled();
      groupStart = kAnsiEscapeGroupStartError;
      break;
   case OutputTypeWarning:
      annotate = isWarningAnnotationEnabled();
      groupStart = kAnsiEscapeGroupStartWarning;
      break;
   case OutputTypeMessage:
      annotate = isMessageAnnotationEnabled();
      groupStart = kAnsiEscapeGroupStartMessage;
      break;
   }

   // Build the output string and print to the appropriate stream
   if (annotate)
   {
      std::string output = fmt::format("{}{}{}{}", groupStart, message, kAnsiEscapeGroupEnd, suffix);

      if (stream == OutputStreamStdout)
         Rprintf("%s", output.c_str());
      else
         REprintf("%s", output.c_str());
   }
   else
   {
      if (stream == OutputStreamStdout)
         Rprintf("%s%s", message.c_str(), suffix);
      else
         REprintf("%s%s", message.c_str(), suffix);
   }

   // Reset pending output type so subsequent output isn't affected
   setPendingOutputType(PendingOutputTypeUnknown);
}

} // anonymous namespace

void write(OutputStream stream,
           const std::string& message,
           OutputType type)
{
   writeImpl(stream, message, type, "");
}

void writeLine(OutputStream stream,
               const std::string& message,
               OutputType type)
{
   writeImpl(stream, message, type, "\n");
}


Error initialize()
{
   using namespace module_context;

#ifndef RSTUDIO_PACKAGE_BUILD
   std::string simulateLatency = core::system::getenv("RS_SIMULATE_CONSOLE_LATENCY");
   s_simulateLatency = core::string_utils::isTruthy(simulateLatency);
#endif

   events().onBusy.connect(onBusy);
   events().onConsoleOutput.connect(onConsoleOutput);
   events().onPreferencesSaved.connect(onPreferencesSaved);

   RS_REGISTER_CALL_METHOD(rs_errorOutputPending);

   synchronize();

   return Success();
}

} // end namespace console_output
} // end namespace session
} // end namespace rstudio
