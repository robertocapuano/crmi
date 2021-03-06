/* 
 * Copyright (c) 2014, Roberto Capuano <roberto@2think.it> 
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package clio;

import java.lang.reflect.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLStreamHandlerFactory;
import java.net.URLStreamHandler;
import java.net.MalformedURLException;


class ClasspathHelper
{
    /**
     * The stream handler factory for loading system protocol handlers.
     */
    private static class Factory implements URLStreamHandlerFactory {
	private static String PREFIX = "sun.net.www.protocol";

	public URLStreamHandler createURLStreamHandler(String protocol) {
	    String name = PREFIX + "." + protocol + ".Handler";
	    try {
		Class c = Class.forName(name);
		return (URLStreamHandler)c.newInstance();
	    } catch (ClassNotFoundException e) {
		e.printStackTrace();
	    } catch (InstantiationException e) {
		e.printStackTrace();
	    } catch (IllegalAccessException e) {
		e.printStackTrace();
	    }
	    throw new InternalError("could not load " + protocol +
				    "system protocol handler");
	}
    }


  private static URLStreamHandler fileHandler;
  private static URLStreamHandlerFactory factory;

  static
  {
    factory = new Factory();
  }

  static File[] getClassPath(String cp) {
      File[] path;
      if (cp != null) {
	  int count = 0, maxCount = 1;
	  int pos = 0, lastPos = 0;
	  // Count the number of separators first
	  while ((pos = cp.indexOf(File.pathSeparator, lastPos)) != -1) {
	      maxCount++;
	      lastPos = pos + 1;
	  }
	  path = new File[maxCount];
	  lastPos = pos = 0;
	  // Now scan for each path component
	  while ((pos = cp.indexOf(File.pathSeparator, lastPos)) != -1) {
	      if (pos - lastPos > 0) {
		  path[count++] = new File(cp.substring(lastPos, pos));
	      } else {
		  // empty path component translates to "."
		  path[count++] = new File(".");
	      }
	      lastPos = pos + 1;
	  }
	  // Make sure we include the last path component
	  if (lastPos < cp.length()) {
	      path[count++] = new File(cp.substring(lastPos));
	  } else {
	      path[count++] = new File(".");
	  }
	  // Trim array to correct size
	  if (count != maxCount) {
	      File[] tmp = new File[count];
	      System.arraycopy(path, 0, tmp, 0, count);
	      path = tmp;
	  }
      } else {
	  path = new File[0];
      }
      // DEBUG
      for (int i = 0; i < path.length; i++)
      {
	System.out.println( "path[" + i + "] = " + '"' + path[i] + '"');
      }
      return path;
  }

  static URL[] pathToURLs(File[] path) {
      URL[] urls = new URL[path.length];
      for (int i = 0; i < path.length; i++) {
	  urls[i] = getFileURL(path[i]);
      }
      // DEBUG
      for (int i = 0; i < urls.length; i++) {
	System.out.println("urls[" + i + "] = " + '"' + urls[i] + '"');
      }
      return urls;
  }

    private static URL getFileURL(File file) {
	try {
	    file = file.getCanonicalFile();
	} catch (IOException e) {
	}
	String path = file.getAbsolutePath();
	if (File.separatorChar != '/') {
	    path = path.replace(File.separatorChar, '/');
	}
	if (!path.startsWith("/")) {
	    path = "/" + path;
	}
	if (!path.endsWith("/") && file.isDirectory()) {
	    path = path + "/";
	}
	if (fileHandler == null) {
	    fileHandler = factory.createURLStreamHandler("file");
	}
	try {
	    return new URL("file", "", -1, path, fileHandler);
	} catch (MalformedURLException e) {
	    // Should never happen since we specify the protocol...
	    throw new InternalError();
	}
    }


}