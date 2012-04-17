/*
 * PnutsLayout.java
 * 
 * Copyright (c) 1997-2006 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * See the file "LICENSE.txt" for information on usage and redistribution of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */

package pnuts.awt;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.LayoutManager2;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.JPanel;

/**
 * The PnutsLayout is a general purpose geometry manager. It is very easy to use and can accomplish any grid based layout.
 * 
 * <pre>
 * setLayout(new PnutsLayout(3));
 * add(button1, &quot;ipadding=20&quot;);
 * add(button2, &quot;padding=20&quot;);
 * add(button3, &quot;colspan=2&quot;);
 * add(button4, &quot;rowspan=2&quot;);
 * add(button3, &quot;align=top:left&quot;);
 * add(button3, &quot;align=bottom:right&quot;);
 * add(button3, &quot;width=100:center&quot;);
 * </pre>
 * 
 * <table border>
 * <tr>
 * <th>Property</th>
 * <th>Meaning</th>
 * <th>Default</th>
 * <th>constructor</th>
 * <th>add()</th>
 * </tr>
 * <tr>
 * <td>columns</td>
 * <td>The number of columns.</td>
 * <td>1</td>
 * <td>true</td>
 * <td>false</td>
 * </tr>
 * <tr>
 * <td>uniform</td>
 * <td>Sets the width or height to be the same for each column. Can be x/y/xy/none.</td>
 * <td>none</td>
 * <td>true</td>
 * <td>false</td>
 * </tr>
 * <tr>
 * <td>colspan</td>
 * <td>Number of columns the component occupies.</td>
 * <td>1</td>
 * <td>false</td>
 * <td>true</td>
 * </tr>
 * <tr>
 * <td>rowspan</td>
 * <td>Number of rows the component occupies.</td>
 * <td>1</td>
 * <td>false</td>
 * <td>true</td>
 * </tr>
 * <tr>
 * <td>spacing</td>
 * <td>Minimum spacing that will be put around the component. Can be a single value (5) or top:right:bottom:left (eg 5:10:5:10).</td>
 * <td>0</td>
 * <td>true</td>
 * <td>true</td>
 * </tr>
 * <tr>
 * <td>padding</td>
 * <td>Padding around the component. Can be a single value (5) or top:right:bottom:left (eg 5:10:5:10).</td>
 * <td>0</td>
 * <td>true</td>
 * <td>true</td>
 * </tr>
 * <tr>
 * <td>ipadding</td>
 * <td>Padding inside the component (making it larger). Can be a single value (5) or x:y (eg 5:10).</td>
 * <td>0</td>
 * <td>true</td>
 * <td>true</td>
 * </tr>
 * <tr>
 * <td>fill</td>
 * <td>Sets the width and/or height of the component to a percentage of the cell width. Can be x/y/xy/none or a single value (5) or
 * x:y (eg 25:75). 0 or none sizes the component to its preferred size.</td>
 * <td>none</td>
 * <td>true</td>
 * <td>true</td>
 * </tr>
 * <tr>
 * <td>align</td>
 * <td>Alignment of the component in the cell. Can be top/bottom/left/right/center or top/bottom/center:left/right/center (eg
 * top:right).</td>
 * <td>center</td>
 * <td>true</td>
 * <td>true</td>
 * </tr>
 * <tr>
 * <td>expand</td>
 * <td>Expands the size of the cell so the table takes up the size of the container. Can be x/y/xy/none.</td>
 * <td>none</td>
 * <td>true</td>
 * <td>true</td>
 * </tr>
 * <tr>
 * <td>border</td>
 * <td>Draws the grid borders of the table. Great for debugging. Can be true/false.</td>
 * <td>false</td>
 * <td>true</td>
 * <td>true</td>
 * </tr>
 * </table>
 * <p>
 * @version 2.0
 * @author Toyokazu Tomatsu
 * @author Nathan Sweet (misc@n4te.com)
 */
public class PnutsLayout implements LayoutManager2, Serializable {
	public static final int CENTER = 2;
	public static final int TOP = 4;
	public static final int BOTTOM = 8;
	public static final int LEFT = 16;
	public static final int RIGHT = 32;
	public static final int X = 64;
	public static final int Y = 128;
	public static final int NONE = 0;

	/**
	 * @serial
	 */
	private final TableConstraints tableConstraints = new TableConstraints();

	/**
	 * @serial
	 */
	private final Map cellConstraints = new HashMap();

	/**
	 * @serial
	 */
	private int widths[];

	/**
	 * @serial
	 */
	private int heights[];

	/**
	 * @serial
	 */
	private int xExpand[];

	/**
	 * @serial
	 */
	private int yExpand[];

	/**
	 * @serial
	 */
	private int xExpandCount;

	/**
	 * @serial
	 */
	private int yExpandCount;

	/**
	 * Grid column index to column x coordinate.
	 * @serial
	 */
	private int columnLeft[];

	/**
	 * Grid row index to row y coordinate.
	 * @serial
	 */
	private int rowTop[];

