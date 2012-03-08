/*
 * TexSynctex.cpp
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

#include <core/tex/TexSynctex.hpp>

#include <boost/algorithm/string/predicate.hpp>

#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/StringUtils.hpp>

#include "synctex/synctex_parser.h"

namespace core {
namespace tex {

std::ostream& operator << (std::ostream& stream, const SourceLocation& loc)
{
   stream << loc.file() << " [" << loc.line() << ", " << loc.column() << "]";
   return stream;
}

std::ostream& operator << (std::ostream& stream, const PdfLocation& loc)
{
   stream << loc.page() << " {" <<
             loc.x() << ", " <<
             loc.y() << ", " <<
             loc.width() << ", " <<
             loc.height() << "}";

   return stream;
}

struct Synctex::Impl
{
   Impl() : scanner(NULL) {}

   FilePath pdfPath;
   synctex_scanner_t scanner;
};


Synctex::Synctex()
  : pImpl_(new Impl())
{
}

Synctex::~Synctex()
{
   try
   {
      if (pImpl_->scanner != NULL)
      {
         ::synctex_scanner_free(pImpl_->scanner);
         pImpl_->scanner = NULL;
      }
   }
   catch(...)
   {

   }
}


bool Synctex::parse(const FilePath& pdfPath)
{
   using namespace core::string_utils;
   pImpl_->pdfPath = pdfPath;
   std::string path = utf8ToSystem(pdfPath.absolutePath());
   std::string buildDir = utf8ToSystem(pdfPath.parent().absolutePath());

   pImpl_->scanner = ::synctex_scanner_new_with_output_file(path.c_str(),
                                                            buildDir.c_str(),
                                                            1);
   return pImpl_->scanner != NULL;
}

PdfLocation Synctex::forwardSearch(const SourceLocation& location)
{
   PdfLocation pdfLocation;

   // first determine the synctex local name for the input file
   std::string name = synctexNameForInputFile(location.file());
   if (name.empty())
      return PdfLocation();

   // run the query
   int result = ::synctex_display_query(pImpl_->scanner,
                                        name.c_str(),
                                        location.line(),
                                        location.column());
   if (result > 0)
   {
      synctex_node_t node = synctex_next_result(pImpl_->scanner);
      if (node != NULL)
      {
         int page = ::synctex_node_page(node);

         float x = ::synctex_node_box_visible_h(node);
         float y = ::synctex_node_box_visible_v(node) -
                   ::synctex_node_box_visible_height(node);
         float w = ::synctex_node_box_visible_width(node);
         float h = ::synctex_node_box_visible_depth(node) +
                   ::synctex_node_box_visible_height(node);

         pdfLocation = PdfLocation(page, x, y, w, h);
      }
   }

   return pdfLocation;
}

SourceLocation Synctex::inverseSearch(const PdfLocation& location)
{
   SourceLocation sourceLocation;

   int result = ::synctex_edit_query(pImpl_->scanner,
                                     location.page(),
                                     location.x(),
                                     location.y());
   if (result > 0)
   {
      synctex_node_t node = synctex_next_result(pImpl_->scanner);
      if (node != NULL)
      {
         // get the filename
         std::string filename = ::synctex_scanner_get_name(
                                                   pImpl_->scanner,
                                                   ::synctex_node_tag(node));

         // might be relative or might be absolute, complete it against the
         // pdf's parent directory to cover both cases
         FilePath filePath = pImpl_->pdfPath.parent().complete(filename);

         // return source location
         sourceLocation = SourceLocation(filePath,
                                         ::synctex_node_line(node),
                                         ::synctex_node_column(node));
      }
   }

   return sourceLocation;
}


std::string Synctex::synctexNameForInputFile(const FilePath& inputFile)
{
   // get the base directory for the input file
   FilePath parentPath = inputFile.parent();

   // iterate through the known input files looking for a match
   synctex_node_t node = ::synctex_scanner_input(pImpl_->scanner);
   while (node != NULL)
   {
      // tex name
      std::string name = ::synctex_scanner_get_name(pImpl_->scanner,
                                                    ::synctex_node_tag(node));

      // if it starts with a ./ then trim that for the purposes of comparison
      std::string adjustedName = name;
      if (boost::algorithm::starts_with(name, "./") && (name.length() > 2))
         adjustedName = name.substr(2);

      // complete the name against the parent path -- if it is equal to
      // the input file that that's the one we are looking for
      if (parentPath.complete(adjustedName) == inputFile)
         return name;

      // next node
      node = ::synctex_node_sibling(node);
   }

   // not found
   return std::string();
}

} // namespace tex
} // namespace core 



