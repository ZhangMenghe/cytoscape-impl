package org.cytoscape.view.vizmap.gui.internal;

import java.awt.Color;
import java.awt.Font;
import java.beans.PropertyEditor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableEntry;
import org.cytoscape.model.CyTableManager;
import org.cytoscape.view.model.VisualLexicon;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.property.MinimalVisualLexicon;
import org.cytoscape.view.vizmap.VisualMappingFunction;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.gui.DefaultViewPanel;
import org.cytoscape.view.vizmap.gui.editor.EditorManager;
import org.cytoscape.view.vizmap.gui.internal.editor.propertyeditor.CyComboBoxPropertyEditor;
import org.cytoscape.view.vizmap.gui.internal.event.CellType;
import org.cytoscape.view.vizmap.gui.internal.theme.ColorManager;
import org.cytoscape.view.vizmap.gui.internal.util.VisualPropertyFilter;
import org.cytoscape.view.vizmap.gui.internal.util.VizMapperUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.l2fprod.common.propertysheet.Property;
import com.l2fprod.common.propertysheet.PropertyEditorRegistry;
import com.l2fprod.common.propertysheet.PropertyRendererRegistry;
import com.l2fprod.common.propertysheet.PropertySheetPanel;
import com.l2fprod.common.propertysheet.PropertySheetTable;
import com.l2fprod.common.propertysheet.PropertySheetTableModel.Item;

/**
 * Maintain property sheet table states.
 * 
 */
public class VizMapPropertySheetBuilder {

	private static final Logger logger = LoggerFactory.getLogger(VizMapPropertySheetBuilder.class);


	private PropertySheetPanel propertySheetPanel;

	private DefaultTableCellRenderer emptyBoxRenderer;
	private DefaultTableCellRenderer filledBoxRenderer;

	private VizMapPropertyBuilder vizMapPropertyBuilder;

	private EditorManager editorManager;

	private ColorManager colorMgr;

	private final VizMapperMenuManager menuMgr;

	private CyNetworkManager cyNetworkManager;
	
	private final VizMapperUtil util;
	
	private final VisualMappingManager vmm;
	
	/*
	 * Keeps Properties in the browser.
	 */
	private Map<VisualStyle, List<Property>> propertyMap;

	private List<VisualProperty<?>> unusedVisualPropType;

	public VizMapPropertySheetBuilder(final VizMapperMenuManager menuMgr, CyNetworkManager cyNetworkManager,
			PropertySheetPanel propertySheetPanel, EditorManager editorManager,
			DefaultViewPanel defViewPanel, CyTableManager tableMgr, final VizMapperUtil util, final VisualMappingManager vmm) {

		this.menuMgr = menuMgr;
		this.cyNetworkManager = cyNetworkManager;
		this.propertySheetPanel = propertySheetPanel;
		this.util = util;
		this.vmm = vmm;

		this.editorManager = editorManager;

		propertyMap = new HashMap<VisualStyle, List<Property>>();
		vizMapPropertyBuilder = new VizMapPropertyBuilder(cyNetworkManager, editorManager, tableMgr);
	}


	/**
	 * Create new properties.
	 * 
	 * @param style
	 */
	public void setPropertyTable(final VisualStyle style) {

		setPropertySheetAppearence(style);

		// Remove all.
		for (Property item : propertySheetPanel.getProperties())
			propertySheetPanel.removeProperty(item);
		
		final List<Property> propRecord = getPropertyListFromVisualStyle(style);

		// Save it for later use.
		propertyMap.put(style, propRecord);

		// Create unused prop section.
		setUnused(propRecord, style);
	}