	/**
	 * Row and column to spacing bottom and right.
	 * @serial
	 */
	private int cellSpacing[][][];

	/**
	 * Component index to x grid position.
	 * @serial
	 */
	private int gridPosition_x[] = new int[8];

	/**
	 * Component index to y grid position.
	 * @serial
	 */
	private int gridPosition_y[] = new int[8];

	/**
	 * @serial
	 */
	private Dimension preferredSize;

	/**
	 * @serial
	 */
	private boolean valid = false;

	/**
	 * The number of rows
	 * @serial
	 */
	private int rows;

	/**
	 * BorderPanel used for drawing grid borders.
	 */
	private BorderPanel borderPanel;

	/**
	 * Constructs a PnutsLayout with a single column.
	 */
	public PnutsLayout () {
		this(1);
	}

	/**
	 * Constructs a PnutsLayout with the specified number of columns.
	 */
	public PnutsLayout (int columns) {
		tableConstraints.setColumns(columns);
	}

	/*
	 * Constructs a PnutsLayout from a constraint String. See PnutsLayout class comments for constraints usage.
	 */
	public PnutsLayout (String str) {
		this(str2table(str));
	}

	/**
	 * Constructs a PnutsLayout from a constraint Map. See PnutsLayout class comments for constraints usage.
	 * @param map
	 */
	public PnutsLayout (Map map) {
		tableConstraints.configure(map);
	}

	/**
	 * Sets the constraints for a component. See PnutsLayout class comments for constraints usage.
	 */
	public void setConstraints (Component comp, String str) {
		setConstraints(comp, new CellConstraints(str2table(str)));
	}

	/**
	 * Sets the constraints for a component. See PnutsLayout class comments for constraints usage.
	 */
	public void setConstraints (Component comp, Map map) {
		setConstraints(comp, new CellConstraints(map));
	}

	/**
	 * Sets the constraints for a component. See PnutsLayout class comments for constraints usage.
	 */
	public void setConstraints (Component comp, CellConstraints constraints) {
		cellConstraints.put(comp, constraints);
		invalidate();
	}

