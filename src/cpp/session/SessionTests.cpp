/*
 * SessionTests.cpp
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

#include <tests/TestThat.hpp>

#include <core/Error.hpp>

#include <r/RExec.hpp>
#include <r/RSexp.hpp>

#include <session/SessionModuleContext.hpp>

namespace rstudio {
namespace session {
namespace tests {

using namespace rstudio::core;

void ouch()
{
   Rf_error("Ouch!");
}

context("RExec::tryCatch")
{
   test_that("tryCatch handles R errors")
   {
      SEXP resultSEXP;
      r::sexp::Protect protect;
      Error error = r::exec::tryCatch(boost::bind(ouch), &resultSEXP, &protect);
      
      expect_true(error == Success());
      expect_true(r::sexp::inherits(resultSEXP, "error"));
   }
}

} // end namespace tests
} // end namespace session
} // end namespace rstudio
