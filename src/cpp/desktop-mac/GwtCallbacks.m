

#import <Cocoa/Cocoa.h>

#include "GwtCallbacks.h"

// https://developer.apple.com/library/mac/documentation/AppleApplications/Conceptual/SafariJSProgTopics/Tasks/ObjCFromJavaScript.html
// https://developer.apple.com/library/mac/samplecode/CallJS/Introduction/Intro.html#//apple_ref/doc/uid/DTS10004241=

@implementation GwtCallbacks

- (id)init
{
   return [super init];
}

- (NSString*) proportionalFont
{
   NSLog(@"%@", NSStringFromSelector(_cmd));
   
   return @"proportionalFont";
}

- (NSString*) fixedWidthFont
{
   NSLog(@"%@", NSStringFromSelector(_cmd));
   
   return @"fixedWidthFont";
}

- (int) collectPendingQuitRequest
{
   NSLog(@"%@", NSStringFromSelector(_cmd));
   
   return (0);
}

- (void) workbenchInitialized
{
   NSLog(@"%@", NSStringFromSelector(_cmd));
}

- (void) browseUrl: (NSString*) url
{
   NSLog(@"%@", NSStringFromSelector(_cmd));
}

- (NSString*) getOpenFileName: (NSString*) caption dir: (NSString*) dir filter: (NSString*) filter
{
   NSLog(@"%@", NSStringFromSelector(_cmd));
   
   return @"getOpenFileName";
}

- (NSString*) getSaveFileName: (NSString*) caption
              dir: (NSString* ) dir
              defaultExtension: (NSString*) defaultExtension
              forceDefaultExtension: (Boolean) forceDefaultExtension
{
   NSLog(@"%@", NSStringFromSelector(_cmd));
   
   return @"getSaveFileName";
}

- (NSString*) getExistingDirectory: (NSString*) caption dir: (NSString*) dir
{
   NSLog(@"%@", NSStringFromSelector(_cmd));
   
   return @"getExistingDirectory";
}

- (void) undo
{
   NSLog(@"%@", NSStringFromSelector(_cmd));
}

- (void) redo
{
   NSLog(@"%@", NSStringFromSelector(_cmd));
}

- (void) clipboardCut
{
   NSLog(@"%@", NSStringFromSelector(_cmd));
}

- (void) clipboardCopy
{
   NSLog(@"%@", NSStringFromSelector(_cmd));
}

- (void) clipboardPaste
{
   NSLog(@"%@", NSStringFromSelector(_cmd));
}

- (NSString*) getUriForPath: (NSString*) path
{
   NSLog(@"%@", NSStringFromSelector(_cmd));
   
   return @"getUriForPath";
}


- (NSString*) onWorkbenchInitialized: (NSString*) scratchPath
{
   NSLog(@"%@", NSStringFromSelector(_cmd));
   
   return @"onWorkbenchInitialized";
}

- (void) showFolder: (NSString*) path
{
    NSLog(@"%@", NSStringFromSelector(_cmd));
}

- (void) showFile: (NSString*) path
{
    NSLog(@"%@", NSStringFromSelector(_cmd));
}

- (NSString*) getRVersion
{
   NSLog(@"%@", NSStringFromSelector(_cmd));
   
   return @"getRVersion";
}

- (NSString*) chooseRVersion
{
   NSLog(@"%@", NSStringFromSelector(_cmd));
   
   return @"chooseRVersion";
}

- (Boolean) canChooseRVersion
{
   NSLog(@"%@", NSStringFromSelector(_cmd));
   
   return false;
}

- (Boolean) isRetina
{
   NSLog(@"%@", NSStringFromSelector(_cmd));
   
   return true;
}

- (void) openMinimalWindow: (NSString*) name url: (NSString*) url
                     width: (int) width height: (int) height
{
   NSLog(@"%@", NSStringFromSelector(_cmd));
}

