package org.cytoscape.internal.view;

import java.util.Map;

import javax.swing.JPanel;


public class SearchPanelListener {
	
	private static final String PANEL_ID = "searchPanel";
	
	private final NetworkMainPanel mainPanel;
	
	
	public SearchPanelListener(final NetworkMainPanel mainPanel) {
		this.mainPanel = mainPanel;
	}

	public final void registerPanel(final JPanel panel, final Map<?, ?> metadata) {
		final Object serviceId = metadata.get("id");
		if(serviceId == null) {
			return;
		}
		
		if(serviceId.toString().equals(PANEL_ID)) {
			mainPanel.injectSearchPanel(panel);
		}
		
	}

	public final void unregisterPanel(final JPanel panel, final Map<?, ?> metadata) {
		final Object serviceId = metadata.get("id");
		if(serviceId == null) {
			return;
		}
		
		if(serviceId.toString().equals(PANEL_ID)) {
			mainPanel.injectSearchPanel(null);
		}
	}
}
