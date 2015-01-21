/*
 * Diagnostic.cpp
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

#include <core/libclang/Diagnostic.hpp>

#include <boost/make_shared.hpp>

#include <core/libclang/Utils.hpp>

#include <core/libclang/LibClang.hpp>

namespace rstudio {
namespace core {
namespace libclang {

Diagnostic::~Diagnostic()
{
   try
   {
      clang().disposeDiagnostic(diagnostic_);
   }
   catch(...)
   {
   }
}

std::string Diagnostic::format() const
{
   return format(clang().defaultDiagnosticDisplayOptions());
}

std::string Diagnostic::format(unsigned options) const
{
   return toStdString(clang().formatDiagnostic(diagnostic_, options));
}

CXDiagnosticSeverity Diagnostic::getSeverity() const
{
   return clang().getDiagnosticSeverity(diagnostic_);
}

SourceLocation Diagnostic::getLocation() const
{
   return SourceLocation(clang().getDiagnosticLocation(diagnostic_));
}

std::string Diagnostic::getSpelling() const
{
   return toStdString(clang().getDiagnosticSpelling(diagnostic_));
}

} // namespace libclang
} // namespace core
} // namespace rstudio

