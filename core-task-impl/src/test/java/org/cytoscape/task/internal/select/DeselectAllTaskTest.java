package org.cytoscape.task.internal.select;

/*
 * #%L
 * Cytoscape Core Task Impl (core-task-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2010 - 2013 The Cytoscape Consortium
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


import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;


import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.work.Task;
import org.cytoscape.work.undo.UndoSupport;
import org.junit.Before;
import org.junit.Test;


public class DeselectAllTaskTest extends AbstractSelectTaskTester {
	@Before
	public void setUp() throws Exception {
		super.setUp();
	}

	@Test
	public void testRun() throws Exception {
		final CyTable nodeTable = mock(CyTable.class);
		when(net.getDefaultNodeTable()).thenReturn(nodeTable);
		final CyTable edgeTable = mock(CyTable.class);
		when(net.getDefaultEdgeTable()).thenReturn(edgeTable);
		UndoSupport undoSupport = mock(UndoSupport.class);

		Set<CyRow> selectedEdges = new HashSet<CyRow>();
		selectedEdges.add(r1);
		selectedEdges.add(r2);
		when(edgeTable.getMatchingRows(CyNetwork.SELECTED, true)).thenReturn(selectedEdges);
		
		when (r1.get(CyNetwork.SUID, Long.class)).thenReturn(1L);
		when (net.getEdge(1L)).thenReturn(e1);
		when (r2.get(CyNetwork.SUID, Long.class)).thenReturn(2L);
		when (net.getEdge(2L)).thenReturn(e2);

		Set<CyRow> selectedNodes = new HashSet<CyRow>();
		selectedNodes.add(r3);
		selectedNodes.add(r4);
		when(nodeTable.getMatchingRows(CyNetwork.SELECTED, true)).thenReturn(selectedNodes);
		
		when (r3.get(CyNetwork.SUID, Long.class)).thenReturn(3L);
		when (net.getNode(3L)).thenReturn(e3);
		when (r4.get(CyNetwork.SUID, Long.class)).thenReturn(4L);
		when (net.getNode(4L)).thenReturn(e4);

		// run the task
		Task t = new DeselectAllTask(undoSupport, net, networkViewManager, eventHelper);
		t.run(tm);

		// check that the expected rows were set
		verify(r1, times(1)).set("selected", false);
		verify(r2, times(1)).set("selected", false);
		verify(r3, times(1)).set("selected", false);
		verify(r4, times(1)).set("selected", false);
	}
}
