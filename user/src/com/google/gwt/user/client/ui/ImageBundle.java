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
 * The image name can optionally be specified using the
 * <code>gwt.resource</code> metadata tag. Valid image name extensions are
 * <code>png</code>, <code>gif</code>, or <code>jpg</code>. If the
 * image name contains '/' characters, it is assumed to be the name of a
 * resource on the classpath, formatted as would be expected by
 * <code>
 *  <a href="http://java.sun.com/j2se/1.4.2/docs/api/java/lang/ClassLoader.html#getResource(java.lang.String)">ClassLoader.getResource(String)</a>.
 * </code>
 * Otherwise, the image must be located in the same package as the user-defined
 * image bundle.
 * </p>
 * 
 * <p>
 * The easiest way to create an image bundle is to omit the
 * <code>gwt.resource</code> metadata tag, and name the method the same as the
 * image name, excluding the extension. When the image name is inferred in this
 * manner, the image name's extension is assumed to be either <code>png</code>,
 * <code>gif</code>, or <code>jpg</code>, and the image location must be
 * in the same package as the user-defined image bundle. In the event that there
 * are multiple image files that have the same name with different extensions,
 * the order of extension precedence is <code>png</code>, <code>gif</code>,
 * <code>jpg</code>.
 * 
 * <h3>Example</h3>
 * 
 * <pre class="code">
 * public interface MyImageBundle extends ImageBundle {
 *
 *   /**
 *    * Notice that the gwt.resource metadata tag is not present, 
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
 * An image bundle that uses the <code>gwt.resource</code> metadata tag to
 * specify image names might look something like this:
 * 
 * <pre class="code">
 * public interface MyImageBundle extends ImageBundle {
 *
 *   /**
 *    * The metadata tag contains no '/' characters, so 
 *    * btn_submit_icon.gif must be located in the same 
 *    * package as MyImageBundle.
 *    *
 *    * @gwt.resource btn_submit_icon.gif
 *    *&#47;
 *   public AbstractImagePrototype submitButtonIcon();
 *
 *   /**
 *    * btn_cancel_icon.png must be located in the package 
 *    * com.mycompany.myapp.icons (which must be on the classpath).
 *    *
 *    * @gwt.resource com/mycompany/myapp/icons/btn_cancel_icon.png
 *    *&#47;
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
 *  MyImageBundle myImageBundle = (MyImageBundle) GWT.create(MyImageBundle.class);
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
 * <h3>Caching Recommendations for Image Bundle Files</h3>
 * Since the filename for the image bundle's composite image is based on a hash
 * of the file's contents, the server can tell the browser to cache the file
 * permanently.
 *
 * <p>
 * To make all image bundle files permanently cacheable, set up a rule in your
 * web server to emit the <code>Expires</code> response header for any files
 * ending with <code>".cache.*"</code>. Such a rule would automatically match
 * generated image bundle filenames
 * (e.g. <code>320ADF600D31858000C612E939F0AD1A.cache.png</code>).
 * The HTTP/1.1 specification recommends specifying date of approximately one
 * year in the future for the <code>Expires</code> header to indicate that the
 * resource is permanently cacheable.
 * </p>
 *
 * <h3>Image Bundles and the HTTPS Protocol</h3>
 * There is an issue with displaying image bundle images in Internet Explorer
 * when:
 *
 * <ul>
 *  <li>The image bundle's composite image is requested using the HTTPS protocol, and</li>
 *  <li>The web application has a security constraint set for the composite image</li>
 * </ul>
 *
 * This issue is known to occur with the web application servers Tomcat and
 * Glassfish. It stems from the way in which Internet Explorer renders
 * the composite image, and the caching headers that are set by Tomcat and
 * Glassfish when the HTTPS protocol is used to request a resource protected
 * by a security constraint. 
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
 * must be cached by the brower. That way, the plugin can read the cached file
 * from the disk. Whenever the composite image has a security constraint specified
 * by the web application's configuration and is requested using the HTTPS protocol,
 * both Tomcat and Glassfish automatically append the HTTP headers
 * <code>Pragma: No-cache</code> and <code>Cache-Control: no-cache</code>
 * to the response.
 * </p>
 *
 * <p>
 * When the browser recieves the response, it does not cache the composite image.
 * Since the composite image is not stored on disk, the plugin is unable to render
 * it, and all of the images in the application which rely on the composite image
 * will not be displayed.
 * </p>
 *
 * <p>
 *
 * There are several ways to work around this issue:
 *
 * <ol>
 *  <li>
 *    Add a servlet filter which overrides the <code>Pragma</code> and
 *    <code>Cache-Control</code> headers for <code>png</code> files. These
 *    headers should either be removed, or set in such a way that the file will
 *    be cached by the browser.
 *  </li>
 *  <li>
 *    Use the <code>securePagesWithPragma</code> (works in Glassfish and Tomcat)
 *    or <code>disableProxyCaching</code> settings (works in Tomcat) in your
 *    web application configuration file. Refer to your web application
 *    server's documentation for specific instructions.
 *  </li>
 *  <li>
 *    Exclude the image bundle's composite image from the web application's
 *    security constraint.
 *  </li>
 *  <li>
 *    If there is sensitive data in any of the images in the image bundle,
 *    exclude that image from the bundle and include it in the web application's
 *    security constraint. Then, rebuild the image bundle, and exclude the updated
 *    bundle's composite image from the security constraint.
 *  </li>
 * </ol>
 *
 * </p>
 *
 * <h3>For More Information</h3>
 * See the GWT Developer Guide for an introduction to image bundles.
 * @see com.google.gwt.user.client.ui.AbstractImagePrototype
 * @see com.google.gwt.user.client.ui.Image#Image(String, int, int, int, int)
 * @see com.google.gwt.user.client.ui.Image#setVisibleRect(int, int, int, int)
 * @see com.google.gwt.user.client.ui.Image#setUrlAndVisibleRect(String, int,
 *      int, int, int)
 */
public interface ImageBundle {
}
