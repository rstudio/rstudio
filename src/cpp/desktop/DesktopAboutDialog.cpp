/*
 * DesktopAboutDialog.cpp
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

#include "DesktopAboutDialog.hpp"
#include "ui_DesktopAboutDialog.h"

#include <core/FilePath.hpp>
#include <core/FileSerializer.hpp>
#include <core/system/System.hpp>

#include <QPushButton>

#include "DesktopOptions.hpp"
#include "config.h"

using namespace core;

AboutDialog::AboutDialog(QWidget *parent) :
      QDialog(parent, Qt::Dialog),
      ui(new Ui::AboutDialog())
{
   ui->setupUi(this);

   ui->buttonBox->addButton(new QPushButton(QString::fromAscii("OK")),
                            QDialogButtonBox::AcceptRole);

   ui->lblIcon->setPixmap(QPixmap(QString::fromAscii(":/icons/resources/freedesktop/icons/64x64/rstudio.png")));
   ui->lblVersion->setText(QString::fromAscii("Version " RSTUDIO_VERSION));

   setWindowModality(Qt::ApplicationModal);

   // read notice file
   FilePath supportingFilePath = desktop::options().supportingFilePath();
   FilePath noticePath = supportingFilePath.complete("NOTICE");
   std::string notice;
   Error error = readStringFromFile(noticePath, &notice);
   if (!error)
   {
      ui->textBrowser->setFontFamily(desktop::options().fixedWidthFont());
#ifdef Q_WS_MACX
      ui->textBrowser->setFontPointSize(11);
#else
      ui->textBrowser->setFontPointSize(9);
#endif
      ui->textBrowser->setText(QString::fromUtf8(notice.c_str()));
   }
}

AboutDialog::~AboutDialog()
{
   delete ui;
}