	/**
	 * Compute geometries that do not depend on the size of the container.
	 */
	private void bindContainer (Container target) {
		int columnCount = tableConstraints.getColumns();

		BitSet map[] = new BitSet[columnCount];
		for (int i = 0; i < columnCount; i++)
			map[i] = new BitSet();

		int compCount = target.getComponentCount();

		gridPosition_x = new int[compCount * 2];
		gridPosition_y = new int[compCount * 2];

		/**
		 * 1) Decide components' logical location. 2) Count total columns and rows.
		 */
		rows = 0;
		for (int i = 0, gridX = 0, gridY = 0; i < compCount; i++) {
			Component comp = target.getComponent(i);
			if (comp instanceof BorderPanel) continue;
			CellConstraints constraints = (CellConstraints)cellConstraints.get(comp);

			int colspan = constraints.getColspan();
			int rowspan = constraints.getRowspan();
			while (!fit(gridX, gridY, colspan, rowspan, map)) {
				if (++gridX >= columnCount) {
					gridX = 0;
					++gridY;
				}
			}
			gridPosition_y[i] = gridY;
			gridPosition_x[i] = gridX;

			for (int jj = 0; jj < colspan; jj++) {
				for (int kk = 0; kk < rowspan; kk++) {
					map[gridX + jj].set(gridY + kk);
				}
			}

			if (rows < gridY + rowspan) {
				rows = gridY + rowspan;
			}

			if (++gridX >= columnCount) {
				gridX = 0;
				++gridY;
			}
		}

		cellSpacing = new int[columnCount][rows][2];
		columnLeft = new int[columnCount + 1];
		rowTop = new int[rows + 1];
		widths = new int[columnCount];
		heights = new int[rows];
		yExpand = new int[rows];
		xExpand = new int[columnCount];

		/*
		 * 3) Mark expanded locations.
		 */
		for (int i = 0, gridX = 0, gridY = 0; i < compCount; i++) {
			Component comp = target.getComponent(i);
			if (comp instanceof BorderPanel) continue;
			CellConstraints constraints = (CellConstraints)cellConstraints.get(comp);

			gridY = gridPosition_y[i];
			gridX = gridPosition_x[i];

			int colspan = constraints.getColspan();
			int rowspan = constraints.getRowspan();
			int expand = constraints.getExpand();

			if ((expand & X) != 0) {
				for (int ii = 0; ii < colspan; ii++)
					xExpand[gridX + ii] |= 1;
			}
			if ((expand & Y) != 0) {
				for (int ii = 0; ii < rowspan; ii++)
					yExpand[gridY + ii] |= 1;
			}
			gridX += colspan;
			if (gridX >= columnCount) {
				gridX = 0;
				gridY++;
			}
		}

		boolean xFixed = (tableConstraints.getFixed() & X) != 0;
		boolean yFixed = (tableConstraints.getFixed() & Y) != 0;

		/*
		 * 4) Compute the size of cells.
		 */
		int maxWidth = 0;
		int maxHeight = 0;
		boolean cellNeedsBorder = false;
		for (int i = 0; i < compCount; i++) {
			Component comp = target.getComponent(i);
			if (comp instanceof BorderPanel) continue;
			CellConstraints constraints = (CellConstraints)cellConstraints.get(comp);
			int colspan = constraints.getColspan();
			int rowspan = constraints.getRowspan();

			if (constraints.getBorder()) cellNeedsBorder = true;

			Dimension d = comp.getPreferredSize();
			int ipadding[] = constraints.getIPadding();
			int compWidth = d.width + ipadding[0];
			int compHeight = d.height + ipadding[1];

			int gridX = gridPosition_x[i];
			int gridY = gridPosition_y[i];

			// Cloned because these will be modified.
			int padding[] = (int[])constraints.getPadding().clone();
			int spacing[] = (int[])constraints.getSpacing().clone();
			// Spacing around other components does not add up.
			if (gridY > 0) {
				spacing[0] -= cellSpacing[gridX][gridY - 1][0];
				if (spacing[0] < 0) spacing[0] = 0;
			}
			if (gridX > 0) {
				spacing[3] -= cellSpacing[gridX - 1][gridY][1];
				if (spacing[3] < 0) spacing[3] = 0;
			}
			// Store bottom and right spacing at all grid locations component occupies.
			for (int colOffset = 0; colOffset < colspan; colOffset++) {
				for (int rowOffset = 0; rowOffset < rowspan; rowOffset++) {
					cellSpacing[gridX + colOffset][gridY + rowOffset][0] = spacing[2];
					cellSpacing[gridX + colOffset][gridY + rowOffset][1] = spacing[1];
				}
			}
			// Increase padding by the modified spacing.
			padding[0] += spacing[0];
			padding[1] += spacing[1];
			padding[2] += spacing[2];
			padding[3] += spacing[3];

			compWidth += padding[1] + padding[3];

			int expand_count = 0;
			int no_expand_widths = 0;
			for (int ii = 0; ii < colspan; ii++) {
				if ((xExpand[gridX + ii] & 1) != 0) { // expand is set
					expand_count++;
				} else {
					no_expand_widths += widths[gridX + ii];
				}
			}

			if (colspan == 1) {
				if (compWidth > widths[gridX]) widths[gridX] = compWidth;
				if (maxWidth < widths[gridX]) maxWidth = widths[gridX];
			} else {
				for (int ii = 0; ii < colspan; ii++) {
					if ((xExpand[gridX + ii] & 1) != 0) { // expand is set
						int expandedWidth = (compWidth - no_expand_widths) / expand_count;
						if (expandedWidth > widths[gridX + ii]) widths[gridX + ii] = expandedWidth;
					}
					if (xFixed && maxWidth < widths[gridX + ii]) maxWidth = widths[gridX + ii];
				}
			}

			if (heights[gridY] < (compHeight + padding[0] + padding[2]) / rowspan) {
				heights[gridY] = (compHeight + padding[0] + padding[2]) / rowspan;
				if (yFixed && maxHeight < heights[gridY]) maxHeight = heights[gridY];
			}
		}

		if (xFixed) {
			for (int i = 0; i < columnCount; i++)
				widths[i] = maxWidth;
		}
		if (yFixed) {
			for (int i = 0; i < rows; i++)
				heights[i] = maxHeight;
		}

		int width = 0, height = 0;
		for (int j = 0; j < columnCount; j++)
			width += widths[j];
		for (int j = 0; j < rows; j++)
			height += heights[j];
		Insets insets = target.getInsets();
		width += insets.left + insets.right;
		height += insets.top + insets.bottom;
		preferredSize = new Dimension(width, height);

		xExpandCount = 0;
		for (int i = 0; i < xExpand.length; i++) {
			if ((xExpand[i] & 1) != 0) xExpandCount++;
		}
		yExpandCount = 0;
		for (int i = 0; i < yExpand.length; i++) {
			if ((yExpand[i] & 1) != 0) {
				yExpandCount++;
			}
		}

		if (cellNeedsBorder || tableConstraints.getBorder()) {
			borderPanel = new BorderPanel();
			target.add(borderPanel);
		}

		valid = true;
	}

