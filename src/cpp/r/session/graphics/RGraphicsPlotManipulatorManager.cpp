/*
 * RGraphicsPlotManipulatorManager.cpp
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

void setManipulatorJsonValue(SEXP manipulatorSEXP,
                             const std::pair<std::string,json::Value>& object)
{
   // get the actual value to assign
   r::exec::RFunction setFunction("manipulate:::setManipulatorValue");
   setFunction.addParam(manipulatorSEXP);
   setFunction.addParam(object.first);
   setFunction.addParam(object.second);
   Error error = setFunction.call();
   if (error)
      LOG_ERROR(error);
}

void setManipulatorValueToFalse(SEXP manipulatorSEXP, const std::string& name)
{
   setManipulatorJsonValue(manipulatorSEXP,
                           std::make_pair(name, json::toJsonValue(false)));
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

void setManipulatorButtonsToFalse(SEXP manipulatorSEXP)
{
   // get the list of buttons
   std::vector<std::string> buttonNames;
   Error error = r::exec::RFunction("manipulate:::buttonNames",
                                    manipulatorSEXP).call(&buttonNames);
   if (error)
   {
      logAndReportError(error, ERROR_LOCATION);
      return;
   }

   // set the values
   std::for_each(buttonNames.begin(),
                 buttonNames.end(),
                 boost::bind(setManipulatorValueToFalse,
                             manipulatorSEXP,
                             _1));
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
      

Error PlotManipulatorManager::initialize(
                           const UnitConversionFunctions& convert)
{
   // save reference to device conversion function
   convert_ = convert;

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

bool PlotManipulatorManager::trackingMouseClicks(SEXP manipulatorSEXP) const
{
   r::exec::RFunction trackingMouseClicks("manipulate:::trackingMouseClicks");
   trackingMouseClicks.addParam(manipulatorSEXP);
   bool tracking = false;
   Error error = trackingMouseClicks.call(&tracking);
   if (error)
      logAndReportError(error, ERROR_LOCATION);
   return tracking;
}

bool PlotManipulatorManager::manipulatorIsActive() const
{
   return plotManager().hasPlot() &&
          plotManager().activePlot().hasManipulator();
}

void PlotManipulatorManager::replayManipulator(SEXP manipulatorSEXP)
{
   replayingManipulator_ = true;
   safeExecuteManipulator(manipulatorSEXP);
   replayingManipulator_ = false;
}

void PlotManipulatorManager::setPlotManipulatorValues(const json::Object& values)
{
   if (manipulatorIsActive())
   {
      // get the manipulator
      SEXP manipulatorSEXP = plotManager().activePlot().manipulatorSEXP();

      // set the underlying values
      std::for_each(values.begin(),
                    values.end(),
                    boost::bind(setManipulatorJsonValue, manipulatorSEXP, _1));

      // replay the manipulator
      replayManipulator(manipulatorSEXP);

      // set all of the buttons to false
      setManipulatorButtonsToFalse(manipulatorSEXP);
   }
   else
   {
      LOG_WARNING_MESSAGE("called setPlotManipulatorValues but active plot "
                          "has no manipulator");
   }
}

void PlotManipulatorManager::manipulatorPlotClicked(int x, int y)
{
   if (manipulatorIsActive())
   {
      // get the manipulator
      SEXP manipulatorSEXP = plotManager().activePlot().manipulatorSEXP();

      // check if we are tracking mouse clicks
      if (trackingMouseClicks(manipulatorSEXP))
      {
         // transform the coordinates to user
         double deviceX = x;
         double deviceY = y;
         double userX = x;
         double userY = y; 
         convert_.deviceToUser(&userX, &userY);
         double ndcX = x;
         double ndcY = y;
         convert_.deviceToNDC(&ndcX,&ndcY);

         // set the mouse click state
         r::exec::RFunction setMouseClick("manipulate:::setMouseClick");
         setMouseClick.addParam(manipulatorSEXP);
         setMouseClick.addParam(deviceX);
         setMouseClick.addParam(deviceY);
         setMouseClick.addParam(userX);
         setMouseClick.addParam(userY);
         setMouseClick.addParam(ndcX);
         setMouseClick.addParam(ndcY);
         Error error = setMouseClick.call();
         if (error)
         {
            logAndReportError(error, ERROR_LOCATION);
            return;
         }

         // replay the manipulator
         replayManipulator(manipulatorSEXP);

         // unset the mouse click state
         r::exec::RFunction clearMouseClick("manipulate:::clearMouseClick");
         clearMouseClick.addParam(manipulatorSEXP);
         error = clearMouseClick.call();
         if (error)
            logAndReportError(error, ERROR_LOCATION);
      }
   }
   else
   {
      LOG_WARNING_MESSAGE("called manipulatorPlotClicked but active plot "
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