	private void setPropertySheetAppearence(final VisualStyle style) {
		/*
		 * Set Tooltiptext for the table.
		 */
		propertySheetPanel.setTable(new VizMapPropertySheetTable());
		propertySheetPanel.getTable().getColumnModel()
				.addColumnModelListener(new VizMapPropertySheetTableColumnModelListener(this));

		/*
		 * By default, show category.
		 */
		propertySheetPanel.setMode(PropertySheetPanel.VIEW_AS_CATEGORIES);


		// TODO: fix listener
		propertySheetPanel.getTable().addMouseListener(
				new VizMapPropertySheetMouseAdapter(this.menuMgr, this, propertySheetPanel, style, editorManager));

		final PropertySheetTable table = propertySheetPanel.getTable();

		table.setRowHeight(27);
		table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		table.setCategoryBackground(new Color(10, 10, 50, 20));
		table.setCategoryForeground(Color.black);
		table.setSelectionBackground(Color.white);
		table.setSelectionForeground(Color.blue);

		/*
		 * Set editors
		 */
		// FIXME
		emptyBoxRenderer = new DefaultTableCellRenderer();
		emptyBoxRenderer.setHorizontalTextPosition(SwingConstants.CENTER);
		emptyBoxRenderer.setHorizontalAlignment(SwingConstants.CENTER);
		emptyBoxRenderer.setBackground(new Color(0, 200, 255, 20));
		emptyBoxRenderer.setForeground(Color.red);
		emptyBoxRenderer.setFont(new Font("SansSerif", Font.BOLD, 12));

		filledBoxRenderer = new DefaultTableCellRenderer();
		filledBoxRenderer.setBackground(Color.white);
		filledBoxRenderer.setForeground(Color.blue);
	}

	private List<Property> getPropertyListFromVisualStyle(final VisualStyle style) {

		final Collection<VisualProperty<?>> nodeVP = util.getVisualPropertySet(CyNode.class);
		final Collection<VisualProperty<?>> edgeVP = util.getVisualPropertySet(CyEdge.class);
		//final Collection<VisualProperty<?>> networkVP = style.getVisualLexicon().getVisualLexiconNode(TwoDVisualLexicon.NETWORK).getChildren();
		
		Collection<VisualProperty<?>> nodeVPSelected = new ArrayList<VisualProperty<?>>();
		Collection<VisualProperty<?>> edgeVPSelected = new ArrayList<VisualProperty<?>>();

		if (PropertySheetUtil.isAdvancedMode()) {
			nodeVPSelected = nodeVP;
			edgeVPSelected = edgeVP;
		} else {

			for (VisualProperty<?> vp : nodeVP) {
				if (PropertySheetUtil.isBasic(vp))
					nodeVPSelected.add(vp);
			}

			for (VisualProperty<?> vp : edgeVP) {
				if (PropertySheetUtil.isBasic(vp))
					edgeVPSelected.add(vp);
			}
		}
		
		
		final List<Property> nodeProps = getProps(style, MinimalVisualLexicon.NODE.getDisplayName(), nodeVPSelected);
		final List<Property> edgeProps = getProps(style, MinimalVisualLexicon.EDGE.getDisplayName(), edgeVPSelected);
		//final List<Property> networkProps = setProps(style, TwoDVisualLexicon.NETWORK);
		
		final List<Property> result = new ArrayList<Property>();
		
		
		result.addAll(nodeProps);
		result.addAll(edgeProps);
		//result.addAll(networkProps);
		
		return result;

	}

	
	private List<Property> getProps(final VisualStyle style, final String categoryName, final Collection<VisualProperty<?>> vpSet) {

		final List<Property> props = new ArrayList<Property>();
		final Collection<VisualMappingFunction<?, ?>> mappings = style.getAllVisualMappingFunctions();

		for (VisualMappingFunction<?, ?> mapping : mappings) {

			final VisualProperty<?> targetVP = mapping.getVisualProperty();
			// execute the following only if category matches.
			if (vpSet.contains(targetVP) == false)
				continue;
			
			logger.debug("This is a leaf VP: " + targetVP.getDisplayName());

			CyComboBoxPropertyEditor mappingSelector = (CyComboBoxPropertyEditor) editorManager.getDefaultComboBoxEditor("mappingTypeEditor");
			Set<Object> factories = mappingSelector.getAvailableValues();
			
			VisualMappingFunctionFactory vmfFactory = null;
			for(Object f: factories) {
				VisualMappingFunctionFactory factory = (VisualMappingFunctionFactory) f;
				Class<?> type = factory.getMappingFunctionType();				
				if(type.isAssignableFrom(mapping.getClass())) {
					vmfFactory = factory;
					break;
				}
			}
			
			final VizMapperProperty<?, String, ?> calculatorTypeProp = vizMapPropertyBuilder
					.buildProperty(mapping, categoryName, propertySheetPanel, vmfFactory);

			logger.debug("Built new PROP: " + calculatorTypeProp.getDisplayName());
			
			
			PropertyEditor editor = ((PropertyEditorRegistry) propertySheetPanel.getTable().getEditorFactory())
					.getEditor(calculatorTypeProp);
			
			
			
			if ((editor == null)
					&& (calculatorTypeProp.getCategory().equals(
							"Unused Properties") == false)) {
				
				((PropertyEditorRegistry) this.propertySheetPanel
						.getTable().getEditorFactory())
						.registerEditor(calculatorTypeProp, editorManager
								.getDataTableComboBoxEditor((Class<? extends CyTableEntry>) targetVP.getTargetDataType()));
			}
			props.add(calculatorTypeProp);
		}

		return props;
	}

