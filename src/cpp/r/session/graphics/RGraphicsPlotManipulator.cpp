/*
 * RGraphicsPlotManipulator.cpp
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

#include "RGraphicsPlotManipulator.hpp"

#include <core/Error.hpp>
#include <core/FilePath.hpp>

#include <r/RExec.hpp>
#include <r/RJson.hpp>

using namespace rstudio::core;

namespace rstudio {
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

void PlotManipulator::clear()
{
   sexp_.releaseNow();
}

Error PlotManipulator::save(const FilePath& filePath) const
{
   // call manipulator save
   r::exec::RFunction manipSave("manipulate:::manipulatorSave");
   manipSave.addParam(sexp_.get());
   manipSave.addParam(filePath.absolutePath());
   return manipSave.call();
}

Error PlotManipulator::load(const FilePath& filePath)
{
   // call manipulator load
   r::exec::RFunction manipLoad("manipulate:::manipulatorLoad");
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
   if (!empty())
   {
      // build manipulator json
      core::json::Object manipulator;

      // meta-info
      manipulator["id"] = getAsJson(".id");
      manipulator["controls"] = getControlsAsJson();
      manipulator["variables"] = getAsJson(".variables");

      // variable values
      core::json::Value valuesJson;
      SEXP valuesSEXP = getUserVisibleValuesList();
      Error error = r::json::jsonValueFromObject(valuesSEXP, &valuesJson);
      if (error)
         LOG_ERROR(error);
      manipulator["values"] = valuesJson;

      // return manipualtor
      *pValue = manipulator;
   }
   else
   {
      *pValue = core::json::Value();
   }
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

core::json::Object PlotManipulator::getControlAsJson(SEXP controlSEXP) const
{
   // field names
   std::vector<std::string> names ;
   Error error = sexp::getNames(controlSEXP, &names);
   if (error)
   {
      LOG_ERROR(error);
      return core::json::Object();
   }

   // json object to return
   core::json::Object control;

   int length = r::sexp::length(controlSEXP);
   for (int i=0; i<length; i++)
   {
      // get name and control
      std::string name = names[i];

      // screen out values field
      if (name == "values")
         continue;

      // get json for field
      SEXP fieldSEXP = VECTOR_ELT(controlSEXP, i);
      core::json::Value fieldValue;
      Error error = r::json::jsonValueFromObject(fieldSEXP, &fieldValue);
      if (error)
      {
         LOG_ERROR(error);
         return core::json::Object();
      }

      // set the field
      control[name] = fieldValue;
   }

   // return the control
   return control;
}

core::json::Object PlotManipulator::getControlsAsJson() const
{
   // extract controls
   r::sexp::Protect rProtect;
   SEXP controlsSEXP = get(".controls");
   if (controlsSEXP != R_NilValue)
   {
      rProtect.add(controlsSEXP);

      // are there any controls contained in the list?
      if (r::sexp::length(controlsSEXP) > 0)
      {

         // control names
         std::vector<std::string> controlNames ;
         Error error = sexp::getNames(controlsSEXP, &controlNames);
         if (error)
         {
            LOG_ERROR(error);
            return core::json::Object();
         }

         // json object to return
         core::json::Object controls;

         int length = r::sexp::length(controlsSEXP);
         for (int i=0; i<length; i++)
         {
            // get name and control
            std::string name = controlNames[i];
            SEXP controlSEXP = VECTOR_ELT(controlsSEXP, i);
            controls[name] = getControlAsJson(controlSEXP);
         }

         // return controls
         return controls;
      }
      else
      {
         return core::json::Object();
      }
   }
   else
   {
      return core::json::Object();
   }
}

SEXP PlotManipulator::getUserVisibleValuesList() const
{
   SEXP variablesSEXP = get(".variables");
   if (variablesSEXP != R_NilValue)
   {
      r::exec::RFunction userValuesFunction("manipulate:::userVisibleValues");
      userValuesFunction.addParam(sexp());
      userValuesFunction.addParam(variablesSEXP);
      r::sexp::Protect rProtect;
      SEXP valuesSEXP;
      Error error = userValuesFunction.call(&valuesSEXP, &rProtect);
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
} // namespace r
} // namespace rstudio

