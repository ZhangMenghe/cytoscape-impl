/*
  File: OpenBrowserImpl.java

  Copyright (c) 2006, The Cytoscape Consortium (www.cytoscape.org)

  The Cytoscape Consortium is:
  - Institute for Systems Biology
  - University of California San Diego
  - Memorial Sloan-Kettering Cancer Center
  - Institut Pasteur
  - Agilent Technologies

  This library is free software; you can redistribute it and/or modify it
  under the terms of the GNU Lesser General Public License as published
  by the Free Software Foundation; either version 2.1 of the License, or
  any later version.

  This library is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
  MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
  documentation provided hereunder is on an "as is" basis, and the
  Institute for Systems Biology and the Whitehead Institute
  have no obligations to provide maintenance, support,
  updates, enhancements or modifications.  In no event shall the
  Institute for Systems Biology and the Whitehead Institute
  be liable to any party for direct, indirect, special,
  incidental or consequential damages, including lost profits, arising
  out of the use of this software and its documentation, even if the
  Institute for Systems Biology and the Whitehead Institute
  have been advised of the possibility of such damage.  See
  the GNU Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public License
  along with this library; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 */

//-------------------------------------------------------------------------
// $Revision: 13206 $
// $Date: 2008-02-26 16:37:29 -0800 (Tue, 26 Feb 2008) $
// $Author: kono $
//-------------------------------------------------------------------------
package org.cytoscape.util.swing.internal;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.JOptionPane;

import org.cytoscape.util.swing.OpenBrowser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenBrowserImpl implements OpenBrowser {

	private final Logger logger = LoggerFactory.getLogger(OpenBrowserImpl.class);
	private static String[] BROWSERS =
        { "xdg-open", "htmlview", "firefox", "mozilla", "konqueror", "chrome", "chromium" };

	/**
	 * Opens the specified URL in the system default web browser.
	 *
	 * @return true if the URL opens successfully.
	 */
	@Override
	public boolean openURL(final String url) {
		try {
			URI uri = new URI(url);
			if(Desktop.isDesktopSupported()) {
				final Desktop desktop = Desktop.getDesktop();
				desktop.browse(uri);
			}
			else { //fallback if desktop API not supported
				for (final String browser : BROWSERS) {
					String cmd = browser + " " + url;
					final Process p = Runtime.getRuntime().exec(cmd);
					if(p.waitFor() == 0)
						break;
				}
			}
		} catch (IOException ioe) {
			JOptionPane.showInputDialog(null, "There was an error while attempting to open the system browser. "
					+ "\nPlease copy and paste the following URL into your browser:", url);
			logger.info("Error opening system browser; displaying copyable link instead");
		} catch (URISyntaxException e) {
			logger.warn("This URI is invalid: " + url, e);
			return false;
		} catch (InterruptedException e) {
			logger.warn("Browser process thread interrupted");
		}
		return true;
	}
}