	private void setUnused(List<Property> propList, VisualStyle style) {
		buildList(style);

		// TODO: Sort the unused list.
		// Collections.sort(getUnusedVisualPropType());

		for (VisualProperty<?> type : getUnusedVisualPropType()) {
			VizMapperProperty<VisualProperty<?>, String, ?> prop = new VizMapperProperty<VisualProperty<?>, String, Object>(CellType.UNUSED, type, String.class);
			prop.setCategory(AbstractVizMapperPanel.CATEGORY_UNUSED);
			prop.setDisplayName(type.getDisplayName());
			prop.setValue("Double-Click to create...");
			prop.setEditable(false);
			propertySheetPanel.addProperty(prop);
			propList.add(prop);
		}
	}

	private void buildList(final VisualStyle style) {

		unusedVisualPropType = new ArrayList<VisualProperty<?>>();
		VisualMappingFunction<?, ?> mapping = null;

		final Set<VisualLexicon> lexSet = vmm.getAllVisualLexicon();
		for(VisualLexicon lex: lexSet) {

			for (VisualProperty<?> type : lex.getAllVisualProperties()) {
				
				if(VisualPropertyFilter.isCompatible(type) == false)
					continue;
				
				if (PropertySheetUtil.isAdvancedMode() == false) {
					if (PropertySheetUtil.isBasic(type) == false)
						continue;
				}
				
				mapping = style.getVisualMappingFunction(type);
	
				if (mapping == null && lex.getVisualLexiconNode(type).getChildren().size() == 0)
					unusedVisualPropType.add(type);
	
				mapping = null;
			}
		}
	}

	public void updateTableView() {
		logger.debug("Table update called:");
		final PropertySheetTable table = propertySheetPanel.getTable();
		final DefaultTableCellRenderer empRenderer = new DefaultTableCellRenderer();

		// Number of rows shown now.
		int rowCount = table.getRowCount();

		for (int i = 0; i < rowCount; i++) {
			
			final VizMapperProperty<?, ?, ?> shownProp = (VizMapperProperty<?, ?, ?>) ((Item) table.getValueAt(i, 0)).getProperty();
			if(shownProp == null)
				continue;
			if(shownProp.getCellType().equals(CellType.CONTINUOUS)) {				
				table.setRowHeight(i, 80);
			} else if ((shownProp.getCategory() != null)
					&& shownProp.getCategory().equals(
							AbstractVizMapperPanel.CATEGORY_UNUSED)) {

				// FIXME
				// empRenderer.setForeground(colorMgr.getColor("UNUSED_COLOR"));
				((PropertyRendererRegistry) this.propertySheetPanel.getTable()
						.getRendererFactory()).registerRenderer(shownProp,
						empRenderer);
			}
		}
		propertySheetPanel.repaint();
	}

