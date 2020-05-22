/*
 * DesktopChooseRHome.cpp
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
#include "DesktopChooseRHome.hpp"
#include "ui_DesktopChooseRHome.h"

#include <QFileDialog>
#include <QInputDialog>
#include <QMessageBox>
#include <QListWidgetItem>

#include <core/system/System.hpp>
#include <core/system/Environment.hpp>

#include "DesktopDetectRHome.hpp"
#include "DesktopRVersion.hpp"
#include "DesktopUtils.hpp"

using namespace rstudio::core;
using namespace rstudio::desktop;

namespace {

static std::map<std::string, std::string> s_engineValueTolabel = {
   {"auto", "Auto-detect (recommended)"},
   {"desktop", "Desktop OpenGL"},
   {"software", "Software"}
};

static std::map<std::string, std::string> s_engineLabelToValue = {
   {"Auto-detect (recommended)", "auto"},
   {"Desktop OpenGL", "desktop"},
   {"Software", "software"}
};

QListWidgetItem* toItem(const RVersion& version)
{
   QListWidgetItem* pItem = new QListWidgetItem();
   pItem->setText(version.description());
   pItem->setData(Qt::UserRole, version.binDir());
   return pItem;
}

RVersion toVersion(QListWidgetItem* pItem)
{
   if (!pItem)
      return RVersion();
   return RVersion(pItem->data(Qt::UserRole).toString());
}

} // anonymous namespace

ChooseRHome::ChooseRHome(QList<RVersion> list, QWidget *parent) :
    QDialog(parent),
    ui(new Ui::ChooseRHome()),
    pOK_(nullptr)
{
    ui->setupUi(this);
    
    setWindowIcon(QIcon(QString::fromUtf8(":/icons/RStudio.ico")));

    setWindowFlags(
          (windowFlags() | Qt::Dialog)
          & ~Qt::WindowContextHelpButtonHint
          );
    
    QStringList engines = {
       QStringLiteral("Auto-detect (recommended)"),
       QStringLiteral("Desktop OpenGL"),
       QStringLiteral("Software")
    };
    ui->comboRenderingEngines->addItems(engines);

    pOK_ = new QPushButton(QString::fromUtf8("OK"));
    ui->buttonBox->addButton(pOK_, QDialogButtonBox::AcceptRole);

    QPushButton* pCancel = new QPushButton(QString::fromUtf8("Cancel"));
    ui->buttonBox->addButton(pCancel, QDialogButtonBox::RejectRole);

    for (int i = 0; i < list.size(); i++)
    {
       ui->listHomeDir->addItem(toItem(list.at(i)));
    }

    connect(ui->btnOther, SIGNAL(clicked()),
            this, SLOT(chooseOther()));
    connect(ui->listHomeDir, SIGNAL(itemSelectionChanged()),
            this, SLOT(validateSelection()));
    validateSelection();

    connect(ui->radioDefault, SIGNAL(toggled(bool)),
            this, SLOT(onModeChanged()));
    ui->radioDefault64->setChecked(true);
    connect(ui->radioDefault64, SIGNAL(toggled(bool)),
            this, SLOT(onModeChanged()));
    onModeChanged();
}

ChooseRHome::~ChooseRHome()
{
    delete ui;
}

void ChooseRHome::chooseOther()
{
   if (lastDir_.isEmpty())
   {
      lastDir_ = QString::fromLocal8Bit(rstudio::core::system::getenv("ProgramFiles").c_str());
   }

   QString dir = QFileDialog::getExistingDirectory(
         this,
         QString::fromUtf8("Choose R Directory"),
         lastDir_,
         QFileDialog::ShowDirsOnly | standardFileDialogOptions());

   if (dir.isEmpty())
      return;

   lastDir_ = dir;

   QList<RVersion> versions = detectVersionsInDir(dir);

   RVersion rVer;

   if (versions.size() == 0)
   {
      showWarning(
            this,
            QString::fromUtf8("Invalid R Directory"),
            QString::fromUtf8("This directory does not appear to contain a "
            "valid R installation.\n\nPlease try again."), QString());
      return;
   }
   else if (versions.size() > 1)
   {
      QStringList items;
      for (int i = 0; i < versions.size(); i++)
         items << versions.at(i).description();

      QInputDialog inputDialog(this);
      inputDialog.setOptions(QInputDialog::UseListViewForComboBoxItems);
      inputDialog.setComboBoxItems(items);
      inputDialog.setComboBoxEditable(false);
      inputDialog.setWindowTitle(QString::fromUtf8("Choose Version"));
      inputDialog.setLabelText(QString::fromUtf8("Please choose the version to use:"));

      if (inputDialog.exec() != QDialog::Accepted)
         return;

      int idx = items.indexOf(QRegExp(inputDialog.textValue(),
                                      Qt::CaseSensitive,
                                      QRegExp::FixedString));
      if (idx < 0)
         return;
      rVer = versions.at(idx);
   }
   else // versions.size() == 1
   {
      rVer = versions.at(0);
   }

   switch (rVer.validate())
   {
   case rstudio::desktop::ValidateSuccess:
      break;
   case rstudio::desktop::ValidateNotFound:
      showWarning(
            this,
            QString::fromUtf8("Invalid R Directory"),
            QString::fromUtf8("This directory does not appear to contain a "
                              "valid R installation.\n\nPlease try again."),
            QString());
      return;
   case rstudio::desktop::ValidateBadArchitecture:
      showWarning(
            this,
            QString::fromUtf8("Incompatible R Build"),
            QString::fromUtf8("The version of R you've selected was built "
                              "for a different CPU architecture and cannot "
                              "be used with this version of RStudio."),
            QString());
      return;
   case rstudio::desktop::ValidateVersionTooOld:
   default:
      showWarning(
            this,
            QString::fromUtf8("Incompatible R Build"),
            QString::fromUtf8("The version of R you've selected is not "
                              "compatible with RStudio. Please install a "
                              "newer version of R."),
            QString());
      return;
   }

   QList<QListWidgetItem*> items = ui->listHomeDir->findItems(
         rVer.description(), Qt::MatchExactly);
   if (!items.isEmpty())
   {
      ui->listHomeDir->setCurrentItem(items[0]);
   }
   else
   {
      QListWidgetItem* pItem = toItem(rVer);
      ui->listHomeDir->addItem(pItem);
      pItem->setSelected(true);
   }
}

void ChooseRHome::done(int r)
{
   if (r == QDialog::Accepted)
   {
      if (!ui->radioCustom->isChecked())
      {
         Architecture arch = preferR64() ? ArchX64 : ArchX86;
         if (rstudio::desktop::autoDetect(arch).isEmpty())
         {
            if (rstudio::desktop::allRVersions().length() > 0)
            {
               QString name = QString::fromUtf8(preferR64() ? "R64" : "R");

               showWarning(
                     this,
                     QString::fromUtf8("No %1 Installation Detected").arg(name),
                     QString::fromUtf8("No compatible %1 version was found. If you "
                                       "have a compatible version of %1 installed, "
                                       "please choose it manually."
                                       ).arg(name),
                     QString());
               ui->radioCustom->setChecked(true);
               return;
            }
            else
            {
               if (showYesNoDialog(
                     QMessageBox::Warning,
                     this,
                     QString::fromUtf8("R Not Installed"),
                     QString::fromUtf8("R does not appear to be installed. Please "
                                       "install R before using RStudio.\n\n"
                                       "You can download R from the official R Project "
                                       "website. Would you like to go there now?"),
                     QString(),
                     true /*yesDefault*/))
               {
                  rstudio::desktop::openUrl(QUrl(QString::fromUtf8("https://www.rstudio.org/links/r-project")));
               }
            }
         }
      }
      
      
   }

   this->QDialog::done(r);
}

