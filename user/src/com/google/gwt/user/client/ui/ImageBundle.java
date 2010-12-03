/*
 * Copyright 2007 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.user.client.ui;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A tag interface that is used in the generation of image bundles. An image
 * bundle is a composition of multiple images into a single large image, along
 * with an interface for accessing a specific image's
 * {@link com.google.gwt.user.client.ui.AbstractImagePrototype prototype} from
 * within the composition. Obtain an image bundle instance by calling
 * <code>GWT.create(<i>T</i>)</code>, where <code>T</code> is an
 * interface that directly or indirectly extends <code>ImageBundle</code>.
 * 
 * <p>
 * To create and use an image bundle, extend the <code>ImageBundle</code>
 * interface, and add a method declaration for each image that is to be part of
 * the bundle. Each method must take no parameters and must have a return type
 * of
 * {@link com.google.gwt.user.client.ui.AbstractImagePrototype AbstractImagePrototype}.
 * The image name can optionally be specified using the {@link Resource}
 * annotation. (Note that the <code>gwt.resource</code> javadoc metadata tag
 * supporting in GWT 1.4 has been superceded by the <code>Resource</code>
 * annotation.) Valid image name extensions are <code>png</code>,
 * <code>gif</code>, or <code>jpg</code>. If the image name contains '/'
 * characters, it is assumed to be the name of a resource on the classpath,
 * formatted as would be expected by <code>
 *  <a href="http://download.oracle.com/javase/1.5.0/docs/api/java/lang/ClassLoader.html#getResource(java.lang.String)">ClassLoader.getResource(String)</a>.
 * </code>
 * Otherwise, the image must be located in the same package as the user-defined
 * image bundle.
 * </p>
 * 
 * <p>
 * The easiest way to create an image bundle is to omit the {@link Resource}
 * annotation, and name the method the same as the image name, excluding the
 * extension. When the image name is inferred in this manner, the image name's
 * extension is assumed to be either <code>png</code>, <code>gif</code>,
 * or <code>jpg</code>, and the image location must be in the same package as
 * the user-defined image bundle. In the event that there are multiple image
 * files that have the same name with different extensions, the order of
 * extension precedence is <code>png</code>, <code>gif</code>,
 * <code>jpg</code>.
 * 
 * <h3>Example</h3>
 * 
 * <pre class="code">
 * public interface MyImageBundle extends ImageBundle {
 *
 *   /**
 *    * Notice that the Resource annotation is not present, 
 *    * so the method name itself is assumed to match the associated 
 *    * image filename.
 *    *
 *    * One of btn_submit_icon.png, btn_submit_icon.gif, or 
 *    * btn_submit_icon.jpg must be located in the same package 
 *    * as MyImageBundle.
 *    *&#47; 
 *   public AbstractImagePrototype btn_submit_icon();
 *
 *   // No doc comment is required if you want the default 
 *   // name-matching behavior.
 *   public AbstractImagePrototype cancelButtonIcon();
 * }
 * </pre>
 * 
 * </p>
 * 
 * <p>
 * An image bundle that uses the <code>Resource</code> annotation to specify
 * image names might look something like this:
 * 
 * <pre class="code">
 * public interface MyImageBundle extends ImageBundle {
 *
 *   /**
 *    * The resource annotation contains no '/' characters, so 
 *    * btn_submit_icon.gif must be located in the same 
 *    * package as MyImageBundle.
 *    *&#47;
 *   {@code @Resource("btn_submit_icon.gif")}
 *   public AbstractImagePrototype submitButtonIcon();
 *
 *   /**
 *    * btn_cancel_icon.png must be located in the package 
 *    * com.mycompany.myapp.icons (which must be on the classpath).
 *    *&#47;
 *   {@code @Resource("com/mycompany/myapp/icons/btn_cancel_icon.png")}
 *   public AbstractImagePrototype cancelButtonIcon();
 * }
 * </pre>
 * 
 * </p>
 * 
 * <p>
 * Here is how MyImageBundle might be used in an application:
 * 
 * <pre class="code">
 *  ...
 *
 *  // Create a new instance of MyImageBundle using GWT.create.
 *  // This only needs to be done once - a reference to myImageBundle can
 *  // be kept for use by other parts of the application.
 *  MyImageBundle myImageBundle = GWT.create(MyImageBundle.class);
 *
 *  // Retrieve the image prototypes from myImageBundle.
 *  AbstractImagePrototype submitButtonImgPrototype = myImageBundle.btn_submit_icon();
 *  AbstractImagePrototype cancelButtonImgPrototype = myImageBundle.cancelButtonIcon();
 *
 *  // Add the images that are created based on the prototypes to the panel.
 *  panel.add(submitButtonImgPrototype.createImage());
 *  panel.add(cancelButtonImgPrototype.createImage());
 *
 * ...
 * </pre>
 * 
 * </p>
 * 
 * <h3>Security Warning: Image Bundle's use of the javax.image.imageio Classes</h3>
 * Certain versions of the JVM are susceptible to a vulnerability in the
 * javax.image.imageio classes, which are generally used to parse images. These
 * classes are used by image bundle's implementation to combine all of the
 * images into a single composite image.
 * 
 * <p>
 * It is possible that the vulnerability could be exploited by using a specially
 * crafted image as part of an image bundle. To prevent this type of attack from
 * occurring, use a version of the JVM that includes a fix for this
 * vulnerability. See the following link for more information:
 * </p>
 * 
 * <pre>
 * <a href="http://sunsolve.sun.com/search/document.do?assetkey=1-26-102934-1">http://sunsolve.sun.com/search/document.do?assetkey=1-26-102934-1</a>
 * </pre>
 * 
 * <p>
 * Alternatively, if the images to be used in the bundle are trusted, then it is
 * not necessary to upgrade the JVM.
 * </p>
 * 
 * <h3>Caching Recommendations for Image Bundle Files</h3>
 * Since the filename for the image bundle's composite image is based on a hash
 * of the file's contents, the server can tell the browser to cache the file
 * permanently.
 * 
 * <p>
 * To make all image bundle files permanently cacheable, set up a rule in your
 * web server to emit the <code>Expires</code> response header for any files
 * ending with <code>".cache.*"</code>. Such a rule would automatically match
 * generated image bundle filenames (e.g.
 * <code>320ADF600D31858000C612E939F0AD1A.cache.png</code>). The HTTP/1.1
 * specification recommends specifying date of approximately one year in the
 * future for the <code>Expires</code> header to indicate that the resource is
 * permanently cacheable.
 * </p>
 * 
 * <h3>Using Security Constraints to Protect Image Bundle Files</h3>
 * When a web application has a security constraint set for the composite image,
 * web application servers may change the image's HTTP response headers so that
 * web browsers will not cache it. For example, Tomcat and Glassfish set the
 * HTTP response headers <code>Pragma: No-cache</code>,
 * <code>Cache-Control: None</code>, and
 * <code>Expires: Thu, 1 Jan 1970 00:00:00</code> (or some other date in the
 * past).
 * 
 * <p>
 * This can lead to performance problems when using image bundles, because the
 * large composite image will be re-requested unnecessarily. In addition,
 * <code>clear.cache.gif</code>, which is a blank image used by the image
 * bundle implementation, will be re-requested as well. While some browsers will
 * only re-request these images for each page load, others will re-request them
 * for each image on the page that is part of an image bundle.
 * </p>
 * 
 * There are several ways to work around this issue:
 * 
 * <ol>
 * <li> Modify the servlet which serves <code>png</code> and <code>gif</code>
 * files so that it explicitly sets the <code>Pragma</code>,
 * <code>Cache-Control</code>, and <code>Expires</code> headers. The
 * <code>Pragma</code> and <code>Cache-Control</code> headers should be
 * removed. The <code>Expires</code> header should be set according to the
 * caching recommendations mentioned in the previous section. </li>
 * <li> If using Tomcat, use the <code>disableProxyCaching</code> property in
 * your web application configuration file to prevent the <code>Pragma</code>,
 * <code>Cache-Control</code>, and <code>Expires</code> headers from being
 * changed by the server. Refer to your web application server's documentation
 * for more information. </li>
 * <li> Exclude the image bundle's composite image from the web application's
 * security constraint. </li>
 * <li> If there is sensitive data in any of the images in the image bundle,
 * exclude that image from the bundle and include it in the web application's
 * security constraint. Then, rebuild the image bundle, and exclude the updated
 * bundle's composite image from the security constraint. </li>
 * </ol>
 * 
 * <h3>Image Bundles and the HTTPS Protocol</h3>
 * There is an issue with displaying image bundle images in Internet Explorer
 * when:
 * 
 * <ul>
 * <li>The image bundle's composite image is requested using the HTTPS
 * protocol, and</li>
 * <li>The web application has a security constraint set for the composite
 * image</li>
 * </ul>
 * 
 * This issue is known to occur with the web application servers Tomcat and
 * Glassfish.
 * 
 * <p>
 * The native format for the composite image is <code>png</code>, and
 * versions of Internet Explorer prior to 7 cannot render <code>png</code>
 * transparerency. To get around this problem, we make use of a plugin built
 * into the operating system.
 * </p>
 * 
 * <p>
 * Internet Explorer specifies that files which require a plugin for viewing
 * must be cached by the browser. That way, the plugin can read the cached file
 * from the disk. Whenever the composite image is protected by a security
 * constraint, the web application server sets caching headers on the response
 * to prevent the browser from caching the image (see the previous section for
 * details).
 * </p>
 * 
 * <p>
 * When using the HTTP protocol, Internet Explorer will disregard the
 * <code>Pragma: No-cache</code> and <code>Cache-Control: None</code>
 * headers, and will cache the image. However, When using the HTTPS protocol,
 * Internet Explorer will enforce these headers, and will not cache the image.
 * Since the composite image is not stored on disk, the plugin is unable to
 * render it, and all of the images in the application which rely on the
 * composite image will not be displayed.
 * </p>
 * 
 * <p>
 * To work around this issue, follow the recommendations outlined in the
 * previous section.
 * </p>
 * 
 * <h3>For More Information</h3>
 * See the GWT Developer Guide for an introduction to image bundles.
 * @see com.google.gwt.user.client.ui.AbstractImagePrototype
 * @see com.google.gwt.user.client.ui.Image#Image(String, int, int, int, int)
 * @see com.google.gwt.user.client.ui.Image#setVisibleRect(int, int, int, int)
 * @see com.google.gwt.user.client.ui.Image#setUrlAndVisibleRect(String, int,
 *      int, int, int)
 * @deprecated replaced by {@link com.google.gwt.resources.client.ClientBundle}
 *             and {@link com.google.gwt.resources.client.ImageResource}
 */
@Deprecated
public interface ImageBundle {

  /**
   * Explicitly specifies a file name or path to the image resource to be
   * associated with a method in an {@link ImageBundle}. If the path is
   * unqualified (that is, if it contains no slashes), then it is sought in the
   * package enclosing the image bundle to which the annotation is attached. If
   * the path is qualified, then it is expected that the string can be passed
   * verbatim to <code>ClassLoader.getResource()</code>.
   */
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Documented
  public @interface Resource {
    String value();
  }

}
