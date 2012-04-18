/*
 * RGraphicsPlotManipulatorManager.hpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef R_SESSION_GRAPHICS_PLOT_MANIPULATOR_MANAGER_HPP
#define R_SESSION_GRAPHICS_PLOT_MANIPULATOR_MANAGER_HPP

#include <boost/signal.hpp>

#include <core/Error.hpp>
#include <core/json/Json.hpp>

#include <r/RSexp.hpp>

#include "RGraphicsTypes.hpp"

namespace core {
   class Error;
}

namespace r {
namespace session {
namespace graphics {

// singleton
class PlotManipulatorManager;
PlotManipulatorManager& plotManipulatorManager();


class PlotManipulatorManager : boost::noncopyable
{   
private:
   friend PlotManipulatorManager& plotManipulatorManager();
   PlotManipulatorManager();

public:
   virtual ~PlotManipulatorManager() {}

public:
   core::Error initialize(const UnitConversionFunctions& convert);

   boost::signal<void ()>& onShowManipulator() ;
   void setPlotManipulatorValues(const core::json::Object& values);
   void manipulatorPlotClicked(int x, int y);
   
   void executeAndAttachManipulator(SEXP manipulatorSEXP);
   bool hasActiveManipulator() const;
   SEXP activeManipulator() const;

   bool replayingManipulator() const { return replayingManipulator_; }
   SEXP pendingManipulatorSEXP() const { return pendingManipulatorSEXP_; }

   void clearPendingManipulatorState()
   {
      pendingManipulatorSEXP_ = R_NilValue;
      replayingManipulator_ = false;
   }

   void ensureManipulatorSaved();

private:
   bool manipulatorIsActive() const;
   bool trackingMouseClicks(SEXP manipulatorSEXP) const;
   void replayManipulator(SEXP manipulatorSEXP);

private:   
   // pending manipulator
   SEXP pendingManipulatorSEXP_;

   // are we currently replaying a manipulator call?
   bool replayingManipulator_;

   // manipulator event hook
   boost::signal<void ()> onShowManipulator_;

   // unit conversion function
   UnitConversionFunctions convert_;

};
   
} // namespace graphics
} // namespace session
} // namespace r

#endif // R_SESSION_GRAPHICS_PLOT_MANIPULATOR_MANAGER_HPP

