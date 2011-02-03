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
#include <r/RJson.hpp>

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

Error PlotManipulator::save(const FilePath& filePath) const
{
   // call manipulator save
   r::exec::RFunction manipSave(".rs.manipulatorSave");
   manipSave.addParam(sexp_.get());
   manipSave.addParam(filePath.absolutePath());
   return manipSave.call();
}

Error PlotManipulator::load(const FilePath& filePath)
{
   // call manipulator load
   r::exec::RFunction manipLoad(".rs.manipulatorLoad");
   manipLoad.addParam(filePath.absolutePath());
   r::sexp::Protect rProtect;
   SEXP manipSEXP;
   Error error = manipLoad.call(&manipSEXP, &rProtect);
   if (error)
      return error;

   // set it
   sexp_.set(manipSEXP);
   return Success();
}

void PlotManipulator::asJson(core::json::Value* pValue) const
{
   // build manipulator json
   core::json::Object manipulator;

   // meta-info
   manipulator["id"] = getAsJson(".id");
   manipulator["controls"] = getAsJson(".controls");
   manipulator["variables"] = getAsJson(".variables");

   // variable values
   core::json::Value valuesJson;
   SEXP valuesSEXP = getValuesList();
   Error error = r::json::jsonValueFromObject(valuesSEXP, &valuesJson);
   if (error)
      LOG_ERROR(error);
   manipulator["values"] = valuesJson;

   // return manipualtor
   *pValue = manipulator;
}

SEXP PlotManipulator::sexp() const
{
   return sexp_.get();
}

SEXP PlotManipulator::get(const std::string& name) const
{
   if (!empty())
   {
      r::exec::RFunction getFunction("get");
      getFunction.addParam(name);
      getFunction.addParam("envir", sexp());
      r::sexp::Protect rProtect;
      SEXP valueSEXP;
      Error error = getFunction.call(&valueSEXP, &rProtect);
      if (error)
      {
         LOG_ERROR(error);
         return R_NilValue;
      }
      else
      {
         return valueSEXP;
      }
   }
   else
   {
      return R_NilValue;
   }
}

core::json::Value PlotManipulator::getAsJson(const std::string& name) const
{
   core::json::Value value;
   Error error = r::json::jsonValueFromObject(get(name), &value);
   if (error)
      LOG_ERROR(error);
   return value;
}

SEXP PlotManipulator::getValuesList() const
{
   SEXP variablesSEXP = get(".variables");
   if (variablesSEXP != R_NilValue)
   {
      r::exec::RFunction mgetFunction("mget");
      mgetFunction.addParam(variablesSEXP);
      mgetFunction.addParam("envir", sexp());
      r::sexp::Protect rProtect;
      SEXP valuesSEXP;
      Error error = mgetFunction.call(&valuesSEXP, &rProtect);
      if (error)
      {
         LOG_ERROR(error);
         return R_NilValue;
      }
      else
      {
         return valuesSEXP;
      }
   }
   else
   {
      return R_NilValue;
   }
}


} // namespace graphics
} // namespace session
} // namesapce r