	/**
	 * Lays out the container. This method will actually reshape the components in the target in order to satisfy the constraints
	 * of the PnutsLayout object.
	 * @see Container
	 */
	public void layoutContainer (Container target) {
		int columnCount = tableConstraints.getColumns();

		int compCount = target.getComponentCount();
		if (!valid) bindContainer(target);

		int targetWidth = target.getSize().width;
		int targetHeight = target.getSize().height;

		int aw = 0;
		int ah = 0;
		if (xExpandCount > 0) aw = (targetWidth - preferredSize.width) / xExpandCount;
		if (yExpandCount > 0) ah = (targetHeight - preferredSize.height) / yExpandCount;

		Insets insets = target.getInsets();
		int w = insets.left;
		columnLeft[0] = w;
		for (int i = 1; i <= columnCount; i++) {
			columnLeft[i] = columnLeft[i - 1] + widths[i - 1];
			if ((xExpand[i - 1] & 1) != 0) {
				columnLeft[i] += aw;
			}
		}
		int h = insets.top;
		rowTop[0] = h;
		for (int i = 1; i <= rows; i++) {
			rowTop[i] = rowTop[i - 1] + heights[i - 1];
			if ((yExpand[i - 1] & 1) != 0) {
				rowTop[i] += ah;
			}
		}

		for (int i = 0; i < compCount; i++) {
			Component comp = target.getComponent(i);
			if (comp instanceof BorderPanel) continue;
			CellConstraints constraints = (CellConstraints)cellConstraints.get(comp);
			int colspan = constraints.getColspan();
			int rowspan = constraints.getRowspan();
			int align = constraints.getAlign();
			int[] fill = constraints.getFill();

			Dimension d = comp.getPreferredSize();
			int ipadding[] = constraints.getIPadding();
			int compWidth = d.width + ipadding[0];
			int compHeight = d.height + ipadding[1];

			int gridX = gridPosition_x[i];
			int gridY = gridPosition_y[i];

			// Cloned because these will be modified.
			int padding[] = (int[])constraints.getPadding().clone();
			int spacing[] = (int[])constraints.getSpacing().clone();
			// Spacing around other components does not add up.
			if (gridY > 0) {
				spacing[0] -= cellSpacing[gridX][gridY - 1][0];
				if (spacing[0] < 0) spacing[0] = 0;
			}
			if (gridX > 0) {
				spacing[3] -= cellSpacing[gridX - 1][gridY][1];
				if (spacing[3] < 0) spacing[3] = 0;
			}
			// Store bottom and right spacing at all grid locations component occupies.
			for (int colOffset = 0; colOffset < colspan; colOffset++) {
				for (int rowOffset = 0; rowOffset < rowspan; rowOffset++) {
					cellSpacing[gridX + colOffset][gridY + rowOffset][0] = spacing[2];
					cellSpacing[gridX + colOffset][gridY + rowOffset][1] = spacing[1];
				}
			}
			// Increase padding by the modified spacing.
			padding[0] += spacing[0];
			padding[1] += spacing[1];
			padding[2] += spacing[2];
			padding[3] += spacing[3];

			// Optionally set component size to a percentage of the cell size.
			if (fill[0] > 0)
				compWidth = (int)((columnLeft[gridX + colspan] - columnLeft[gridX] - (padding[3] + padding[1])) * (fill[0] / 100F));
			if (fill[1] > 0)
				compHeight = (int)((rowTop[gridY + rowspan] - rowTop[gridY] - (padding[0] + padding[2])) * (fill[1] / 100F));

			// Compute location of component.
			int x, y;

			if ((align & LEFT) != 0) {
				x = columnLeft[gridX] + padding[3];
			} else if ((align & RIGHT) != 0) {
				x = columnLeft[gridX + colspan] - compWidth - padding[1];
			} else {
				x = (columnLeft[gridX] + columnLeft[gridX + colspan] - compWidth + padding[3] - padding[1]) / 2;
			}

			if ((align & TOP) != 0) {
				y = rowTop[gridY] + padding[0];
			} else if ((align & BOTTOM) != 0) {
				y = rowTop[gridY + rowspan] - compHeight - padding[2];
			} else {
				y = (rowTop[gridY] + rowTop[gridY + rowspan] - compHeight + padding[0] - padding[2]) / 2;
			}

			comp.setBounds(x, y, compWidth, compHeight);
		}

		if (borderPanel != null) borderPanel.setSize(target.getSize());
	}

	private boolean fit (int x, int y, int colspan, int rowspan, BitSet[] map) {
		for (int i = 0; i < colspan; i++) {
			for (int j = 0; j < rowspan; j++) {
				if (x + i >= tableConstraints.getColumns()) return false;
				if (map[x + i].get(y + j)) return false;
			}
		}
		return true;
	}

	/**
	 * Returns the number of rows in this layout.
	 */
	public int getRows () {
		if (!valid) throw new RuntimeException("PnutsLayout has not been realized.");
		return rows;
	}

	/**
	 * Returns the constraints used by the table and the default constraints used for all cells in the table. Modifying this object
	 * will modify the table's constraints.
	 */
	public TableConstraints getTableConstraints () {
		return tableConstraints;
	}

	/**
	 * Returns the constraints used by the cell for the specified component. Modifying this object will modify the cell's
	 * constraints.
	 */
	public TableConstraints getCellConstraints (Component comp) {
		return (TableConstraints)cellConstraints.get(comp);
	}

	/**
	 * Returns the left-top point of the specified cell.
	 */
	public Point getGridPoint (int gridX, int gridY) {
		if (!valid) return null;
		return new Point(columnLeft[gridX], rowTop[gridY]);
	}

