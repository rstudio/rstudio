/*
 * SumatraSynctex.hpp
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

#ifndef DESKTOP_SYNCTEX_SUMATRASYNCTEX_HPP
#define DESKTOP_SYNCTEX_SUMATRASYNCTEX_HPP

#include <QObject>
#include <QMap>
#include <QPoint>

#include <DesktopSynctex.hpp>

namespace desktop {

class MainWindow;

namespace synctex {

class SumatraSynctex : public Synctex
{
   Q_OBJECT

public:
   explicit SumatraSynctex(MainWindow* pMainWindow);

   virtual void syncView(const QString& pdfFile,
                         const QString& srcFile,
                         const QPoint& srcLoc);

   virtual void syncView(const QString& pdfFile, int pdfPage);

private:
   QString sumatraExePath_;
};


} // namespace synctex
} // namespace desktop

#endif // DESKTOP_SYNCTEX_SUMATRASYNCTEX_HPP
