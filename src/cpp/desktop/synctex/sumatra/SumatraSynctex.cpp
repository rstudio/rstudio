/*
 * SumatraSynctex.cpp
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

#include "SumatraSynctex.hpp"

#include <boost/lexical_cast.hpp>

#include <core/Log.hpp>
#include <shared_core/Error.hpp>
#include <shared_core/SafeConvert.hpp>
#include <core/system/Environment.hpp>

#include "DesktopUtils.hpp"
#include "DesktopOptions.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace desktop {
namespace synctex {

namespace {


QStringList standardSumatraArgs()
{
   QStringList args;
   args.append(QString::fromUtf8("-bg-color"));
   args.append(QString::fromUtf8("#ffffff"));
   args.append(QString::fromUtf8("-reuse-instance"));
   return args;
}

QStringList inverseSearchArgs(WId mainWindowId)
{
   QStringList args;
   args.append(QString::fromUtf8("-inverse-search"));

   QString cmdFormat;
   QString quote = QString::fromUtf8("\"");
   QString space = QString::fromUtf8(" ");

   // path to rsinverse binary
   std::string rsinverse = desktop::options().rsinversePath().getAbsolutePath();
   cmdFormat.append(quote + QString::fromStdString(rsinverse) + quote);
   cmdFormat.append(space);

   // main window handle
   std::string hwndStr = safe_convert::numberToString(mainWindowId);
   cmdFormat.append(QString::fromStdString(hwndStr));
   cmdFormat.append(space);

   // port
   cmdFormat.append(desktop::options().portNumber());
   cmdFormat.append(space);

   // shared secret
   cmdFormat.append(
          QString::fromStdString(core::system::getenv("RS_SHARED_SECRET")));
   cmdFormat.append(space);

   // file and line placeholders
   cmdFormat.append(QString::fromUtf8("\"%f\" %l"));
   args.append(cmdFormat);

   return args;
}

} // anonymous namespace

SumatraSynctex::SumatraSynctex(MainWindow* pMainWindow)
   : Synctex(pMainWindow)
{
   sumatraExePath_ = pMainWindow->getSumatraPdfExePath();
}

void SumatraSynctex::syncView(const QString& pdfFile,
                              const QString& srcFile,
                              const QPoint& srcLoc)
{
   QStringList args = standardSumatraArgs();
   args.append(QString::fromUtf8("-forward-search"));
   args.append(srcFile);
   args.append(
      QString::fromStdString(safe_convert::numberToString(srcLoc.x())));
   args.append(inverseSearchArgs(mainWindowId()));
   args.append(pdfFile);
   QProcess::startDetached(sumatraExePath_, args);
}

void SumatraSynctex::syncView(const QString& pdfFile, int page)
{
   QStringList args = standardSumatraArgs();
   args.append(QString::fromUtf8("-page"));
   args.append(QString::fromStdString(safe_convert::numberToString(page)));
   args.append(inverseSearchArgs(mainWindowId()));
   args.append(pdfFile);
   QProcess::startDetached(sumatraExePath_, args);
}

void SumatraSynctex::view(const QString& pdfFile, int pdfPage)
{
   QStringList args = standardSumatraArgs();
   if (pdfPage != -1)
   {
      args.append(QString::fromUtf8("-page"));
      args.append(QString::fromStdString(safe_convert::numberToString(pdfPage)));
   }
   args.append(pdfFile);
   QProcess::startDetached(sumatraExePath_, args);
}

} // namesapce synctex
} // namespace desktop
} // namespace rstudio