	/**
	 * Returns the bounding box for child component at the specified index.
	 */
	public Rectangle getGridRectangle (Container parent, int index) {
		if (!valid) return null;

		int gridX = gridPosition_x[index];
		int gridY = gridPosition_y[index];
		int cellWidth = columnLeft[gridX];
		int cellHeight = rowTop[gridY];

		CellConstraints constraints = (CellConstraints)cellConstraints.get(parent.getComponent(index));
		int colspan = constraints.getColspan();
		int rowspan = constraints.getRowspan();
		return new Rectangle(cellWidth, cellHeight, columnLeft[gridX + colspan] - cellWidth, rowTop[gridY + rowspan] - cellHeight);
	}

	/**
	 * Adds the specified component to the layout, using the specified constraint object.
	 * @param obj Map or String defining constraints for the component.
	 */
	public void addLayoutComponent (Component comp, Object obj) {
		if (obj instanceof Map) {
			setConstraints(comp, (Map)obj);
		} else if (obj instanceof String) {
			setConstraints(comp, (String)obj);
		}
	}

	/**
	 * Returns the preferred dimensions for this layout given the components in the specified target container.
	 * @see Container
	 * @see #minimumLayoutSize
	 */
	public Dimension preferredLayoutSize (Container target) {
		if (!valid) bindContainer(target);
		return preferredSize;
	}

	/**
	 * Returns the minimum dimensions needed to layout the components contained in the specified target container.
	 * @see #preferredLayoutSize
	 */
	public Dimension minimumLayoutSize (Container target) {
		return preferredLayoutSize(target);
	}

	/**
	 * Returns the maximum size of this component.
	 * @see java.awt.Component#getMinimumSize()
	 * @see java.awt.Component#getPreferredSize()
	 * @see LayoutManager
	 */
	public Dimension maximumLayoutSize (Container target) {
		return target.getMaximumSize();
	}

	/**
	 * Invalidates the layout, indicating that if the PnutsLayout has cached information it should be discarded. This happens
	 * automatically when cell or table constraints are modified.
	 */
	public void invalidate () {
		synchronized (this) {
			if (!valid) return;
			valid = false;
		}

		// Remove BorderPanel from its parent.
		if (borderPanel != null) {
			Container parent = borderPanel.getParent();
			if (parent != null) parent.remove(borderPanel);
			borderPanel = null;
		}
	}

	/**
	 * Invalidates the layout, indicating that if the layout manager has cached information it should be discarded.
	 */
	public void invalidateLayout (Container target) {
		invalidate();
	}

	/**
	 * Adds the specified component with the specified name to the layout.
	 */
	public void addLayoutComponent (String name, Component comp) {
	}

	/**
	 * Removes the specified component from the layout.
	 */
	public void removeLayoutComponent (Component comp) {
	}

	/**
	 * Returns the alignment along the x axis. This specifies how the component would like to be aligned relative to other
	 * components. The value should be a number between 0 and 1 where 0 represents alignment along the origin, 1 is aligned the
	 * furthest away from the origin, 0.5 is centered, etc.
	 */
	public float getLayoutAlignmentX (Container target) {
		return 0f;
	}

	/**
	 * Returns the alignment along the y axis. This specifies how the component would like to be aligned relative to other
	 * components. The value should be a number between 0 and 1 where 0 represents alignment along the origin, 1 is aligned the
	 * furthest away from the origin, 0.5 is centered, etc.
	 */
	public float getLayoutAlignmentY (Container target) {
		return 0f;
	}

	public String toString () {
		return getClass().getName() + "[" + getTableConstraints() + "]";
	}

	/**
	 * Transforms a constraint name/value pair String into a Map.
	 */
	static Map str2table (String constraintString) {
		Map table = new HashMap();
		StringTokenizer constraints = new StringTokenizer(constraintString, ",");
		while (constraints.hasMoreTokens()) {
			String token = constraints.nextToken();
			if (constraintString.indexOf('=') == -1) throw new LayoutException("Invalid constraint name/value pair: " + token);
			StringTokenizer nameValue = new StringTokenizer(token, "=");
			String name = nameValue.nextToken();
			String value = null;
			if (nameValue.hasMoreTokens()) value = nameValue.nextToken().trim();
			table.put(name.trim(), value);
		}
		return table;
	}

	static private final Integer ZERO = new Integer(0);
	static private Integer[] integer = new Integer[33];
	static {
		for (int i = 0; i < integer.length; i++)
			integer[i] = new Integer(i);
	}

	/**
	 * Uses caching of low numbered Integer objects to reduce object creation.
	 * @return An Integer object for the specified int primitive.
	 */
	static private Integer getInteger (int i) {
		if (i < 32) {
			return integer[i];
		} else {
			return new Integer(i);
		}
	}

	/**
	 * @param obj Must be a String.
	 * @return An Integer representing the passed in String, or null if the String is null.
	 * @throws NumberFormatException if the String could not be parsed into an Integer.
	 */
	static private Integer getInteger (Object obj) {
		if (obj == null) return null;
		return getInteger(Integer.parseInt((String)obj));
	}

