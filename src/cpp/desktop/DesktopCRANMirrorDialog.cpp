/*
 * DesktopCRANMirrorDialog.cpp
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

#include "DesktopCRANMirrorDialog.hpp"
#include "ui_DesktopCRANMirrorDialog.h"

#include <QtGui>
#include <QNetworkAccessManager>
#include <QNetworkReply>
#include <QNetworkRequest>

#include <core/text/CsvParser.hpp>

#include "DesktopOptions.hpp"
#include "DesktopURLDownloader.hpp"
#include "DesktopUtils.hpp"

const int ROLE_URL = Qt::UserRole;

using namespace desktop;

CRANMirrorDialog::CRANMirrorDialog(QWidget *parent) :
   QDialog(parent),
   ui(new Ui::CRANMirrorDialog()),
   pOK_(NULL)
{
    ui->setupUi(this);

    pOK_ = new QPushButton("OK");
    ui->buttonBox->addButton(pOK_, QDialogButtonBox::AcceptRole);

    QPushButton* pCancel = new QPushButton("Cancel");
    ui->buttonBox->addButton(pCancel, QDialogButtonBox::RejectRole);

    connect(ui->listWidget, SIGNAL(itemSelectionChanged()),
            this, SLOT(manageButtons()));
    manageButtons();

    // create URL downloader (it deletes itself when completed)
    QUrl url("http://cran.r-project.org/CRAN_mirrors.csv");
    URLDownloader* pURLDownloader = new URLDownloader(url, 5000, false, this);
    connect(pURLDownloader, SIGNAL(downloadComplete(const QByteArray&)),
            this, SLOT(loadMirrorCsv(const QByteArray&)));
    connect(pURLDownloader, SIGNAL(downloadError(const QString&)),
            this, SLOT(showNetworkError(const QString&)));
    connect(pURLDownloader, SIGNAL(downloadTimeout()),
            this, SLOT(requestTimeout()));
}

CRANMirrorDialog::~CRANMirrorDialog()
{
   delete ui;
}

QString CRANMirrorDialog::selectedName()
{
   QList<QListWidgetItem*> selectedItems = ui->listWidget->selectedItems();
   if (selectedItems.size() == 0)
      return QString();

   return selectedItems.at(0)->text();
}

QString CRANMirrorDialog::selectedURL()
{
   QList<QListWidgetItem*> selectedItems = ui->listWidget->selectedItems();
   if (selectedItems.size() == 0)
      return QString();

   return selectedItems.at(0)->data(ROLE_URL).toString();
}

void CRANMirrorDialog::manageButtons()
{
   pOK_->setEnabled(ui->listWidget->selectedItems().size());
}

namespace {

std::string lookup(const std::map<std::string, std::string>& data, const std::string& key)
{
   std::map<std::string, std::string>::const_iterator it = data.find(key);
   if (it == data.end())
      return std::string();
   else
      return it->second;
}

} // anonymous namespace


void CRANMirrorDialog::showNetworkError(const QString& errorString)
{
   QMessageBox errorDialog(
         safeMessageBoxIcon(QMessageBox::Warning),
         "Error Download Mirror List",
         "An error occurred while retrieving mirrors from CRAN:<br/><br/><i>"
         + Qt::escape(errorString)
         + "</i><br/><br/>Using local mirror list instead.",
         QMessageBox::NoButton,
         this);
   errorDialog.setTextFormat(Qt::RichText);
   errorDialog.addButton(new QPushButton("OK"), QMessageBox::AcceptRole);
   errorDialog.exec();

   QDir rDocPath(desktop::options().rDocPath());
   if (rDocPath.exists())
   {
      QString csvPath = rDocPath.absoluteFilePath("CRAN_mirrors.csv");
      QFile mirrors(csvPath);
      if (mirrors.exists())
      {
         if (mirrors.open(QIODevice::ReadOnly))
         {
            QByteArray data = mirrors.readAll();
            loadMirrorCsv(data);
         }
      }
   }
}

void CRANMirrorDialog::requestTimeout()
{
   if (ui->listWidget->count() == 0)
      showNetworkError("The CRAN server took too long to respond.");
}

void CRANMirrorDialog::loadMirrorCsv(const QByteArray& data)
{
   std::vector<char> csvBuffer(data.size());
   ::memcpy(&(csvBuffer[0]),
            data.constData(),
            data.size());

   int usRows = 0;
   while (true)
   {
      std::pair<std::vector<std::string>, std::vector<char>::iterator> result;
      result = core::text::parseCsvLine(csvBuffer.begin(), csvBuffer.end());
      csvBuffer.erase(csvBuffer.begin(), result.second);
      std::vector<std::string> line = result.first;

      if (line.size() == 0)
         break;

      if (headers_.empty())
      {
         headers_ = line;
      }
      else
      {
         std::map<std::string, std::string> mirror;
         for (size_t i = 0; i < line.size(); i++)
         {
            mirror.insert(std::pair<std::string, std::string>(headers_[i],
                                                              line[i]));
         }

         if (lookup(mirror, "OK") == "0")
            continue;

         QString url = QString::fromAscii(lookup(mirror, "URL").c_str());
         QString name = QString::fromUtf8(lookup(mirror, "Name").c_str());
         QString host = QString::fromUtf8(lookup(mirror, "Host").c_str());
         QString countryCode = QString::fromAscii(
               lookup(mirror, "CountryCode").c_str());

         QListWidgetItem* item = new QListWidgetItem(name + " - " + host);
         item->setData(ROLE_URL, url);

         if (countryCode == "us")
            ui->listWidget->insertItem(usRows++, item);
         else
            ui->listWidget->addItem(item);
      }
   }

   if (ui->listWidget->count() > 0)
      ui->listWidget->item(0)->setSelected(true);

   ui->lblProgress->setText("");
}
