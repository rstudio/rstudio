/*
 * DesktopMainWindow.cpp
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

#include "DesktopMainWindow.hpp"

#include <QtGui>
#include <QtWebKit>

#include <boost/bind.hpp>

#include <core/FilePath.hpp>
#include <core/system/System.hpp>

#include "DesktopGwtCallback.hpp"
#include "DesktopMenuCallback.hpp"
#include "DesktopWebView.hpp"
#include "DesktopOptions.hpp"
#include "DesktopSlotBinders.hpp"

using namespace core;

extern QProcess* pRSessionProcess;

namespace desktop {

MainWindow::MainWindow(QUrl url) :
      BrowserWindow(false, url, NULL),
      menuCallback_(this),
      gwtCallback_(this),
      updateChecker_(this)
{
   quitConfirmed_ = false;
   pToolbar_->setVisible(false);

   // Dummy menu bar to deal with the fact that
   // the real menu bar isn't ready until well
   // after startup.
   QMenuBar* pMainMenuStub = new QMenuBar(this);
   pMainMenuStub->addMenu("File");
   pMainMenuStub->addMenu("Edit");
   pMainMenuStub->addMenu("View");
   pMainMenuStub->addMenu("Workspace");
   pMainMenuStub->addMenu("Plots");
   pMainMenuStub->addMenu("Help");
   setMenuBar(pMainMenuStub);

   connect(&menuCallback_, SIGNAL(menuBarCompleted(QMenuBar*)),
           this, SLOT(setMenuBar(QMenuBar*)));
   connect(&menuCallback_, SIGNAL(commandInvoked(QString)),
           this, SLOT(invokeCommand(QString)));
   connect(&menuCallback_, SIGNAL(manageCommand(QString,QAction*)),
           this, SLOT(manageCommand(QString,QAction*)));

   connect(&gwtCallback_, SIGNAL(workbenchInitialized()),
           this, SIGNAL(workbenchInitialized()));
   connect(&gwtCallback_, SIGNAL(workbenchInitialized()),
           this, SLOT(onWorkbenchInitialized()));

   setWindowIcon(QIcon(":/icons/RStudio.ico"));

#ifdef Q_OS_MAC
   QMenuBar* pDefaultMenu = new QMenuBar();
   pDefaultMenu->addMenu(new WindowMenu());
#endif

   //setContentsMargins(10000, 0, -10000, 0);
   setStyleSheet("QMainWindow { background: #e1e2e5; }");
}

void MainWindow::onWorkbenchInitialized()
{
   //QTimer::singleShot(300, this, SLOT(resetMargins()));

#ifdef Q_WS_MACX
   webView()->page()->mainFrame()->evaluateJavaScript(
         "document.body.className = document.body.className + ' avoid-move-cursor'");
#endif

   // check for updates
   updateChecker_.performCheck(false);
}

void MainWindow::resetMargins()
{
   setContentsMargins(0, 0, 0, 0);
}

void MainWindow::loadUrl(const QUrl& url)
{
   webView()->load(url);
}

void MainWindow::quit()
{
   quitConfirmed_ = true;
   close();
}

void MainWindow::onJavaScriptWindowObjectCleared()
{
   webView()->page()->mainFrame()->addToJavaScriptWindowObject(
         "desktop",
         &gwtCallback_,
         QScriptEngine::QtOwnership);
   webView()->page()->mainFrame()->addToJavaScriptWindowObject(
         "desktopMenuCallback",
         &menuCallback_,
         QScriptEngine::QtOwnership);
}

void MainWindow::invokeCommand(QString commandId)
{
   webView()->page()->mainFrame()->evaluateJavaScript(
         "window.desktopHooks.invokeCommand('" + commandId + "');");
}

void MainWindow::manageCommand(QString cmdId, QAction* action)
{
   QWebFrame* pMainFrame = webView()->page()->mainFrame();
   action->setVisible(pMainFrame->evaluateJavaScript(
         "window.desktopHooks.isCommandVisible('" + cmdId + "')").toBool());
   action->setEnabled(pMainFrame->evaluateJavaScript(
         "window.desktopHooks.isCommandEnabled('" + cmdId + "')").toBool());
   action->setText(pMainFrame->evaluateJavaScript(
         "window.desktopHooks.getCommandLabel('" + cmdId + "')").toString());
}

void MainWindow::closeEvent(QCloseEvent* pEvent)
{
   QWebFrame* pFrame = webView()->page()->mainFrame();
   if (!pFrame)
   {
       pEvent->accept();
       return;
   }

   QVariant hasQuitR = pFrame->evaluateJavaScript("!!window.desktopHooks");

   if (quitConfirmed_
       || !hasQuitR.toBool()
       || pRSessionProcess->state() != QProcess::Running)
   {
      pEvent->accept();
   }
   else
   {
      bool save;
      switch (desktop::options().saveWorkspaceOnExit())
      {
      case SAVE_YES:
         save = true;
         break;
      case SAVE_NO:
         save = false;
         break;
      case SAVE_ASK:
      default:
         QMessageBox prompt(QMessageBox::Warning,
                            "Quit R Session",
                            "Save workspace image?",
                            QMessageBox::NoButton,
                            this,
                            Qt::Sheet | Qt::Dialog |
                            Qt::MSWindowsFixedSizeDialogHint);
         prompt.setWindowModality(Qt::WindowModal);
         QPushButton* pSave = prompt.addButton("&Save",
                                                   QMessageBox::AcceptRole);
         prompt.addButton("&Don't Save", QMessageBox::DestructiveRole);
         QPushButton* pCancel = prompt.addButton("Cancel",
                                                     QMessageBox::RejectRole);
         prompt.setDefaultButton(pSave);


         FunctionSlotBinder aw(boost::bind(&QDialog::activateWindow, &prompt),
                               &prompt);
         QTimer::singleShot(10, &aw, SLOT(execute()));
         QTimer::singleShot(25, &aw, SLOT(execute()));
         QTimer::singleShot(50, &aw, SLOT(execute()));
         QTimer::singleShot(100, &aw, SLOT(execute()));

         prompt.exec();

         QAbstractButton* pClicked = prompt.clickedButton();
         if (!pClicked || pClicked == pCancel)
         {
            pEvent->ignore();
            return;
         }
         save = pClicked == pSave;
         break;
      }

      pFrame->evaluateJavaScript("window.desktopHooks.quitR(" +
                                 (save ? QString("true") : QString("false")) +
                                 ")");
      pEvent->ignore();
   }
}

void MainWindow::setMenuBar(QMenuBar *pMenubar)
{
   delete menuBar();
   this->QMainWindow::setMenuBar(pMenubar);
}

void MainWindow::openFileInRStudio(QString path)
{
   QFileInfo fileInfo(path);
   if (!fileInfo.isAbsolute() || !fileInfo.exists() || !fileInfo.isFile())
      return;

   path = path.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");

   webView()->page()->mainFrame()->evaluateJavaScript(
         "window.desktopHooks.openFile(\"" + path + "\")");
}

void MainWindow::checkForUpdates()
{
   updateChecker_.performCheck(true);
}

} // namespace desktop
