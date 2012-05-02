/*
 * SumatraSynctex.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include "SumatraSynctex.hpp"

#include <boost/lexical_cast.hpp>

#include <core/Log.hpp>
#include <core/Error.hpp>

#include "DesktopUtils.hpp"

using namespace core;

namespace desktop {
namespace synctex {

namespace {


QStringList standardSumatraArgs()
{
   QStringList args;
   args.append(QString::fromAscii("-bg-color"));
   args.append(QString::fromAscii("#ffffff"));
   args.append(QString::fromAscii("-reuse-instance"));
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
   args.append(QString::fromAscii("-forward-search"));
   args.append(srcFile);
   args.append(
      QString::fromStdString(boost::lexical_cast<std::string>(srcLoc.x())));
   args.append(pdfFile);
   QProcess::startDetached(sumatraExePath_, args);
}

void SumatraSynctex::syncView(const QString& pdfFile, int page)
{
   QStringList args = standardSumatraArgs();
   args.append(QString::fromAscii("-page"));
   args.append(QString::fromStdString(boost::lexical_cast<std::string>(page)));
   args.append(pdfFile);
   QProcess::startDetached(sumatraExePath_, args);
}

} // namesapce synctex
} // namespace desktop
