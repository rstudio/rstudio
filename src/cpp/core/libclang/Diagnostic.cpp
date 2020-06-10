/*
 * Diagnostic.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

CXDiagnosticSeverity Diagnostic::severity() const
{
   return clang().getDiagnosticSeverity(diagnostic_);
}

SourceLocation Diagnostic::location() const
{
   return SourceLocation(clang().getDiagnosticLocation(diagnostic_));
}

std::string Diagnostic::spelling() const
{
   return toStdString(clang().getDiagnosticSpelling(diagnostic_));
}

unsigned Diagnostic::category() const
{
   return clang().getDiagnosticCategory(diagnostic_);
}

std::string Diagnostic::categoryText() const
{
   return toStdString(clang().getDiagnosticCategoryText(diagnostic_));
}

std::string Diagnostic::enableOption() const
{
   return toStdString(clang().getDiagnosticOption(diagnostic_, nullptr));
}

std::string Diagnostic::disableOption() const
{
   CXString disableStr;
   clang().getDiagnosticOption(diagnostic_, &disableStr);
   return toStdString(disableStr);
}

unsigned Diagnostic::numRanges() const
{
   return clang().getDiagnosticNumRanges(diagnostic_);
}

SourceRange Diagnostic::getSourceRange(unsigned i) const
{
   return SourceRange(clang().getDiagnosticRange(diagnostic_, i));
}

unsigned Diagnostic::numFixIts() const
{
   return clang().getDiagnosticNumFixIts(diagnostic_);
}

FixIt Diagnostic::getFixIt(unsigned i) const
{
   CXSourceRange cxRange;
   CXString cxReplacement = clang().getDiagnosticFixIt(diagnostic_,
                                                       i,
                                                       &cxRange);

   return FixIt(SourceRange(cxRange), toStdString(cxReplacement));
}


boost::shared_ptr<DiagnosticSet> Diagnostic::children() const
{
   boost::shared_ptr<DiagnosticSet> pChildren;
   CXDiagnosticSet set = clang().getChildDiagnostics(diagnostic_);
   if (set != nullptr)
      pChildren.reset(new DiagnosticSet(set, false));
   return pChildren;
}

DiagnosticSet::~DiagnosticSet()
{
   try
   {
      if (dispose_)
         clang().disposeDiagnosticSet(diagnosticSet_);
   }
   catch(...)
   {
   }
}

unsigned DiagnosticSet::diagnostics() const
{
   return clang().getNumDiagnosticsInSet(diagnosticSet_);
}

boost::shared_ptr<Diagnostic> DiagnosticSet::getDiagnostic(unsigned i) const
{
   return boost::shared_ptr<Diagnostic>(
     new Diagnostic(clang().getDiagnosticInSet(diagnosticSet_, i))
   );
}


} // namespace libclang
} // namespace core
} // namespace rstudio

