package org.cytoscape.group.data.internal.aggregators;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cytoscape.group.CyGroup;
import org.cytoscape.group.data.Aggregator;
import org.cytoscape.group.data.AttributeHandlingType;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;

public class ListAggregator implements Aggregator {
		static AttributeHandlingType[] supportedTypes = {
			AttributeHandlingType.NONE,
			AttributeHandlingType.CONCAT,
			AttributeHandlingType.UNIQUE
		};

		static public AttributeHandlingType[] getSupportedTypes() { return supportedTypes; }

		AttributeHandlingType type;

		public ListAggregator(AttributeHandlingType type) {
			this.type = type;
		}

		public List aggregate(CyTable table, CyGroup group, CyColumn column) {
			Class listType = column.getListElementType();
			List <Object> agg = new ArrayList<Object>();
			Set <Object> aggset = new HashSet<Object>();
			List <?> aggregation = null;

			if (type == AttributeHandlingType.NONE) return null;

			// Initialization

			// Loop processing
			for (CyNode node: group.getNodeList()) {
				List<Object> list = table.getRow(node.getSUID()).getList(column.getName(), listType);
				if (list == null) continue;
				for (Object value: list) {
					switch (type) {
					case CONCAT:
						agg.add(value);
						break;
					case UNIQUE:
						aggset.add(value);
						break;
					}
				}
			}

			if (type == AttributeHandlingType.CONCAT)
				aggregation = agg;
			else if (type == AttributeHandlingType.UNIQUE)
				aggregation = new ArrayList<Object>(aggset);

			if (aggregation != null)
				table.getRow(group.getGroupNode().getSUID()).set(column.getName(), aggregation);

			return aggregation;
		}
}
