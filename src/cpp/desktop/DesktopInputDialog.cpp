/*
 * DesktopInputDialog.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#include "DesktopInputDialog.hpp"
#include "DesktopUtils.hpp"
#include "ui_DesktopInputDialog.h"

#include <QPushButton>

InputDialog::InputDialog(QWidget *parent) :
    QDialog(parent),
    ui(new Ui::InputDialog()),
    pOK_(NULL)
{
   ui->setupUi(this);
   setWindowFlags(Qt::Dialog | Qt::MSWindowsFixedSizeDialogHint);

   pOK_ = new QPushButton(QString::fromUtf8("OK"));
   ui->buttonBox->addButton(pOK_, QDialogButtonBox::AcceptRole);

   QPushButton* pCancel = new QPushButton(QString::fromUtf8("Cancel"));
   ui->buttonBox->addButton(pCancel, QDialogButtonBox::RejectRole);

   ui->extraOption->setVisible(false);
}

InputDialog::~InputDialog()
{
   delete ui;
}

QString InputDialog::caption()
{
   return ui->label->text();
}

void InputDialog::setCaption(const QString& caption)
{
   ui->label->setText(caption);
}

QString InputDialog::textValue()
{
   return ui->lineEdit->text();
}

void InputDialog::setTextValue(const QString& value)
{
   ui->lineEdit->setText(value);
}

void InputDialog::setSelection(int offset, int length)
{
   offset = std::min(offset, textValue().size());
   length = std::min(length,
                     textValue().size() - offset);

   ui->lineEdit->setSelection(offset, length);
}

void InputDialog::setOkButtonLabel(const QString& label)
{
   pOK_->setText(label);
}

void InputDialog::setEchoMode(QLineEdit::EchoMode mode)
{
   ui->lineEdit->setEchoMode(mode);
}

void InputDialog::setNumbersOnly(bool numbersOnly)
{
   if (numbersOnly)
      ui->lineEdit->setInputMask(QString::fromUtf8("D99999999"));
   else
      ui->lineEdit->setInputMask(QString());
}

void InputDialog::setExtraOptionPrompt(const QString& prompt)
{
   ui->extraOption->setVisible(!prompt.isEmpty());
   ui->extraOption->setText(prompt);
}

void InputDialog::setExtraOption(bool extraOption)
{
   ui->extraOption->setCheckState(Qt::Checked);
}

bool InputDialog::extraOption()
{
   return ui->extraOption->checkState() == Qt::Checked;
}

void InputDialog::done(int r)
{
   if (QDialog::Accepted == r) // ok was pressed
   {
      if (ui->lineEdit->text().size() == 0)
      {
         rstudio::desktop::showWarning(this,
                     QString::fromUtf8("Error"),
                     QString::fromUtf8("You must enter a value."));
         return;
       }
   }
   QDialog::done(r);
}
