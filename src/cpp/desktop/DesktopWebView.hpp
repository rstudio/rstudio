/*
 * DesktopWebView.hpp
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

#ifndef DESKTOP_WEB_VIEW_HPP
#define DESKTOP_WEB_VIEW_HPP

#include <QtGui>
#include <QWebEngineView>

#include "DesktopWebPage.hpp"

namespace rstudio {
namespace desktop {

class MainWindow;

class WebView : public QWebEngineView
{
   Q_OBJECT

public:
   explicit WebView(QUrl baseUrl = QUrl(),
                    QWidget *parent = nullptr,
                    bool allowExternalNavigate = false);

   explicit WebView(QWebEngineProfile *profile,
                    QUrl baseUrl = QUrl(),
                    QWidget *parent = nullptr,
                    bool allowExternalNavigate = false);

   void setBaseUrl(const QUrl& baseUrl);
   QUrl baseUrl();

   void activateSatelliteWindow(QString name);
   void prepareForWindow(const PendingWindow& pendingWnd);

   bool event(QEvent* event) override;
   void childEvent(QChildEvent *event) override;

   WebPage* webPage() const { return pWebPage_; }

   void contextMenuEvent(QContextMenuEvent* event) override;
   void mouseNavigateButtonClick(QMouseEvent* pMouseEvent);

Q_SIGNALS:
  void onCloseWindowShortcut();
  void onClose();

public Q_SLOTS:

protected:
   QString promptForFilename(const QNetworkRequest& request,
                             QNetworkReply* pReply);
   void keyPressEvent(QKeyEvent* pEvent) override;
   void closeEvent(QCloseEvent* pEv) override;
   void dragEnterEvent(QDragEnterEvent *e) override;
   void dropEvent(QDropEvent *e) override;

protected Q_SLOTS:
   void openFile(QString file);

private:
   void init();

   QUrl baseUrl_;
   WebPage* pWebPage_;
};

} // namespace desktop
} // namespace rstudio

#endif // DESKTOP_WEB_VIEW_HPP
