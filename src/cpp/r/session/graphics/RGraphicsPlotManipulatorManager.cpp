/*
 * RGraphicsPlotManipulatorManager.cpp
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

/* Manipulator TODO notes

Manipulate work:

- To get choices with names:

  choices <- unlist(list(...))
  names <- names(choices)

NOTE: this is recursive. if we want to allow lists as values
then we need to go non-recursive and have an optional explicit
"choices" parameter. alternatively we can be recursive and allow
developers to turn off with recursive = FALSE (don't like this)

- BIG ONE: do first? Picker values stored on the server! means they
don't need to be serializable as json (then can get rid of typecheck
requirement mentioned below)

- bug 1281: lose manipulator on resume
- picker named list (note above)
- picker bind to arbitrary values including lists:
   - NOTE: "value" is complicated by names -- must be by name?
   - watch unexpected types! (typecheck each value?)
   - can't do lists with recursive. need param for flatten/recurse
   - OR (simpler) could provide an explicit choices argument, then
     wouldn't recurse and therefore could preserve exact types


[DONE] bug 1284: manipulatorSetState, manipulatorGetState

[DONE] bug 1258: can't bind to variable inside list
   Note: not a bug, trying to use picker to return a boolean

*/

#include "RGraphicsPlotManipulatorManager.hpp"

#include <string>
#include <algorithm>

#include <boost/bind.hpp>
#include <boost/function.hpp>

#include <core/Log.hpp>
#include <core/Error.hpp>

#include <r/RExec.hpp>
#include <r/RErrorCategory.hpp>
#include <r/RRoutines.hpp>

#include "RGraphicsUtils.hpp"
#include "RGraphicsPlotManager.hpp"

using namespace core;