- (void) activateSatelliteWindow: (NSString*) name
{
   NSLog(@"%@", NSStringFromSelector(_cmd));
}

- (void) prepareForSatelliteWindow: (NSString*) name
                             width: (int) width height: (int) height
{
   NSLog(@"%@", NSStringFromSelector(_cmd));
}

- (void) copyImageToClipboard: (int) left top: (int) top
                        width: (int) width height: (int) height
{
   NSLog(@"%@", NSStringFromSelector(_cmd));
}

- (Boolean) supportsClipboardMetafile
{
   NSLog(@"%@", NSStringFromSelector(_cmd));
   return false;
}

- (int) showMessageBox: type
               caption: (NSString*) caption
               message: (NSString*) message
               buttons: (NSString*) buttons
         defaultButton: (int) defaultButton
          cancelButton: (int) cancelButton
{
   NSLog(@"%@", NSStringFromSelector(_cmd));
   return 42;
}

/*
 
 
 public slots:

 
 QVariant promptForText(QString title,
 QString caption,
 QString defaultValue,
 bool usePasswordMask,
 QString rememberPasswordPrompt,
 bool rememberByDefault,
 bool numbersOnly,
 int selectionStart,
 int selectionLength);
 
 void checkForUpdates();
 void showAboutDialog();
 void bringMainFrameToFront();
 
 QString filterText(QString text);
 
 void cleanClipboard(bool stripHtml);
 
 void setPendingQuit(int pendingQuit);
 
 void openProjectInNewWindow(QString projectFilePath);
 
 void openTerminal(QString terminalPath,
 QString workingDirectory,
 QString extraPathEntries);
 
 QVariant getFontList(bool fixedWidthOnly);
 QString getFixedWidthFont();
 void setFixedWidthFont(QString font);
 
 QVariant getZoomLevels();
 double getZoomLevel();
 void setZoomLevel(double zoomLevel);
 
 bool forceFastScrollFactor();
 
 QString getDesktopSynctexViewer();
 
 void externalSynctexPreview(QString pdfPath, int page);
 
 void externalSynctexView(const QString& pdfFile,
 const QString& srcFile,
 int line,
 int column);
 
 bool supportsFullscreenMode();
 void toggleFullscreenMode();
 void showKeyboardShortcutHelp();
 
 void launchSession(bool reload);
 
 void reloadZoomWindow();
 
 void setViewerUrl(QString url);
 
 
 */






+ (NSString *) webScriptNameForSelector: (SEL) sel
{
   if (sel == @selector(browseUrl:))
      return @"browseUrl";
   else if (sel == @selector(getOpenFileName:dir:filter:))
      return @"getOpenFileName";
   else if (sel == @selector(getSaveFileName:dir:defaultExtension:forceDefaultExtension:))
      return @"getSaveFileName";
   else if (sel == @selector(getExistingDirectory:dir:))
      return @"getExistingDirectory";
   else if (sel == @selector(getUriForPath:))
      return @"getUriForPath";
   else if (sel == @selector(onWorkbenchInitialized:))
      return @"onWorkbenchInitialized";
   else if (sel == @selector(showFolder:))
      return @"showFolder";
   else if (sel == @selector(showFile:))
      return @"showFile";
   else if (sel == @selector(openMinimalWindow:url:width:height:))
      return @"openMinimalWindow";
   else if (sel == @selector(activateSatelliteWindow:))
      return @"activateSatelliteWindow";
   else if (sel == @selector(prepareForSatelliteWindow:width:height:))
      return @"prepareForSatelliteWindow";
   else if (sel == @selector(copyImageToClipboard:top:width:height:))
      return @"copyImageToClipboard";
   else if (sel == @selector(showMessageBox:caption:message:buttons:defaultButton:cancelButton:))
      return @"showMessageBox";
   

   return nil;
}

+ (BOOL)isSelectorExcludedFromWebScript: (SEL) sel
{
   return NO;
}

@end