	/**
	 * @param obj Must be a String.
	 * @return An Integer[] representing the passed in String, or null if the String is null.
	 * @throws NumberFormatException if the String could not be parsed into an Integer[].
	 */
	static private Integer[] getIntegerArray (Object obj, int length) {
		if (obj == null) return null;
		String str = (String)obj;
		if (str.indexOf(':') == -1) {
			Integer value = getInteger(str);
			return new Integer[] {value, value, value, value};
		}
		Integer[] array = new Integer[length];
		StringTokenizer st = new StringTokenizer(str, ":");
		for (int i = 0; i < length; i++) {
			if (st.hasMoreTokens()) {
				String value = st.nextToken();
				array[i] = getInteger(Integer.parseInt(value));
			} else
				array[i] = ZERO;
		}
		return array;
	}

	/**
	 * Stores constraints common to both the table and cells.
	 */
	public abstract class Constraints {
		private Integer[] padding, spacing, ipadding, fill;
		private Integer align, expand;
		private Boolean border;

		/**
		 * Configures this contraint object with a Map. See PnutsLayout class comments for constraints usage.
		 */
		public void configure (Map map) {
			setSpacing(getIntegerArray(map.get("spacing"), 4));
			setPadding(getIntegerArray(map.get("padding"), 4));
			setIPadding(getIntegerArray(map.get("ipadding"), 2));
			setAlign((String)map.get("align"));
			setExpand((String)map.get("expand"));
			setFill((String)map.get("fill"));

			String border = (String)map.get("border");
			if (border != null) setBorder(new Boolean("true".equals(border)));

			// Legacy constraints.
			if (map.get("padding") == null) {
				Integer padx = getInteger(map.get("padx"));
				Integer pady = getInteger(map.get("pady"));
				if (padx != null || pady != null) {
					if (pady == null) pady = ZERO;
					if (padx == null) padx = ZERO;
					setPadding(new Integer[] {pady, padx, pady, padx});
				}
			}

			if (map.get("ipadding") == null) {
				Integer ipadx = getInteger(map.get("ipadx"));
				Integer ipady = getInteger(map.get("ipady"));
				if (ipadx != null || ipady != null) {
					if (ipady == null) ipady = ZERO;
					if (ipadx == null) ipadx = ZERO;
					setIPadding(new Integer[] {ipadx, ipady});
				}
			}

			if (map.get("align") == null) {
				String valign = (String)map.get("valign");
				if ("fill".equals(valign)) {
					setFill(new Integer[] {getInteger(getFill()[0]), getInteger(100)});
					valign = null;
				}
				String halign = (String)map.get("halign");
				if ("fill".equals(halign)) {
					setFill(new Integer[] {getInteger(100), getInteger(getFill()[1])});
					halign = null;
				}
				String alignString = null;
				if (valign != null && halign != null)
					alignString = valign + ':' + halign;
				else if (valign != null)
					alignString = valign;
				else if (halign != null) {
					alignString = halign;
				}
				if (alignString != null) setAlign(alignString);
			}
		}

		public int getAlign () {
			if (align == null) return tableConstraints.getAlign();
			return align.intValue();
		}

		/**
		 * Can be TOP, RIGHT, BOTTOM, LEFT, or any combination of TOP/BOTTOM and RIGHT/LEFT.
		 */
		public void setAlign (Integer align) {
			if (align == null && this == tableConstraints) return;
			this.align = align;
			invalidate();
		}

		public void setAlign (String alignString) {
			if (alignString == null || alignString.length() == 0) {
				setAlign((Integer)null);
				return;
			}

			String halign, valign;
			StringTokenizer st = new StringTokenizer(alignString, ":");
			if (st.countTokens() == 2) {
				valign = st.nextToken();
				halign = st.nextToken();
			} else {
				valign = alignString;
				halign = alignString;
			}

			int align;

			if ("left".equalsIgnoreCase(halign)) {
				align = LEFT;
			} else if ("right".equalsIgnoreCase(halign)) {
				align = RIGHT;
			} else {
				align = CENTER;
			}

			if ("top".equalsIgnoreCase(valign)) {
				align |= TOP;
			} else if ("bottom".equalsIgnoreCase(valign)) {
				align |= BOTTOM;
			} else {
				align |= CENTER;
			}

			setAlign(getInteger(align));
		}

		public int getExpand () {
			if (expand == null) return tableConstraints.getExpand();
			return expand.intValue();
		}

		/**
		 * Can be X, Y, X|Y, or NONE.
		 */
		public void setExpand (Integer expand) {
			if (expand == null && this == tableConstraints) return;
			this.expand = expand;
			invalidate();
		}

		public void setExpand (String expandString) {
			if ("x".equalsIgnoreCase(expandString)) {
				setExpand(getInteger(X));
			} else if ("y".equalsIgnoreCase(expandString)) {
				setExpand(getInteger(Y));
			} else if ("xy".equalsIgnoreCase(expandString)) {
				setExpand(getInteger(X | Y));
			} else if ("none".equalsIgnoreCase(expandString)) {
				setExpand(getInteger(NONE));
			} else {
				setExpand((Integer)null);
			}
		}

