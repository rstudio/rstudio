package org.rstudio.studio.client.workbench.views.source.editors.text.r;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.Rectangle;
import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.codetools.CodeToolsServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay.AnchoredSelection;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.CodeModel;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Renderer.ScreenCoordinates;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.TokenCursor;
import org.rstudio.studio.client.workbench.views.source.editors.text.cpp.CppCompletionToolTip;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.inject.Inject;

public class RCompletionToolTip extends CppCompletionToolTip
{
   public RCompletionToolTip(DocDisplay docDisplay)
   {
      // save references
      docDisplay_ = docDisplay;

      // set the max width
      setMaxWidth(Window.getClientWidth() - 200);
      
      // create an update timer
      signatureUpdater_ = makeSignatureUpdater();
      
      RStudioGinjector.INSTANCE.injectMembers(this);
   }
   
   private Timer makeSignatureUpdater()
   {
      return new Timer() {

         @Override
         public void run()
         {
            final AceEditor editor = (AceEditor) docDisplay_;
            if (editor == null)
               return;

            CodeModel codeModel = editor.getSession().getMode().getCodeModel();
            if (codeModel == null)
               return;

            TokenCursor cursor = codeModel.getTokenCursor();
            if (!cursor.moveToPosition(docDisplay_.getCursorPosition()))
               return;

            if (!cursor.findOpeningBracket("(", false))
               return;
            
            TokenCursor matchingParenCursor = cursor.cloneCursor();
            if (!matchingParenCursor.fwdToMatchingToken())
               return;
            
            setAnchor(
                  cursor.currentPosition(),
                  matchingParenCursor.currentPosition());
            
            final Position pos = cursor.currentPosition();

            if (!cursor.moveToPreviousToken())
               return;

            final String functionName = cursor.currentValue();

            server_.getArgs(
                  functionName,
                  "",
                  new ServerRequestCallback<String>() {

                     @Override
                     public void onResponseReceived(String args)
                     {
                        if (!StringUtil.isNullOrEmpty(args))
                        {
                           ScreenCoordinates coordinates =
                                 editor.getWidget()
                                 .getEditor()
                                 .getRenderer()
                                 .textToScreenCoordinates(
                                       pos.getRow(), pos.getColumn());

                           resolvePositionAndShow(
                                 functionName + args,
                                 coordinates.getPageX(),
                                 coordinates.getPageY());

                        }
                     }

                     @Override
                     public void onError(ServerError error)
                     {
                        Debug.logError(error);
                     }
                  });

         }

      };

   }
   
   @Inject
   void initialize(CodeToolsServerOperations server)
   {
      server_ = server;
   }
   
   public void previewKeyDown(NativeEvent event)
   {
      if (!isShowing())
         return;
      
      if (event.getKeyCode() == KeyCodes.KEY_ESCAPE)
      {
         hide();
         return;
      }
   }
   
   public void resolvePositionAndShow(String signature,
                                      Rectangle rectangle)
   {
      resolvePositionAndShow(
            signature,
            rectangle.getLeft(),
            rectangle.getTop());
   }
   
   public void resolvePositionAndShow(String signature,
                                      int left,
                                      int top)
   {
      if (signature != null)
         setText(signature);
      
      resolvePositionRelativeTo(left, top);
      
      setVisible(true);
      show();
      
   }
   
   public void resolvePositionAndShow(String signature)
   {
      setCursorAnchor();
      resolvePositionAndShow(signature, docDisplay_.getCursorBounds());
   }
   
   private void resolvePositionRelativeTo(final int left,
                                          final int top)
   {
      // some constants
      final int H_PAD = 12;
      final int V_PAD = -3;
      final int H_BUFFER = 100;
      final int MIN_WIDTH = 300;
      
      // do we have enough room to the right? if not then
      int roomRight = Window.getClientWidth() - left;
      int maxWidth = Math.min(roomRight - H_BUFFER, 500);
      final boolean showLeft = maxWidth < MIN_WIDTH;
      if (showLeft)
         maxWidth = left - H_BUFFER;

      setMaxWidth(maxWidth);
      setPopupPositionAndShow(new PositionCallback(){

         @Override
         public void setPosition(int offsetWidth,
                                 int offsetHeight)
         {
            // if we are showing left then adjust
            int adjustedLeft = left;
            if (showLeft)
            {
               adjustedLeft = getAbsoluteLeft() -
                     offsetWidth - H_PAD;
            }

            setPopupPosition(adjustedLeft, top - getOffsetHeight() + V_PAD);
         }
      });

   }
   
   private void setAnchor(Position start, Position end)
   {
      int startCol = start.getColumn();
      if (startCol > 0)
         start.setColumn(start.getColumn() - 1);
      
      end.setColumn(end.getColumn() + 1);
      anchor_ = docDisplay_.createAnchoredSelection(start, end);
   }
   
   private void setCursorAnchor()
   {
      Position start = docDisplay_.getSelectionStart();
      start = Position.create(start.getRow(), start.getColumn() - 1);
      Position end = docDisplay_.getSelectionEnd();
      end = Position.create(end.getRow(), end.getColumn() + 1);
      anchor_ = docDisplay_.createAnchoredSelection(start, end);
   }
   
   @Override
   protected void onUnload()
   {
      super.onUnload();
      nativePreviewReg_.removeHandler();
   }
   
   @Override
   protected void onLoad()
   {
      super.onLoad();
      nativePreviewReg_ = Event.addNativePreviewHandler(new NativePreviewHandler()
      {
         public void onPreviewNativeEvent(NativePreviewEvent e)
         {
            if (e.getTypeInt() == Event.ONKEYDOWN)
            {
               // dismiss if we've left our anchor zone
               // (defer this so the current key has a chance to 
               // enter the editor and affect the cursor)
               Scheduler.get().scheduleDeferred(new ScheduledCommand() {

                  @Override
                  public void execute()
                  {
                     Position cursorPos = docDisplay_.getCursorPosition();
                     Range anchorRange = anchor_.getRange();
                     
                     if (cursorPos.isBeforeOrEqualTo(anchorRange.getStart()) ||
                         cursorPos.isAfterOrEqualTo(anchorRange.getEnd()))
                     {
                        hide();
                     }
                     
                     signatureUpdater_.schedule(700);
                  }
               });
            }
         }
      });
   }

   private final DocDisplay docDisplay_;
   private CodeToolsServerOperations server_;

   private AnchoredSelection anchor_;
   private HandlerRegistration nativePreviewReg_;

   private final Timer signatureUpdater_;

}
