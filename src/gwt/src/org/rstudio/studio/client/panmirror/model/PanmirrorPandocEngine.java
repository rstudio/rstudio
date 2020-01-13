package org.rstudio.studio.client.panmirror.model;

import org.rstudio.core.client.promise.PromiseServerRequestCallback;
import org.rstudio.studio.client.RStudioGinjector;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.inject.Inject;

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
