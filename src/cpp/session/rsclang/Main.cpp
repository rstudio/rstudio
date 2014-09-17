/*
 * Main.cpp
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

#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/system/System.hpp>

#include <libclang/LibClang.hpp>

#include "rsclang-config.h"

using namespace core;
using namespace rsclang;

namespace {



} // anonymous namespace


int main(int argc, char** argv)
{
  core::system::initializeStderrLog("rstudio-rsclang",
                                    core::system::kLogLevelWarning);

  // ignore SIGPIPE
  Error error = core::system::ignoreSignal(core::system::SigPipe);
  if (error)
     LOG_ERROR(error);




  return EXIT_SUCCESS;
}
