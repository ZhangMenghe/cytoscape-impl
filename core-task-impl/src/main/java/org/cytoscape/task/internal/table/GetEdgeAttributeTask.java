package org.cytoscape.task.internal.table;

/*
 * #%L
 * Cytoscape Core Task Impl (core-task-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2006 - 2013 The Cytoscape Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

import java.util.Map;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableManager;
import org.cytoscape.task.internal.utils.EdgeTunable;
import org.cytoscape.task.internal.utils.ColumnTunable;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.ContainsTunables;

public class GetEdgeAttributeTask extends AbstractGetTableDataTask implements ObservableTask {
	final CyApplicationManager appMgr;
	Map<CyIdentifiable, Map<String, Object>> edgeData;

	@ContainsTunables
	public EdgeTunable edgeTunable;

	@ContainsTunables
	public ColumnTunable columnTunable;

	public GetEdgeAttributeTask(CyTableManager mgr, CyApplicationManager appMgr) {
		super(mgr);
		this.appMgr = appMgr;
		edgeTunable = new EdgeTunable(appMgr);
		columnTunable = new ColumnTunable();
	}

	@Override
	public void run(final TaskMonitor taskMonitor) {
		CyNetwork network = edgeTunable.getNetwork();

		CyTable edgeTable = getNetworkTable(network, CyEdge.class, columnTunable.getNamespace());

		edgeData = getCyIdentifierData(edgeTable, 
		                               edgeTunable.getEdgeList(),
		                               columnTunable.getColumnNames(edgeTable));

		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Edge attribute Data for network "+getNetworkTitle(network)+":");
		for (CyIdentifiable id: edgeData.keySet()) {
			taskMonitor.showMessage(TaskMonitor.Level.INFO, 
				"   Edge: "+edgeTable.getRow(id.getSUID()).get(CyNetwork.NAME, String.class)+" suid: "+id.getSUID());
			Map<String, Object> dataMap = edgeData.get(id);
			for (String column: dataMap.keySet()) {
				if (dataMap.get(column) != null)
					taskMonitor.showMessage(TaskMonitor.Level.INFO, "     "+column+"="+convertData(dataMap.get(column)));
			}
		}
	}

	public Object getResults(Class requestedType) {
		if (requestedType.equals(String.class)) {
			return convertMapToString(edgeData);
		}
		return edgeData;
	}
	
}
