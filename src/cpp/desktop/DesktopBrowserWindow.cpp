/*
 * DesktopBrowserWindow.cpp
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

#include "DesktopBrowserWindow.hpp"

#include <QAccessibleWidget>
#include <QApplication>
#include <QToolBar>
#include <QWebChannel>
#include <QWebEngineScript>
#include <QWebEngineScriptCollection>

#include "DesktopInfo.hpp"
#include "DesktopOptions.hpp"

namespace rstudio {
namespace desktop {

namespace {

void initializeWebchannel(BrowserWindow* pWindow)
{
   auto* page = pWindow->webView()->webPage();
   
   // create web channel
   auto* channel = new QWebChannel(pWindow);
   channel->registerObject(QStringLiteral("desktopInfo"), &desktopInfo());
   page->setWebChannel(channel);
   
   // load qwebchannel.js
   QFile webChannelJsFile(QStringLiteral(":/qtwebchannel/qwebchannel.js"));
   if (!webChannelJsFile.open(QFile::ReadOnly))
      qWarning() << "Failed to load qwebchannel.js";

   QString webChannelJs = QString::fromUtf8(webChannelJsFile.readAll());
   webChannelJsFile.close();

   // append our WebChannel initialization code
   const char* webChannelInit =
R"EOF(new QWebChannel(qt.webChannelTransport, function(channel) {

   // export channel objects to the main window
   for (var key in channel.objects) {
      window[key] = channel.objects[key];
   }

   // notify that we're finished initialization and load
   // GWT sources if necessary
   window.qt.webChannelReady = true;
   if (typeof window.rstudioDelayLoadApplication == "function") {
      window.rstudioDelayLoadApplication();
      window.rstudioDelayLoadApplication = null;
   }
});
   )EOF";

   webChannelJs.append(QString::fromUtf8(webChannelInit));

   QWebEngineScript script;
   script.setName(QStringLiteral("qwebchannel"));
   script.setInjectionPoint(QWebEngineScript::DocumentCreation);
   script.setWorldId(QWebEngineScript::MainWorld);
   script.setSourceCode(webChannelJs);
   page->scripts().insert(script);
}

// Partial fix for VoiceOver / Screen Reader support on macOS, based on the work described
// in link below. This is the subset that is in application code, more complete fix also requires
// changes to QtWebEngine itself, tentatively expected in Qt 5.14.
//
// This partial fix somewhat improves behavior of VoiceOver on desktop RStudio in that more
// keyboard focus changes are noticed and announced than without the change. Still
// it is far from usable and at this stage is mostly for fact-finding experimentation.
//
// https://codereview.qt-project.org/c/qt/qtwebengine/+/281210
//
class BrowserWindowAccessibility : public QAccessibleWidget
{
   static QAccessibleInterface *findFocusChild(QAccessibleInterface *iface)
   {
      if (!iface)
         return nullptr;

      if (iface->state().focused)
         return iface;

      for (int i = 0; i < iface->childCount(); ++i)
      {
         if (QAccessibleInterface *focusChild = findFocusChild(iface->child(i)))
            return focusChild;
      }

      return nullptr;
   }

public:
   BrowserWindowAccessibility(BrowserWindow *bw) : QAccessibleWidget(bw, QAccessible::Window)
   {
   }

   QAccessibleInterface *focusChild() const override
   {
      QAccessibleInterface *iface = QAccessibleWidget::focusChild();

      BrowserWindow *bw = qobject_cast<BrowserWindow *>(widget());
      if (iface == QAccessible::queryAccessibleInterface(bw->webView()->focusWidget()))
      {
         QAccessibleInterface *viewInterface = QAccessible::queryAccessibleInterface(bw->webView());
         return findFocusChild(viewInterface->child(0));
      }

      return iface;
   }
};

QAccessibleInterface *accessibleFactory(const QString &key, QObject *object)
{
   Q_UNUSED(key)

   if (BrowserWindow *bw = qobject_cast<BrowserWindow *>(object))
      return new BrowserWindowAccessibility(bw);

   return nullptr;
}

} // end anonymous namespace

BrowserWindow::BrowserWindow(bool showToolbar,
                             bool adjustTitle,
                             QString name,
                             QUrl baseUrl,
                             QWidget* pParent,
                             WebPage* pOpener,
                             bool allowExternalNavigate) :
   QMainWindow(pParent),
   name_(name),
   pOpener_(pOpener)
{
#ifdef Q_OS_MAC
   // NSAccessibility queries the window for the focused accessibility
   // element. QAccessibleWidget::focusChild() returns the accessibility interface
   // of the RenderWidgetHostViewQtDelegateWidget because it has the active
   // focus instead of the QWebEngineView, so install accessibility factory to
   // compensate for this macOS-specific behavior.
   if (desktop::options().enableAccessibility())
      QAccessible::installFactory(&accessibleFactory);
#endif

   adjustTitle_ = adjustTitle;
   progress_ = 0;

   pView_ = new WebView(baseUrl, this, allowExternalNavigate);
   connect(pView_, SIGNAL(titleChanged(QString)), SLOT(adjustTitle()));
   connect(pView_, SIGNAL(loadProgress(int)), SLOT(setProgress(int)));
   connect(pView_, SIGNAL(loadFinished(bool)), SLOT(finishLoading(bool)));
   
   initializeWebchannel(this);
   
   // set zoom factor
   double zoomLevel = options().zoomLevel();
   pView_->setZoomFactor(zoomLevel);

   // Kind of a hack to new up a toolbar and not attach it to anything.
   // Once it is clear what secondary browser windows look like we can
   // decide whether to keep this.
   pToolbar_ = showToolbar ? addToolBar(tr("Navigation")) : new QToolBar();
   pToolbar_->setMovable(false);

   setCentralWidget(pView_);

   desktop::enableFullscreenMode(this, false);
}

void BrowserWindow::closeEvent(QCloseEvent *event)
{
   if (pOpener_ == nullptr)
   {
      // if we don't know where we were opened from, check window.opener
      // (note that this could also be empty)
      QString cmd = QString::fromUtf8("if (window.opener && "
         "window.opener.unregisterDesktopChildWindow)"
         "   window.opener.unregisterDesktopChildWindow('");
      cmd.append(name_);
      cmd.append(QString::fromUtf8("');"));
      webPage()->runJavaScript(cmd);
   }
   else if (pOpener_)
   {
      // if we do know where we were opened from and it has the appropriate
      // handlers, let it know we're closing
      QString cmd = QString::fromUtf8(
                  "if (window.unregisterDesktopChildWindow) "
                  "   window.unregisterDesktopChildWindow('");
      cmd.append(name_);
      cmd.append(QString::fromUtf8("');"));
      webPage()->runJavaScript(cmd);
   }

   // forward the close event to the page
   webPage()->event(event);
}

void BrowserWindow::adjustTitle()
{
   if (adjustTitle_)
      setWindowTitle(pView_->title());
}

void BrowserWindow::setProgress(int p)
{
   progress_ = p;
   adjustTitle();
}

void BrowserWindow::finishLoading(bool succeeded)
{
   progress_ = 100;
   adjustTitle();

   if (succeeded)
   {
      // If the screen that the Window is being shown on has changed,
      // then force a quick re-size of the window so that the contents
      // can be re-rendered. This helps fix issues where the window is
      // moved between displays with different DPIs, where the text
      // can occasionally be briefly 'fuzzy'.
      QObject::connect(
               this->window()->windowHandle(),
               &QWindow::screenChanged,
               [&]()
      {
         // We use a timer here as it's been observed messing with the
         // window size while the screenChanged signal is being handled
         // can cause the screenChanged signal to be repeatedly fired
         // during the window move.
         QTimer::singleShot(0, [&]() {
            QSize originalSize = size();
            resize(originalSize.width(), originalSize.height() + 1);
            resize(originalSize.width(), originalSize.height() + 0);
         });

      });

      QString cmd = QString::fromUtf8("if (window.opener && "
         "window.opener.registerDesktopChildWindow)"
         "   window.opener.registerDesktopChildWindow('");
      cmd.append(name_);
      cmd.append(QString::fromUtf8("', window);"));
      webPage()->runJavaScript(cmd);
   }
}

WebView* BrowserWindow::webView()
{
   return pView_;
}

void BrowserWindow::avoidMoveCursorIfNecessary()
{
#ifdef Q_OS_MAC
   webView()->page()->runJavaScript(
         QString::fromUtf8("document.body.className = document.body.className + ' avoid-move-cursor'"));
#endif
}

QWidget* BrowserWindow::asWidget()
{
   return this;
}

WebPage* BrowserWindow::webPage()
{
   return webView()->webPage();
}

void BrowserWindow::postWebViewEvent(QEvent *keyEvent)
{
   for (auto* child : webView()->children())
      QApplication::sendEvent(child, keyEvent);
}

void BrowserWindow::triggerPageAction(QWebEnginePage::WebAction action)
{
   webView()->triggerPageAction(action);
}

QString BrowserWindow::getName()
{
   return name_;
}

WebPage* BrowserWindow::opener()
{
   return pOpener_;
}

} // namespace desktop
} // namespace rstudio