		public void setFill (String fillString) {
			if ("x".equalsIgnoreCase(fillString))
				setFill(new Integer[] {getInteger(100), ZERO});
			else if ("y".equalsIgnoreCase(fillString))
				setFill(new Integer[] {ZERO, getInteger(100)});
			else if ("xy".equalsIgnoreCase(fillString))
				setFill(new Integer[] {getInteger(100), getInteger(100)});
			else if ("none".equalsIgnoreCase(fillString))
				setFill(new Integer[] {ZERO, ZERO});
			else
				setFill(getIntegerArray(fillString, 2));
		}

		public int[] getIPadding () {
			if (ipadding == null) return tableConstraints.getIPadding();
			return new int[] {ipadding[0].intValue(), ipadding[1].intValue()};
		}

		public void setIPadding (Integer[] ipadding) {
			if (ipadding == null && this == tableConstraints) return;
			this.ipadding = ipadding;
			invalidate();
		}

		public int[] getPadding () {
			if (padding == null) return tableConstraints.getPadding();
			return new int[] {padding[0].intValue(), padding[1].intValue(), padding[2].intValue(), padding[3].intValue()};
		}

		public void setPadding (Integer[] padding) {
			if (padding == null && this == tableConstraints) return;
			this.padding = padding;
			invalidate();
		}

		public int[] getSpacing () {
			if (spacing == null) return tableConstraints.getSpacing();
			return new int[] {spacing[0].intValue(), spacing[1].intValue(), spacing[2].intValue(), spacing[3].intValue()};
		}

		public void setSpacing (Integer[] spacing) {
			if (spacing == null && this == tableConstraints) return;
			this.spacing = spacing;
			invalidate();
		}

		public boolean getBorder () {
			if (border == null) return tableConstraints.getBorder();
			return border.booleanValue();
		}

		public void setBorder (Boolean border) {
			if (border == null && this == tableConstraints) return;
			this.border = border;
			invalidate();
		}

		public int[] getFill () {
			if (fill == null) return tableConstraints.getFill();
			return new int[] {fill[0].intValue(), fill[1].intValue()};
		}

		public void setFill (Integer[] fill) {
			if (fill == null && this == tableConstraints) return;
			this.fill = fill;
			invalidate();
		}

		public String toString () {
			StringBuffer buffer = new StringBuffer(150);

			int align = getAlign();
			String valign = "";
			if ((align & TOP) != 0)
				valign = "top";
			else if ((align & BOTTOM) != 0)
				valign = "bottom";
			else {
				valign = "center";
			}
			String halign = "";
			if ((align & LEFT) != 0)
				halign = "left";
			else if ((align & RIGHT) != 0)
				halign = "right";
			else {
				halign = "center";
			}
			if (valign.equals(halign)) {
				if (!valign.equals("center")) {
					buffer.append("align=");
					buffer.append(valign);
				}
			} else {
				buffer.append("align=");
				buffer.append(valign);
				buffer.append(':');
				buffer.append(halign);
			}

			int expand = getExpand();
			String expandString = "";
			if ((expand & X) != 0) expandString += "x";
			if ((expand & Y) != 0) expandString += "y";
			if (expandString.length() > 0) {
				if (buffer.length() > 0) buffer.append(", ");
				buffer.append("expand=");
				buffer.append(expandString);
			}

			int[] spacing = getSpacing();
			if (spacing[0] != 0 || spacing[1] != 0 || spacing[2] != 0 || spacing[3] != 0) {
				if (buffer.length() > 0) buffer.append(", ");
				buffer.append("spacing=");
				if (spacing[0] == spacing[1] && spacing[1] == spacing[2] && spacing[2] == spacing[3]) {
					buffer.append(spacing[0]);
				} else {
					buffer.append(spacing[0]);
					buffer.append(':');
					buffer.append(spacing[1]);
					buffer.append(':');
					buffer.append(spacing[2]);
					buffer.append(':');
					buffer.append(spacing[3]);
				}
			}

			int[] padding = getPadding();
			if (padding[0] != 0 || padding[1] != 0 || padding[2] != 0 || padding[3] != 0) {
				if (buffer.length() > 0) buffer.append(", ");
				buffer.append("padding=");
				if (padding[0] == padding[1] && padding[1] == padding[2] && padding[2] == padding[3]) {
					buffer.append(padding[0]);
				} else {
					buffer.append(padding[0]);
					buffer.append(':');
					buffer.append(padding[1]);
					buffer.append(':');
					buffer.append(padding[2]);
					buffer.append(':');
					buffer.append(padding[3]);
				}
			}

			int[] ipadding = getIPadding();
			if (ipadding[0] != 0 || ipadding[1] != 0) {
				if (buffer.length() > 0) buffer.append(", ");
				buffer.append("ipadding=");
				if (ipadding[0] == ipadding[1]) {
					buffer.append(ipadding[0]);
				} else {
					buffer.append(ipadding[0]);
					buffer.append(':');
					buffer.append(ipadding[1]);
				}
			}

			int[] fill = getFill();
			if (fill[0] != 0 || fill[1] != 0) {
				if (buffer.length() > 0) buffer.append(", ");
				buffer.append("fill=");
				if (fill[0] == fill[1]) {
					buffer.append(fill[0]);
				} else {
					buffer.append(fill[0]);
					buffer.append(':');
					buffer.append(fill[1]);
				}
			}

			if (getBorder()) {
				if (buffer.length() > 0) buffer.append(", ");
				buffer.append("border=true");
			}

			return buffer.toString();
		}
	}

