/*
 * EvinceSynctex.hpp
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

#ifndef DESKTOP_SYNCTEX_EVINCESYNCTEX_HPP
#define DESKTOP_SYNCTEX_EVINCESYNCTEX_HPP

#undef QT_NO_CAST_FROM_ASCII
#include <QObject>
#include <QMap>
#include <QPoint>

#include <QDBusPendingCallWatcher>
#define QT_NO_CAST_FROM_ASCII

#include <DesktopSynctex.hpp>

namespace rstudio {
namespace desktop {

class MainWindow;

namespace synctex {

class EvinceDaemon;
class EvinceWindow;

class EvinceSynctex : public Synctex
{
   Q_OBJECT

public:
   explicit EvinceSynctex(const SynctexViewerInfo& viewerInfo,
                          MainWindow* pMainWindow);

   virtual void syncView(const QString& pdfFile,
                         const QString& srcFile,
                         const QPoint& srcLoc);

   virtual void syncView(const QString& pdfFile, int pdfPage);

private Q_SLOTS:
   void onFindWindowFinished(QDBusPendingCallWatcher *pCall);
   void onSyncViewFinished(QDBusPendingCallWatcher *pCall);
   void onClosed();
   void onSyncSource(const QString &source_file,
                     const QPoint &source_point,
                     uint timestamp);


private:
   struct SyncRequest
   {
      SyncRequest()
         : page(-1)
      {
      }

      SyncRequest(QString pdfFile)
         : pdfFile(pdfFile), page(-1)
      {
      }

      SyncRequest(QString pdfFile, int page)
         : pdfFile(pdfFile), page(page)
      {
      }

      SyncRequest(QString pdfFile, QString srcFile, QPoint srcLoc)
         : pdfFile(pdfFile), page(-1), srcFile(srcFile), srcLoc(srcLoc)
      {
      }

      bool hasSourceLoc() const { return !srcFile.isEmpty(); }
      bool hasPage() const { return page != -1; }

      QString pdfFile;
      int page;
      QString srcFile;
      QPoint srcLoc;
   };

   void syncView(const SyncRequest& syncRequest);

   void syncView(EvinceWindow* pWindow, const SyncRequest& syncRequest);
   void syncView(EvinceWindow* pWindow,
                 const QString& srcFile,
                 const QPoint& srcLoc);

private:
   SynctexViewerInfo viewerInfo_;
   EvinceDaemon* pEvince_;
   QMap<QString, EvinceWindow*> windows_;

   QMap<QDBusPendingCallWatcher*, SyncRequest> pendingSyncRequests_;
};


} // namespace synctex
} // namespace desktop
} // namespace rstudio

#endif // DESKTOP_SYNCTEX_EVINCESYNCTEX_HPP
