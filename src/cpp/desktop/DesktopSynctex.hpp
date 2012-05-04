/*
 * DesktopSynctex.hpp
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

#ifndef DESKTOP_SYNCTEX_HPP
#define DESKTOP_SYNCTEX_HPP

#include <QObject>

#include "DesktopMainWindow.hpp"
namespace desktop {

struct SynctexViewerInfo
{
   SynctexViewerInfo()
      : versionMajor(0), versionMinor(0), versionPatch(0)
   {
   }

   QString name;

   bool empty() const { return name.isEmpty(); }

   int version() const
   {
      return QT_VERSION_CHECK(versionMajor, versionMinor, versionPatch);
   }

   int versionMajor;
   int versionMinor;
   int versionPatch;
};


class Synctex : public QObject
{
public:
    // return the desktop viewer if there is one for this platform/environment
    static SynctexViewerInfo desktopViewerInfo();

    static Synctex* create(MainWindow* pMainWindow);

   Q_OBJECT
public:
   explicit Synctex(MainWindow* pMainWindow)
      : QObject(pMainWindow), pMainWindow_(pMainWindow)
   {
   }

   // the base Synctex class does nothing -- subclasses provide an
   // implementation that does something by overriding syncView and
   // calling onClosed and onSyncSource at the appropriate times

   virtual void syncView(const QString& pdfFile,
                         const QString& srcFile,
                         const QPoint& srcLoc)
   {
   }

   virtual void syncView(const QString& pdfFile, int pdfPage)
   {
   }

protected:
   WId mainWindowId() const { return pMainWindow_->effectiveWinId(); }

protected:
   void onClosed(const QString& pdfFile);
   void onSyncSource(const QString& srcFile, const QPoint& sourceLoc);

public slots:
   

private:
   MainWindow* pMainWindow_;
};

} // namespace desktop

#endif // DESKTOP_SYNCTEX_HPP
