/*
 * RGraphicsPlotManipulator.cpp
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

#include "RGraphicsPlotManipulator.hpp"

#include <core/Error.hpp>
#include <core/FilePath.hpp>

#include <r/RExec.hpp>

using namespace core;

namespace r {
namespace session {
namespace graphics { 


PlotManipulator::PlotManipulator()
   : sexp_()
{
}

PlotManipulator::PlotManipulator(SEXP sexp)
   : sexp_(sexp)
{
}

PlotManipulator::~PlotManipulator()
{
   try
   {
   }
   catch(...)
   {
   }
}

Error PlotManipulator::save(const FilePath& filePath)
{
   // call manipulator save
   r::exec::RFunction manipSave(".rs.manipulator.save");
   manipSave.addParam(sexp_.get());
   manipSave.addParam(filePath.absolutePath());
   return manipSave.call();
}

Error PlotManipulator::load(const FilePath& filePath)
{
   // call manipulator load
   r::exec::RFunction manipLoad(".rs.manipulator.load");
   manipLoad.addParam(filePath.absolutePath());
   SEXP manipSEXP;
   Error error = manipLoad.call(&manipSEXP);
   if (error)
      return error;

   // set it
   sexp_.set(manipSEXP);
   return Success();
}




} // namespace graphics
} // namespace session
} // namesapce r

