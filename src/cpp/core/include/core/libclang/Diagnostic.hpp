/*
 * Diagnostic.hpp
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

#ifndef CORE_LIBCLANG_DIAGNOSTIC_HPP
#define CORE_LIBCLANG_DIAGNOSTIC_HPP

#include <boost/noncopyable.hpp>
#include <boost/shared_ptr.hpp>

#include "clang-c/Index.h"

#include "SourceRange.hpp"
#include "SourceLocation.hpp"

namespace rstudio {
namespace core {
namespace libclang {

class FixIt
{
public:
   FixIt(const SourceRange& sourceRange, std::string replacement)
      : sourceRange_(sourceRange), replacement_(replacement)
   {
   }

   SourceRange sourceRange() const { return sourceRange_; }
   std::string replacement() const { return replacement_; }

private:
   SourceRange sourceRange_;
   std::string replacement_;
};

class DiagnosticSet;

class Diagnostic : boost::noncopyable
{
public:
   explicit Diagnostic(CXDiagnostic diagnostic)
      : diagnostic_(diagnostic)
   {
   }

   ~Diagnostic();

   std::string format() const;
   std::string format(unsigned options) const;

   CXDiagnosticSeverity severity() const;
   SourceLocation location() const;
   std::string spelling() const;

   unsigned category() const;
   std::string categoryText() const;

   std::string enableOption() const;
   std::string disableOption() const;

   unsigned numRanges() const;
   SourceRange getSourceRange(unsigned i) const;

   unsigned numFixIts() const;
   FixIt getFixIt(unsigned i) const;

   boost::shared_ptr<DiagnosticSet> children() const;

private:
   CXDiagnostic diagnostic_;
};

class DiagnosticSet : boost::noncopyable
{
public:
   explicit DiagnosticSet(CXDiagnosticSet diagnosticSet, bool dispose)
      : diagnosticSet_(diagnosticSet), dispose_(dispose)
   {
   }

   virtual ~DiagnosticSet();

   unsigned diagnostics() const;
   boost::shared_ptr<Diagnostic> getDiagnostic(unsigned i) const;

private:
   CXDiagnosticSet diagnosticSet_;
   bool dispose_;
};

} // namespace libclang
} // namespace core
} // namespace rstudio

#endif // CORE_LIBCLANG_DIAGNOSTIC_HPP
