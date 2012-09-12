package org.cytoscape.session;

import static org.cytoscape.model.CyNetwork.DEFAULT_ATTRS;
import static org.cytoscape.model.CyNetwork.HIDDEN_ATTRS;
import static org.cytoscape.model.CyNetwork.LOCAL_ATTRS;
import static org.cytoscape.model.CyNetwork.NAME;
import static org.cytoscape.model.CyNetwork.SELECTED;
import static org.cytoscape.model.subnetwork.CyRootNetwork.SHARED_ATTRS;
import static org.cytoscape.model.subnetwork.CyRootNetwork.SHARED_DEFAULT_ATTRS;
import static org.junit.Assert.*;

import java.awt.Color;
import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.SavePolicy;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.NodeShapeVisualProperty;
import org.cytoscape.view.vizmap.VisualMappingFunction;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.mappings.PassthroughMapping;
import org.cytoscape.work.TaskIterator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class Cy3SimpleSessionLodingTest extends BasicIntegrationTest {

	private static final int NODE_COUNT = 3;
	private static final int EDGE_COUNT = 2;

	@Before
	public void setup() throws Exception {
		sessionFile = new File("./src/test/resources/testData/session3x/", "simpleSession.cys");
		checkBasicConfiguration();
	}

	@Test
	public void testLoadSession() throws Exception {
		final TaskIterator ti = openSessionTF.createTaskIterator(sessionFile);
		tm.execute(ti);
		confirm();
	}

	private void confirm() {
		// test overall status of current session.
		checkGlobalStatus();
		
		Set<CyNetwork> networks = networkManager.getNetworkSet();
		final Iterator<CyNetwork> itr = networks.iterator();
		CyNetwork net = itr.next();
		
		checkNetwork(net);
		checkRootNetwork(((CySubNetwork) net).getRootNetwork());
	}
	
	private void checkGlobalStatus() {
		assertEquals(1, networkManager.getNetworkSet().size());
		assertEquals(1, viewManager.getNetworkViewSet().size());
		// Since this test runs in headless mode, this should be zero.
		assertEquals(0, renderingEngineManager.getAllRenderingEngines().size());
		// 3 public tables per subnetwork
		assertEquals(3, tableManager.getAllTables(false).size());
		// 6 total tables per network
		// TODO why not root-network tables?
		final int totalNet = networkTableManager.getNetworkSet().size();
		assertTrue(totalNet >= 2); // At least root+base-network; there can be other (private) networks
		assertEquals(6 * totalNet, tableManager.getAllTables(true).size());
//		assertEquals(totalNet, tableManager.getLocalTables(CyNetwork.class).size());
//		assertEquals(totalNet, tableManager.getLocalTables(CyNode.class).size());
//		assertEquals(totalNet, tableManager.getLocalTables(CyEdge.class).size());
		// No global tables in this example
		assertEquals(0, tableManager.getGlobalTables().size());
		
		for (CyNetwork net : networkTableManager.getNetworkSet())
			checkNetworkTables(net);
		
		// TODO test root-network tables
		// TODO test private tables
	}
	
	private void checkNetwork(final CyNetwork net) {
		assertEquals(SavePolicy.SESSION_FILE, net.getSavePolicy());
		
		assertEquals(NODE_COUNT, net.getNodeCount());
		assertEquals(EDGE_COUNT, net.getEdgeCount());
		
		// Network attributes
		assertEquals("Na", net.getDefaultNetworkTable().getRow(net.getSUID()).get(NAME, String.class));
		assertEquals("Na", net.getTable(CyNetwork.class, LOCAL_ATTRS).getRow(net.getSUID()).get(NAME, String.class));
		
		// Selection state
		Collection<CyRow> selectedNodes = net.getDefaultNodeTable().getMatchingRows(SELECTED, true);
		Collection<CyRow> selectedEdges = net.getDefaultEdgeTable().getMatchingRows(SELECTED, true);
		assertEquals(2, selectedNodes.size());
		assertEquals(1, selectedEdges.size());
		
		for (CyRow row : selectedNodes) {
			String name = row.get(NAME, String.class);
			Boolean selected = row.get(SELECTED, Boolean.class);
			assertEquals(name.equals("Node 1") || name.equals("Node 2"), selected);
		}
		for (CyRow row : selectedEdges) {
			String name = row.get(NAME, String.class);
			Boolean selected = row.get(SELECTED, Boolean.class);
			assertEquals(name.equals("Node 1 (interaction) Node 2"), selected);
		}
		
		// View test
		Collection<CyNetworkView> views = viewManager.getNetworkViews(net);
		assertEquals(1, views.size());
		
		final CyNetworkView view = views.iterator().next();
		assertEquals(3, view.getNodeViews().size());
		assertEquals(2, view.getEdgeViews().size());
		
		// Visual Style
		assertEquals(8, vmm.getAllVisualStyles().size());
		final VisualStyle style = vmm.getVisualStyle(view);
		checkVisualStyle(style);
		
		assertEquals(Color.WHITE, view.getVisualProperty(BasicVisualLexicon.NETWORK_BACKGROUND_PAINT));
		assertEquals(Double.valueOf(354.0d), view.getVisualProperty(BasicVisualLexicon.NETWORK_HEIGHT));
		assertEquals(Double.valueOf(526.0d), view.getVisualProperty(BasicVisualLexicon.NETWORK_WIDTH));
		assertEquals(1.15d, view.getVisualProperty(BasicVisualLexicon.NETWORK_SCALE_FACTOR), 0.001);
		assertEquals(1.06d, view.getVisualProperty(BasicVisualLexicon.NETWORK_CENTER_X_LOCATION), 0.009);
		assertEquals(16.96d, view.getVisualProperty(BasicVisualLexicon.NETWORK_CENTER_Y_LOCATION), 0.009);
//		assertEquals("Na", view.getVisualProperty(BasicVisualLexicon.NETWORK_TITLE)); // TODO: rename not setting title
		
		View<CyNode> nodeView = view.getNodeView(net.getNodeList().iterator().next());
		assertEquals(NodeShapeVisualProperty.ROUND_RECTANGLE, nodeView.getVisualProperty(BasicVisualLexicon.NODE_SHAPE));
		assertEquals(Double.valueOf(3.0d), nodeView.getVisualProperty(BasicVisualLexicon.NODE_BORDER_WIDTH));
		assertEquals(Double.valueOf(70.0d), nodeView.getVisualProperty(BasicVisualLexicon.NODE_WIDTH));
		assertEquals(Double.valueOf(40.0d), nodeView.getVisualProperty(BasicVisualLexicon.NODE_HEIGHT));
		assertEquals(new Color(0x00acad), nodeView.getVisualProperty(BasicVisualLexicon.NODE_FILL_COLOR));
		assertEquals(new Color(0x333333), nodeView.getVisualProperty(BasicVisualLexicon.NODE_BORDER_PAINT));
		
		View<CyEdge> edgeView = view.getEdgeView(net.getEdgeList().iterator().next());
		assertEquals(Double.valueOf(2.0d), edgeView.getVisualProperty(BasicVisualLexicon.EDGE_WIDTH));
		assertEquals(new Color(0x333333), edgeView.getVisualProperty(BasicVisualLexicon.EDGE_STROKE_UNSELECTED_PAINT));
		
		// TODO test custom assigned table (e.g. created by a plugin)
		assertTrue(net.getTable(CyNetwork.class, DEFAULT_ATTRS).isPublic());
		assertTrue(net.getTable(CyNode.class, DEFAULT_ATTRS).isPublic());
		assertTrue(net.getTable(CyEdge.class, DEFAULT_ATTRS).isPublic());
	}
	
	private void checkRootNetwork(final CyRootNetwork net) {
		assertEquals(SavePolicy.SESSION_FILE, net.getSavePolicy());
		
		assertNotNull(net.getTable(CyNetwork.class, DEFAULT_ATTRS));
		assertNotNull(net.getTable(CyNetwork.class, SHARED_DEFAULT_ATTRS));
		assertNotNull(net.getTable(CyNetwork.class, LOCAL_ATTRS));
		assertNotNull(net.getTable(CyNetwork.class, HIDDEN_ATTRS));
		assertNotNull(net.getTable(CyNetwork.class, SHARED_ATTRS));
		
		Set<CyTable> allTables = tableManager.getAllTables(true);
//		assertTrue(allTables.contains(net.getTable(CyNetwork.class, DEFAULT_ATTRS))); // TODO Why does it fail?
//		assertTrue(allTables.contains(net.getTable(CyNetwork.class, SHARED_DEFAULT_ATTRS)));
		assertTrue(allTables.contains(net.getTable(CyNetwork.class, LOCAL_ATTRS)));
		assertTrue(allTables.contains(net.getTable(CyNetwork.class, HIDDEN_ATTRS)));
		assertTrue(allTables.contains(net.getTable(CyNetwork.class, SHARED_ATTRS)));
		assertTrue(allTables.contains(net.getTable(CyNode.class, SHARED_ATTRS)));
		assertTrue(allTables.contains(net.getTable(CyEdge.class, SHARED_ATTRS)));
	}
	
	private void checkVisualStyle(final VisualStyle style) {
		assertNotNull(style);
		assertEquals(vmm.getDefaultVisualStyle(), style);
		
		Collection<VisualMappingFunction<?, ?>> mappings = style.getAllVisualMappingFunctions();
		assertEquals(1, mappings.size());
		VisualMappingFunction<?, ?> labelMapping = mappings.iterator().next();
		assertTrue(labelMapping instanceof PassthroughMapping);
		assertEquals(BasicVisualLexicon.NODE_LABEL, labelMapping.getVisualProperty());
		assertEquals(NAME, labelMapping.getMappingColumnName());
		assertEquals(String.class, labelMapping.getMappingColumnType());
	}
	
	private void checkNetworkTables(final CyNetwork net) {
		Map<String, CyTable> tables = networkTableManager.getTables(net, CyNetwork.class);
		
		for (Map.Entry<String, CyTable> entry : tables.entrySet()) {
			String namespace = entry.getKey();
			CyTable tbl = entry.getValue();
			
			if (namespace.equals(LOCAL_ATTRS) || namespace.equals(SHARED_ATTRS) || namespace.equals(HIDDEN_ATTRS))
				assertEquals(SavePolicy.SESSION_FILE, tbl.getSavePolicy());
			else
				assertEquals(namespace + " should have DO_NOT_SAVE policy", SavePolicy.DO_NOT_SAVE, tbl.getSavePolicy());
		}
		
		assertTrue(tables.containsValue(net.getTable(CyNetwork.class, DEFAULT_ATTRS)));
		assertTrue(tables.containsValue(net.getTable(CyNetwork.class, LOCAL_ATTRS)));
		assertTrue(tables.containsValue(net.getTable(CyNetwork.class, HIDDEN_ATTRS)));
		// These tables are always private
		assertFalse(net.getTable(CyNetwork.class, LOCAL_ATTRS).isPublic());
		assertFalse(net.getTable(CyNetwork.class, HIDDEN_ATTRS).isPublic());
		assertEquals(1, net.getTable(CyNetwork.class, DEFAULT_ATTRS).getAllRows().size());
		
		if (net instanceof CyRootNetwork) {
			assertTrue(tables.containsValue(net.getTable(CyNetwork.class, SHARED_ATTRS)));
			assertTrue(tables.containsValue(net.getTable(CyNetwork.class, SHARED_DEFAULT_ATTRS)));
			assertFalse(net.getTable(CyNetwork.class, SHARED_ATTRS).isPublic());
			assertFalse(net.getTable(CyNetwork.class, SHARED_DEFAULT_ATTRS).isPublic());
		}
		
		Map<String, CyTable> nodeTables = networkTableManager.getTables(net, CyNode.class);
		assertTrue(nodeTables.containsValue(net.getTable(CyNode.class, DEFAULT_ATTRS)));
		assertTrue(nodeTables.containsValue(net.getTable(CyNode.class, LOCAL_ATTRS)));
		assertTrue(nodeTables.containsValue(net.getTable(CyNode.class, HIDDEN_ATTRS)));
		assertFalse(net.getTable(CyNode.class, LOCAL_ATTRS).isPublic());
		assertFalse(net.getTable(CyNode.class, HIDDEN_ATTRS).isPublic());
		assertEquals(NODE_COUNT, net.getTable(CyNode.class, DEFAULT_ATTRS).getAllRows().size());
		
		if (net instanceof CyRootNetwork) {
			assertTrue(nodeTables.containsValue(net.getTable(CyNode.class, SHARED_ATTRS)));
			assertTrue(nodeTables.containsValue(net.getTable(CyNode.class, SHARED_DEFAULT_ATTRS)));
			assertFalse(net.getTable(CyNode.class, SHARED_ATTRS).isPublic());
			assertFalse(net.getTable(CyNode.class, SHARED_DEFAULT_ATTRS).isPublic());
		}
		
		Map<String, CyTable> edgeTables = networkTableManager.getTables(net, CyEdge.class);
		assertTrue(edgeTables.containsValue(net.getTable(CyEdge.class, DEFAULT_ATTRS)));
		assertTrue(edgeTables.containsValue(net.getTable(CyEdge.class, LOCAL_ATTRS)));
		assertTrue(edgeTables.containsValue(net.getTable(CyEdge.class, HIDDEN_ATTRS)));
		assertFalse(net.getTable(CyEdge.class, LOCAL_ATTRS).isPublic());
		assertFalse(net.getTable(CyEdge.class, HIDDEN_ATTRS).isPublic());
		assertEquals(EDGE_COUNT, net.getTable(CyEdge.class, DEFAULT_ATTRS).getAllRows().size());
		
		if (net instanceof CyRootNetwork) {
			assertTrue(edgeTables.containsValue(net.getTable(CyEdge.class, SHARED_ATTRS)));
			assertTrue(edgeTables.containsValue(net.getTable(CyEdge.class, SHARED_DEFAULT_ATTRS)));
			assertFalse(net.getTable(CyEdge.class, SHARED_ATTRS).isPublic());
			assertFalse(net.getTable(CyEdge.class, SHARED_DEFAULT_ATTRS).isPublic());
		}
	}
}
