/*
 * PdfLatexEngine.cpp
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

#include <core/tex/TexEngine.hpp>

#include <core/Error.hpp>
#include <core/FilePath.hpp>


namespace core {
namespace tex {


namespace {

class PdfLatexEngine : public TexEngine
{
public:
   PdfLatexEngine()
      : TexEngine("pdflatex")
   {
   }

private:

   virtual FilePath programFilePath()
   {
      return FilePath("/usr/texbin/pdflatex");
   }

};

} // anonymous namespace


Error createPdfLatex(boost::shared_ptr<TexEngine>* ppEngine)
{
   boost::shared_ptr<PdfLatexEngine> pNew(new PdfLatexEngine());

   *ppEngine = boost::shared_static_cast<TexEngine>(pNew);

   return Success();


}
   
} // namespace tex
} // namespace core 