	/**
	 * Stores constraints for the table and default constraints for the table cells. These defaults are used for all cells that
	 * don't specify a value. This means TableConstraints fields canont be set to null.
	 */
	public class TableConstraints extends Constraints {
		private int columns = 1;
		private int fixed = NONE;

		public TableConstraints () {
			setAlign(getInteger(CENTER));
			setExpand(getInteger(NONE));
			setIPadding(new Integer[] {ZERO, ZERO});
			setFill(new Integer[] {ZERO, ZERO});
			setSpacing(new Integer[] {ZERO, ZERO, ZERO, ZERO});
			setPadding(new Integer[] {ZERO, ZERO, ZERO, ZERO});
			setBorder(Boolean.FALSE);
		}

		public TableConstraints (Map map) {
			this();
			configure(map);
		}

		/**
		 * Any null values in the map will not be set on the TableConstraints.
		 */
		public void configure (Map map) {
			super.configure(map);
			if (map.get("columns") != null) setColumns(Integer.parseInt((String)map.get("columns")));
			if (map.get("uniform") != null) setFixed((String)map.get("colspan"));

			// Legacy constraints.
			if (map.get("columns") == null && map.get("cols") != null) setColumns(Integer.parseInt((String)map.get("cols")));
		}

		public int getColumns () {
			return columns;
		}

		public void setColumns (int columns) {
			this.columns = columns;
			invalidate();
		}

		public int getFixed () {
			return fixed;
		}

		/**
		 * Can be X, Y, X|Y, or NONE.
		 */
		public void setFixed (int fixed) {
			this.fixed = fixed;
			invalidate();
		}

		public void setFixed (String expandString) {
			if ("x".equalsIgnoreCase(expandString)) {
				setFixed(X);
			} else if ("y".equalsIgnoreCase(expandString)) {
				setFixed(Y);
			} else if ("xy".equalsIgnoreCase(expandString)) {
				setFixed(X | Y);
			} else {
				setFixed(NONE);
			}
		}

		public String toString () {
			StringBuffer buffer = new StringBuffer(150);

			buffer.append("columns=");
			buffer.append(getColumns());

			int fixed = getFixed();
			String fixedString = "";
			if ((fixed & X) != 0) fixedString += "x";
			if ((fixed & Y) != 0) fixedString += "y";
			if (fixedString.length() > 0) {
				buffer.append(", uniform=");
				buffer.append(fixedString);
			}

			buffer.append(", ");
			buffer.append(super.toString());

			return buffer.toString();
		}
	}

	/**
	 * Stores constraints for the cells.
	 */
	public class CellConstraints extends Constraints {
		private int colspan = 1, rowspan = 1;

		public CellConstraints () {
		}

		public CellConstraints (Map map) {
			configure(map);
		}

		public void configure (Map map) {
			super.configure(map);
			if (map.get("colspan") != null) setColspan(Integer.parseInt((String)map.get("colspan")));
			if (map.get("rowspan") != null) setRowspan(Integer.parseInt((String)map.get("rowspan")));
		}

		public int getRowspan () {
			return rowspan;
		}

		public void setRowspan (int rowspan) {
			if (rowspan <= 0) throw new IndexOutOfBoundsException("rowspan cannot be less than 1.");
			this.rowspan = rowspan;
			invalidate();
		}

		public int getColspan () {
			return colspan;
		}

		public void setColspan (int colspan) {
			if (colspan <= 0) throw new IndexOutOfBoundsException("colspan cannot be less than 1.");
			this.colspan = colspan;
			invalidate();
		}

		public String toString () {
			StringBuffer buffer = new StringBuffer(150);

			buffer.append("colspan=");
			buffer.append(getColspan());

			buffer.append(", rowspan=");
			buffer.append(getRowspan());

			buffer.append(", ");
			buffer.append(super.toString());

			return buffer.toString();
		}
	}

	/**
	 * Panel for drawing grid borders.
	 */
	private class BorderPanel extends JPanel {
		public BorderPanel () {
			setOpaque(false);
		}

		public void paint (Graphics g) {
			Container target = getParent();
			for (int i = 0, n = target.getComponentCount(); i < n; i++) {
				Component comp = target.getComponent(i);
				if (comp instanceof BorderPanel) continue;
				CellConstraints constraints = (CellConstraints)cellConstraints.get(comp);
				if (!constraints.getBorder()) continue;
				int colspan = constraints.getColspan();
				int rowspan = constraints.getRowspan();
				int gridX = gridPosition_x[i];
				int gridY = gridPosition_y[i];
				int x = columnLeft[gridX];
				int y = rowTop[gridY];
				int width = columnLeft[gridX + colspan] - x;
				int height = rowTop[gridY + rowspan] - y;
				g.drawRect(x, y, width, height);
			}
		}
	}
}
