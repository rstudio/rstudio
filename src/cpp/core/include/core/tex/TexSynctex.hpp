/*
 * TexSynctex.hpp
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

// This is a C++ wrapper for the SyncTeX utilities from the TexLive project.
// We have an embedded copy of the synctex code at core/tex/synctex. The
// original sources can be accessed/updated from:
//
//     svn://www.tug.org/texlive/trunk/Build/source/texk/web2c/synctexdir/
//

#ifndef CORE_TEX_TEX_SYNCTEX_HPP
#define CORE_TEX_TEX_SYNCTEX_HPP

#include <iosfwd>
#include <string>

#include <boost/utility.hpp>
#include <boost/scoped_ptr.hpp>

#include <shared_core/FilePath.hpp>

namespace rstudio {
namespace core {
namespace tex {

class SourceLocation
{
public:
   SourceLocation()
      : line_(0)
   {
   }

   SourceLocation(const FilePath& file, int line, int column)
      : file_(file), line_(line), column_(column)
   {
   }

   // COPYING: via compiler

   bool empty() const { return file().isEmpty(); }

   const FilePath& file() const { return file_; }

   // 1-based line and column. Note that synctex returns 0 if there
   // is no column information available
   int line() const { return line_; }
   int column() const { return column_; }

private:
   FilePath file_;
   int line_;
   int column_;
};


std::ostream& operator << (std::ostream& stream, const SourceLocation& loc);

class PdfLocation
{
public:
   PdfLocation()
      : page_(0), x_(0), y_(0), width_(0), height_(0)
   {
   }

   PdfLocation(int page, float x, float y)
      : page_(page), x_(x), y_(y), width_(0), height_(0)
   {
   }

   PdfLocation(int page, float x, float y, float width, float height)
      : page_(page), x_(x), y_(y), width_(width), height_(height)
   {
   }

   // COPYING: via compiler

   bool empty() const { return page() < 1; }

   // 1-based pages
   int page() const { return page_; }

   // coordinates in 72-dpi units (require transform for both dpi as
   // as well as whatever magnfification level is currently active)
   float x() const { return x_; }
   float y() const { return y_; }
   float width() const { return width_; }
   float height() const { return height_; }

private:
   int page_;
   float x_;
   float y_;
   float width_;
   float height_;
};

std::ostream& operator << (std::ostream& stream, const PdfLocation& loc);

class Synctex : boost::noncopyable
{
public:
   Synctex();
   virtual ~Synctex();
   // COPYING: prohibited

public:
   bool parse(const FilePath& pdfPath);

   PdfLocation forwardSearch(const SourceLocation& location);
   SourceLocation inverseSearch(const PdfLocation& location);

   PdfLocation topOfPageContent(int page);

private:
   std::string synctexNameForInputFile(const FilePath& inputFile);

private:
   struct Impl;
   boost::scoped_ptr<Impl> pImpl_;
};

std::string normalizeSynctexName(const std::string& name);


} // namespace tex
} // namespace core 
} // namespace rstudio


#endif // CORE_TEX_TEX_SYNCTEX_HPP