void ChooseRHome::validateSelection()
{
   ui->listHomeDir->setEnabled(ui->radioCustom->isChecked());
   ui->btnOther->setEnabled(ui->radioCustom->isChecked());

   if (!ui->radioCustom->isChecked())
      ui->listHomeDir->setCurrentRow(-1);

   if (ui->radioCustom->isChecked())
   {
      pOK_->setEnabled(this->version().isValid());
   }
   else
   {
      pOK_->setEnabled(true);
   }
}

void ChooseRHome::onModeChanged()
{
   validateSelection();
}

RVersion ChooseRHome::version()
{
   if (!ui->radioCustom->isChecked())
      return QString();

   QList<QListWidgetItem*> selectedItems
         = ui->listHomeDir->selectedItems();
   return selectedItems.isEmpty()
             ? RVersion()
             : toVersion(selectedItems.at(0));
}

void ChooseRHome::setVersion(const RVersion& value, bool preferR64)
{
   if (value.isEmpty())
   {
      if (preferR64)
         ui->radioDefault64->setChecked(true);
      else
         ui->radioDefault->setChecked(true);
   }
   else
   {
      ui->radioCustom->setChecked(true);
      QList<QListWidgetItem*> matches =
            ui->listHomeDir->findItems(value.description(), Qt::MatchExactly);
      if (matches.size() > 0)
         matches.first()->setSelected(true);
      ui->listHomeDir->setFocus();
   }
}

QString ChooseRHome::renderingEngine()
{
   std::string entry = ui->comboRenderingEngines->currentText().toStdString();
   if (s_engineLabelToValue.count(entry))
      return QString::fromStdString(s_engineLabelToValue[entry]);
   else
      return QStringLiteral("auto");
}

void ChooseRHome::setRenderingEngine(const QString& renderingEngine)
{
   std::string engine = renderingEngine.toStdString();
   if (s_engineValueTolabel.count(engine))
      ui->comboRenderingEngines->setCurrentText(QString::fromStdString(s_engineValueTolabel[engine]));
   else
      ui->comboRenderingEngines->setCurrentText(QStringLiteral("Auto-detect (recommended)"));
}

bool ChooseRHome::preferR64()
{
   return ui->radioDefault64->isChecked();
}