	public void expandLastSelectedItem(String name) {
		final PropertySheetTable table = propertySheetPanel.getTable();
		Item item = null;
		Property curProp;

		for (int i = 0; i < table.getRowCount(); i++) {
			item = (Item) table.getValueAt(i, 0);

			curProp = item.getProperty();

			if ((curProp != null) && (curProp.getDisplayName().equals(name))) {
				table.setRowSelectionInterval(i, i);

				if (item.isVisible() == false) {
					item.toggle();
				}
				return;
			}
		}
	}
	
	public List<Property> getPropertyList(final VisualStyle style) {
		if(propertyMap.containsKey(style) == false) {
			final List<Property> newList = new ArrayList<Property>();
			propertyMap.put(style, newList);
			return newList;
		} else
			return propertyMap.get(style);
	}
	
	public void removePropertyList(final VisualStyle style) {
		propertyMap.remove(style);
	}
	
	

	/*
	 * Remove an entry in the browser.
	 */
	public void removeProperty(final Property prop, final VisualStyle style) {

		final List<Property> props = propertyMap.get(style);
		if (props == null)
			return;

		final List<Property> targets = new ArrayList<Property>();
		
		for (Property p : props) {
			if(p.getDisplayName() == null)
				continue;
			if (p.getDisplayName().equals(prop.getDisplayName()))
				targets.add(p);
		}

		for (Property p : targets)
			props.remove(p);
	}

	// TODO: this should be gone
	public void setAttrComboBox() {
		// Attribute Names
		final List<String> names = new ArrayList<String>();
		//
		// CyTable attr = /* TODO */getTargetNetwork().getNodeCyDataTables()
		// .get(CyNetwork.DEFAULT_ATTRS);
		//
		// // TODO remove the next line too!
		// if (attr == null)
		// return;
		//
		// Map<String, Class<?>> cols = attr.getColumnTypeMap();
		// names.addAll(cols.keySet());
		//
		// Collections.sort(names);
		//
		// // nodeAttrEditor.setAvailableValues(names.toArray());
		// spcs.firePropertyChange("UPDATE_AVAILABLE_VAL", "nodeAttrEditor",
		// names
		// .toArray());
		//
		// names.clear();
		//
		// for (String name : cols.keySet()) {
		// Class<?> dataClass = cols.get(name);
		//
		// if ((dataClass == Integer.class) || (dataClass == Double.class))
		// names.add(name);
		// }
		//
		// Collections.sort(names);
		// // nodeNumericalAttrEditor.setAvailableValues(names.toArray());
		// spcs.firePropertyChange("UPDATE_AVAILABLE_VAL",
		// "nodeNumericalAttrEditor", names.toArray());
		//
		// names.clear();
		//
		// attr = getTargetNetwork().getEdgeCyDataTables().get(
		// CyNetwork.DEFAULT_ATTRS);
		// cols = attr.getColumnTypeMap();
		// names.addAll(cols.keySet());
		// Collections.sort(names);
		//
		// // edgeAttrEditor.setAvailableValues(names.toArray());
		// spcs.firePropertyChange("UPDATE_AVAILABLE_VAL", "edgeAttrEditor",
		// names
		// .toArray());
		// names.clear();
		//
		// for (String name : cols.keySet()) {
		// Class<?> dataClass = cols.get(name);
		//
		// if ((dataClass == Integer.class) || (dataClass == Double.class))
		// names.add(name);
		// }
		//
		// Collections.sort(names);
		// // edgeNumericalAttrEditor.setAvailableValues(names.toArray());
		// spcs.firePropertyChange("UPDATE_AVAILABLE_VAL",
		// "edgeNumericalAttrEditor", names.toArray());
		// propertySheetPanel.repaint();
	}

	public VizMapPropertyBuilder getPropertyBuilder() {
		return this.vizMapPropertyBuilder;
	}

	public List<VisualProperty<?>> getUnusedVisualPropType() {
		return unusedVisualPropType;
	}

}
