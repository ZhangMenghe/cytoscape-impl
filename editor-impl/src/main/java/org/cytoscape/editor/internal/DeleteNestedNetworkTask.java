package org.cytoscape.editor.internal;

import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.task.AbstractNodeViewTask;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.work.TaskMonitor;

public class DeleteNestedNetworkTask extends AbstractNodeViewTask {

	private final VisualMappingManager vmMgr;
	
	public DeleteNestedNetworkTask(final View<CyNode> nv,
								   final CyNetworkView view,
								   final CyNetworkManager mgr,
								   final VisualMappingManager vmMgr) {
		super(nv,view);
		this.vmMgr = vmMgr;
	}

	@Override
	public void run(TaskMonitor tm) throws Exception {
		final CyNode node = nodeView.getModel();
		node.setNetworkPointer(null);
		
		final VisualStyle style = vmMgr.getVisualStyle(netView);
		style.apply(netView);
		netView.updateView();
	}
}