namespace r {
namespace session {  
namespace graphics {

namespace {

template <typename T>
void setManipulatorValue(SEXP manipulatorSEXP,
                         const std::string& name,
                         const T& value)
{
   r::exec::RFunction assignFunction("assign");
   assignFunction.addParam(name);
   assignFunction.addParam(value);
   assignFunction.addParam("envir", manipulatorSEXP);
   Error error = assignFunction.call();
   if (error)
      LOG_ERROR(error);
}

void setManipulatorJsonValue(SEXP manipulatorSEXP,
                             const std::pair<std::string,json::Value>& object)
{
   setManipulatorValue(manipulatorSEXP, object.first, object.second);
}

void safeExecuteManipulator(SEXP manipulatorSEXP)
{
   try
   {
      // execute the code within the manipulator.
      Error error = r::exec::RFunction("manipulate:::manipulatorExecute",
                                       manipulatorSEXP).call();

      // r code execution errors are expected (e.g. for invalid manipulate
      // code or incorrectly specified controls). if we get something that
      // isn't an r code execution error then report and log it
      if (error)
      {
         if (!r::isCodeExecutionError(error))
            logAndReportError(error, ERROR_LOCATION);
      }
   }
   CATCH_UNEXPECTED_EXCEPTION
}

SEXP rs_executeAndAttachManipulator(SEXP manipulatorSEXP)
{
   plotManipulatorManager().executeAndAttachManipulator(manipulatorSEXP);
   return R_NilValue;
}

SEXP rs_hasActiveManipulator()
{
   r::sexp::Protect rProtect;
   return r::sexp::create(plotManipulatorManager().hasActiveManipulator(),
                          &rProtect);
}

SEXP rs_activeManipulator()
{
   return plotManipulatorManager().activeManipulator();
}

SEXP rs_ensureManipulatorSaved()
{
   try
   {
      plotManipulatorManager().ensureManipulatorSaved();
   }
   CATCH_UNEXPECTED_EXCEPTION

   return R_NilValue;
}


} // anonymous namespace


PlotManipulatorManager& plotManipulatorManager()
{
   static PlotManipulatorManager instance;
   return instance;
}

PlotManipulatorManager::PlotManipulatorManager()
   :  pendingManipulatorSEXP_(R_NilValue),
      replayingManipulator_(false)
{
}
      

Error PlotManipulatorManager::initialize()
{
   // register R entry points for this class
   R_CallMethodDef execManipulatorMethodDef ;
   execManipulatorMethodDef.name = "rs_executeAndAttachManipulator" ;
   execManipulatorMethodDef.fun = (DL_FUNC) rs_executeAndAttachManipulator;
   execManipulatorMethodDef.numArgs = 1;
   r::routines::addCallMethod(execManipulatorMethodDef);

   // register has active manipulator routine
   R_CallMethodDef hasActiveManipulatorMethodDef ;
   hasActiveManipulatorMethodDef.name = "rs_hasActiveManipulator" ;
   hasActiveManipulatorMethodDef.fun = (DL_FUNC) rs_hasActiveManipulator;
   hasActiveManipulatorMethodDef.numArgs = 0;
   r::routines::addCallMethod(hasActiveManipulatorMethodDef);

   // register active manipulator routine
   R_CallMethodDef activeManipulatorMethodDef ;
   activeManipulatorMethodDef.name = "rs_activeManipulator" ;
   activeManipulatorMethodDef.fun = (DL_FUNC) rs_activeManipulator;
   activeManipulatorMethodDef.numArgs = 0;
   r::routines::addCallMethod(activeManipulatorMethodDef);

   // register ensure manipulator saved routine
   R_CallMethodDef ensureManipulatorSavedMethodDef ;
   ensureManipulatorSavedMethodDef.name = "rs_ensureManipulatorSaved" ;
   ensureManipulatorSavedMethodDef.fun = (DL_FUNC) rs_ensureManipulatorSaved;
   ensureManipulatorSavedMethodDef.numArgs = 0;
   r::routines::addCallMethod(ensureManipulatorSavedMethodDef);

   return Success();
}


boost::signal<void ()>& PlotManipulatorManager::onShowManipulator()
{
   return onShowManipulator_;
}


// execute a manipulator
void PlotManipulatorManager::executeAndAttachManipulator(SEXP manipulatorSEXP)
{
   // keep the pending manipulator set for the duration of this call.
   // this allows the plot manager to "collect" it on a new plot
   // but to ignore and let it die if the manipulator code block
   // doesn't create a plot
   pendingManipulatorSEXP_ = manipulatorSEXP;

   // execute it
   safeExecuteManipulator(pendingManipulatorSEXP_);

   // did the code create a new manipulator that is still active?
   bool showNewManipulator = (pendingManipulatorSEXP_ == R_NilValue) &&
                             hasActiveManipulator();

   // always clear the pending manipulator state so it is only attached
   // to plots which are generated by the code above
   pendingManipulatorSEXP_ = R_NilValue;

   // if the active plot has a manipulator after this call then
   // notify listeners that we need to show the manipulator
   if (showNewManipulator)
      onShowManipulator_();
}

bool PlotManipulatorManager::hasActiveManipulator() const
{
   return activeManipulator() != R_NilValue;
}

// the "active" manipultor is either the currently pending manipulator
// or if there is none pending then the the one associated with the
// currently active plot
SEXP PlotManipulatorManager::activeManipulator() const
{
   if (pendingManipulatorSEXP_ != R_NilValue)
   {
      return pendingManipulatorSEXP_;
   }
   else if (plotManager().hasPlot())
   {
      return plotManager().activePlot().manipulatorSEXP();
   }
   else
   {
      return R_NilValue;
   }
}

void PlotManipulatorManager::setPlotManipulatorValues(const json::Object& values)
{
   if (plotManager().hasPlot() &&
       plotManager().activePlot().hasManipulator())
   {
      // get the manipulator
      SEXP manipulatorSEXP = plotManager().activePlot().manipulatorSEXP();

      // set the underlying values
      std::for_each(values.begin(),
                    values.end(),
                    boost::bind(setManipulatorJsonValue, manipulatorSEXP, _1));

      // replay the manipulator
      replayingManipulator_ = true;
      safeExecuteManipulator(manipulatorSEXP);
      replayingManipulator_ = false;
   }
   else
   {
      LOG_WARNING_MESSAGE("called setPlotManipulatorValues but active plot "
                          "has no manipulator");
   }
}

void PlotManipulatorManager::ensureManipulatorSaved()
{
   if (plotManager().hasPlot() && plotManager().activePlot().hasManipulator())
      plotManager().activePlot().saveManipulator();
}

} // namespace graphics
} // namespace session
} // namespace r
