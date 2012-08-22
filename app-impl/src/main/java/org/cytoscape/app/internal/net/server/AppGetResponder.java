package org.cytoscape.app.internal.net.server;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.cytoscape.app.internal.exception.AppDownloadException;
import org.cytoscape.app.internal.exception.AppInstallException;
import org.cytoscape.app.internal.exception.AppParsingException;
import org.cytoscape.app.internal.manager.App;
import org.cytoscape.app.internal.manager.AppManager;
import org.cytoscape.app.internal.net.WebApp;
import org.cytoscape.app.internal.util.DebugHelper;
import org.json.JSONObject;

/**
 * This class is responsible for handling GET requests received by the local HTTP server.
 */
public class AppGetResponder {
    private static final CyHttpResponseFactory responseFactory = new CyHttpResponseFactoryImpl();

	private AppManager appManager;
	
	public AppGetResponder(AppManager appManager) {
		this.appManager = appManager;
	}

    private abstract static class JsonResponder implements CyHttpResponder {
        protected abstract Map<String,String> jsonRespond(CyHttpRequest request, Matcher matchedURI);

        public CyHttpResponse respond(CyHttpRequest request, Matcher matchedURI) {
            final Map<String,String> responseData = jsonRespond(request, matchedURI);
            JSONObject jsonObject = new JSONObject(responseData);
            return responseFactory.createHttpResponse(jsonObject.toString(), "application/json");
        }
    }

    public class StatusResponder extends JsonResponder {
        final Pattern pattern = Pattern.compile("^/status/(.*)$");

        public Pattern getURIPattern() {
            return pattern;
        }

        protected Map<String,String> jsonRespond(CyHttpRequest request, Matcher matchedURI) {
            Map<String, String> responseData = new HashMap<String, String>();
            String appName = matchedURI.group(1);
            if (appName != null && appName.length() != 0) {
                String status = "not-found";
                String version = "not-found";

                // Searches web apps first. If not found, searches other apps using manifest name field.
                for (App app : appManager.getApps()) {
                    if (app.getAppName().equalsIgnoreCase(appName)) {
                        if (app.getStatus() != null) {
                            status = app.getStatus().toString().toLowerCase();
                        }

                        if (app.getVersion() != null) {
                            version = app.getVersion();
                        }
                    }
                }

                responseData.put("request_name", appName); // web unique identifier
                responseData.put("status", status);
                responseData.put("version", version);
            }
            return responseData;
        }
    }

    public class InstallResponder extends JsonResponder {
        final Pattern pattern = Pattern.compile("^/install/(.+)/(.+)$");

        public Pattern getURIPattern() {
            return pattern;
        }

        protected Map<String,String> jsonRespond(CyHttpRequest request, Matcher matchedURI) {
            Map<String, String> responseData = new HashMap<String, String>();
			String appName = matchedURI.group(1);
			String version = matchedURI.group(2);
			
			if (appName != null && appName.length() != 0 && version != null && version.length() != 0) {
				// Use the WebQuerier to obtain the app from the app store using the app name and version
				//responseBody = "Will obtain \"" + appName + "\", version " + version;

				String installStatus = "app-not-found";
				String installError = "";
				boolean appFoundInStore = false;
				WebApp appToDownload = null;
				
				// Check if the app is available on the app store
				// TODO: Use a web query to do this?
				
				for (WebApp webApp : appManager.getWebQuerier().getAllApps()) {
					if (webApp.getName().equals(appName)) {
						appFoundInStore = true;
						appToDownload = webApp;
						break;
					}
				}
				responseData.put("name", appName);
				
				if (appFoundInStore) {
					
					// Download app
					File appFile = null;
					try {
						appFile = appManager.getWebQuerier().downloadApp(
							appToDownload, version, new File(appManager.getDownloadedAppsPath()));
					} catch (AppDownloadException e) {
					}
					
					// Attempt to install app
					if (appFile == null) {
						installStatus = "version-not-found";
						installError = "An entry for the app " + appName + " with version " + version
							+ " was not found in the app store database at: " + appManager.getWebQuerier().getDefaultAppStoreUrl();
					} else {
						installStatus = "success";
						
						try {
							App app = appManager.getAppParser().parseApp(appFile);
							
							appManager.installApp(app);
						} catch (AppParsingException e) {
							installStatus = "install-failed";
							installError = "The installation could not be completed because there were errors in the app file. "
								+ "Details: " + e.getMessage();
						} catch (AppInstallException e) {
							installStatus = "install-failed";
							installError = "The app file passed checking, but the app manager encountered errors while attempting" 
								+ "install. Details: " + e.getMessage();
						}
					}
				} else {
					installStatus = "app-not-found";
					installError = "The app " + appName + " is not found in the app store database at "
						+ appManager.getWebQuerier().getDefaultAppStoreUrl();
				}
				
				responseData.put("install_status", installStatus);
				responseData.put("install_error", installError);
            }
            return responseData;
        }
    }
}
