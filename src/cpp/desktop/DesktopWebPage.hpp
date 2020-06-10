/*
 * DesktopWebPage.hpp
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

#ifndef DESKTOP_WEB_PAGE_HPP
#define DESKTOP_WEB_PAGE_HPP

#include <queue>

#include <QtGui>
#include <QWebEnginePage>
#include <QWebEngineUrlRequestInfo>

#include "DesktopWebProfile.hpp"
#include "DesktopUtils.hpp"

namespace rstudio {
namespace desktop {

class MainWindow;

struct PendingWindow
{
   PendingWindow()
      : name(), pMainWindow(nullptr), x(-1), y(-1), width(-1), height(-1),
        isSatellite(false), allowExternalNavigate(false), showToolbar(false)
   {
   }

   PendingWindow(QString name,
                 MainWindow* pMainWindow,
                 int screenX,
                 int screenY,
                 int width,
                 int height);

   PendingWindow(QString name, bool allowExternalNavigation,
                 bool showDesktopToolbar)
      : name(name), pMainWindow(nullptr), isSatellite(false),
        allowExternalNavigate(allowExternalNavigation),
        showToolbar(showDesktopToolbar)
   {
   }

   bool isEmpty() const { return name.isEmpty(); }

   QString name;

   MainWindow* pMainWindow;

   int x = 0;
   int y = 0;
   int width = 0;
   int height = 0;
   bool isSatellite;
   bool allowExternalNavigate;
   bool showToolbar;
};


class WebPage : public QWebEnginePage
{
   Q_OBJECT

public:
   explicit WebPage(QUrl baseUrl = QUrl(),
                    QWidget *parent = nullptr,
                    bool allowExternalNavigate = false);

   explicit WebPage(QWebEngineProfile *profile,
                    QUrl baseUrl = QUrl(),
                    QWidget *parent = nullptr,
                    bool allowExternalNavigate = false);

   void setBaseUrl(const QUrl& baseUrl);
   void setTutorialUrl(const QString& tutorialUrl);
   void setViewerUrl(const QString& viewerUrl);
   void setShinyDialogUrl(const QString& shinyDialogUrl);
   void prepareExternalNavigate(const QString& externalUrl);

   void activateWindow(QString name);
   void prepareForWindow(const PendingWindow& pendingWnd);
   void closeWindow(QString name);

   void triggerAction(QWebEnginePage::WebAction action, bool checked = false) override;

   inline WebProfile* profile() { return static_cast<WebProfile*>(QWebEnginePage::profile()); }

public Q_SLOTS:
   bool shouldInterruptJavaScript();
   void closeRequested();
   void onUrlIntercepted(const QUrl& url, int type);

protected:
   QWebEnginePage* createWindow(QWebEnginePage::WebWindowType type) override;
   void javaScriptConsoleMessage(JavaScriptConsoleMessageLevel level, const QString& message,
                                 int lineNumber, const QString& sourceID) override;
   QString userAgentForUrl(const QUrl &url) const;
   bool acceptNavigationRequest(const QUrl &url, NavigationType, bool isMainFrame) override;
   
   QString tutorialUrl();
   QString viewerUrl();

private:
   void init();
   void handleBase64Download(QUrl url);

private:
   QUrl baseUrl_;
   QString tutorialUrl_;
   QString viewerUrl_;
   QString shinyDialogUrl_;
   bool allowExternalNav_;
   std::queue<PendingWindow> pendingWindows_;
   QDir defaultSaveDir_;
};

} // namespace desktop
} // namespace rstudio

#endif // DESKTOP_WEB_PAGE_HPP
