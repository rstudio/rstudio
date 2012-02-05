/*
 * SessionTexPdfLatex.cpp
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

#include "SessionTexPdfLatex.hpp"


#include <session/SessionModuleContext.hpp>

using namespace core;

namespace session {
namespace modules { 
namespace tex {
namespace pdflatex {

bool isInstalled()
{
   return !module_context::findProgram("pdflatex").empty();
}

} // namespace pdflatex
} // namespace tex
} // namespace modules
} // namesapce session

