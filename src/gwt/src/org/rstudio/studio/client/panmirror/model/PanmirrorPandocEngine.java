package org.rstudio.studio.client.panmirror.model;

import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.inject.Inject;

import elemental2.core.JsError;
import elemental2.promise.Promise;
import elemental2.promise.Promise.PromiseExecutorCallbackFn.RejectCallbackFn;
import elemental2.promise.Promise.PromiseExecutorCallbackFn.ResolveCallbackFn;

import jsinterop.annotations.JsType;


@JsType
public class PanmirrorPandocEngine {
   
  
   public PanmirrorPandocEngine() {
      RStudioGinjector.INSTANCE.injectMembers(this);
   }
   
   @Inject
   void initialize(PanmirrorServerOperations server)
   {
      server_ = server;
   }

   public Promise<JavaScriptObject> markdownToAst(String markdown, String format, JsArrayString options)
   {
      return new Promise<JavaScriptObject>((ResolveCallbackFn<JavaScriptObject> resolve, RejectCallbackFn reject) -> {
         server_.pandocMarkdownToAst(
            markdown, format, options, 
            new PromiseServerRequestCallback<JavaScriptObject>(resolve, reject)
         );
      });
   }
   
   public Promise<String> astToMarkdown(JavaScriptObject ast, String format, JsArrayString options)
   {
      return new Promise<String>((ResolveCallbackFn<String> resolve, RejectCallbackFn reject) -> {
         server_.pandocAstToMarkdown(
            ast, format, options, 
            new PromiseServerRequestCallback<String>(resolve, reject)
         );
      });
   }

   public Promise<String> listExtensions(String format)
   {
      return new Promise<String>((ResolveCallbackFn<String> resolve, RejectCallbackFn reject) -> {
         server_.pandocListExtensions(format, new PromiseServerRequestCallback<String>(resolve, reject));
      });
   }

   private PanmirrorServerOperations server_;
   
}

class PromiseServerRequestCallback<T> extends ServerRequestCallback<T> {

   public PromiseServerRequestCallback(ResolveCallbackFn<T> resolve, RejectCallbackFn reject) {
      this.resolve_ = resolve;
      this.reject_ = reject;
   }
   
   @Override
   public void onResponseReceived(T response)
   {
      resolve_.onInvoke(response);
   }
   
   @Override
   public void onError(ServerError error)
   {
      reject_.onInvoke(new JsError(error.getMessage())); 
   }
   
   
   private ResolveCallbackFn<T> resolve_;
   private RejectCallbackFn reject_;
}