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

void setManipulatorValue(SEXP manipulatorSEXP,
                         const std::pair<std::string,json::Value>& object)
{
   Error error = r::sexp::setNamedListElement(manipulatorSEXP,
                                              object.first,
                                              object.second);

   if (error)
      LOG_ERROR(error);
}

void safeExecuteManipulator(SEXP manipulatorSEXP)
{
   try
   {
      // execute the code within the manipulator.
      Error error = r::exec::RFunction(".rs.manipulator.execute",
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

SEXP rs_setManipulatorState(SEXP stateSEXP)
{
   plotManipulatorManager().setActiveManipulatorState(stateSEXP);
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

   // register set manipulator state routine
   R_CallMethodDef setManipulatorStateMethodDef ;
   setManipulatorStateMethodDef.name = "rs_setManipulatorState" ;
   setManipulatorStateMethodDef.fun = (DL_FUNC) rs_setManipulatorState;
   setManipulatorStateMethodDef.numArgs = 1;
   r::routines::addCallMethod(setManipulatorStateMethodDef);

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

void PlotManipulatorManager::setActiveManipulatorState(SEXP stateSEXP)
{
   SEXP manipulatorSEXP = activeManipulator();
   if (manipulatorSEXP != R_NilValue)
   {
      Error error = r::sexp::setNamedListElement(manipulatorSEXP,
                                                 "manip_state",
                                                 stateSEXP);
      if (error)
         LOG_ERROR(error);

      // if the active plot has a manipulator then ensure it is saved.
      ensurePlotManipulatorSaved();
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
                    boost::bind(setManipulatorValue, manipulatorSEXP, _1));

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

void PlotManipulatorManager::ensurePlotManipulatorSaved()
{
   if (plotManager().hasPlot() && plotManager().activePlot().hasManipulator())
      plotManager().activePlot().saveManipulator();
}

} // namespace graphics
} // namespace session
} // namespace r



