/*
 * @(#)ColumnMap.java 1.2 04/12/06
 *
 * Copyright (c) 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.jdbc;

import java.sql.*;
import java.util.*;
import pnuts.lang.Property;
import pnuts.lang.Indexed;

/*
 * A poor man's O/R mapper
 */
public class ColumnMap extends AbstractMap implements Indexed {

	protected ResultSet rs;
	private String[] columnNames;
	private HashSet columnNameSet;
        private boolean clobToStringConversion = true;
        private boolean updated;
        
	protected ColumnMap(){
	}

	public ColumnMap(ResultSet rs) throws SQLException {
		this.rs = rs;
		initialize(rs.getMetaData());
	}
	
	void initialize(ResultSetMetaData meta) throws SQLException {
		int columnCount = meta.getColumnCount();
		columnNameSet = new HashSet();
		columnNames = new String[columnCount];
		for (int i = 0; i < columnCount; i++){
			String name = meta.getColumnName(i + 1);
			name = name.toLowerCase();
			columnNames[i] = name;
			columnNameSet.add(name);
		}
	}

        public void setClobToStringConvertion(boolean flag){
            this.clobToStringConversion = flag;
        }
        
        public boolean getClobToStringConvertion(){
            return this.clobToStringConversion;
        }
        
	public boolean next() throws SQLException {
                if (updated){
                    rs.updateRow();
                    updated = false;
                }
		return rs.next();
	}

	public void set(int idx, Object value) {
		try {
			rs.updateObject(idx + 1, value);
		} catch (SQLException e){
			handleSQLException(e);
		}
                updated = true;
	}

	public Object get(int idx) {
		try {
                        return unwrap(rs.getObject(idx + 1));
		} catch (SQLException e){
			handleSQLException(e);
			return null;
		}
	}

	public int size(){
		return columnNames.length;
	}

	public boolean isEmpty(){
		return size() == 0;
	}

	public boolean containsKey(Object key) {
		return columnNameSet.contains(key);
	}

	public boolean containsValue(Object value){
		return false;
	}

	public Object get(Object key) {
		try {
                        return unwrap(rs.getObject(((String)key).toLowerCase()));
		} catch (SQLException e){
			handleSQLException(e);
			return null;
		}
	}

	public Object put(Object key, Object value) {
		try {
			rs.updateObject(((String)key).toLowerCase(), value);
		} catch (SQLException e){
			handleSQLException(e);
		}
                updated = true;
		return null;
	}

	public Object remove(Object key){
		throw new UnsupportedOperationException();
	}

	public void putAll(Map t){
		throw new UnsupportedOperationException();
	}

	public void clear(){
		throw new UnsupportedOperationException();
	}

	public Set keySet(){
		return columnNameSet;
	}

	public Collection values(){
		throw new UnsupportedOperationException();
	}

	public Set entrySet(){
		return new EntrySet();
	}

	protected void handleSQLException(SQLException e){
		throw new RuntimeException(String.valueOf(e));
	}
        
        private Object unwrap(Object x) throws SQLException {
            if (clobToStringConversion){
                if (x instanceof Clob){
                    Clob clob = (Clob)x;
                    long len = clob.length();
                    if (len < 0 || len > Integer.MAX_VALUE){
                        throw new RuntimeException("Can't convert to String");
                    }
		      if (len == 0){
			  return "";
		      } else {
			  return clob.getSubString(1L, (int)len);
		      }
                }
            }
            return x;
        }

	private class EntryIterator implements Iterator {
		int pos = 0;

		public boolean hasNext(){
			return columnNames.length > pos;
		}

		public Object next(){
			final int p = pos;
			pos++;
			return new Map.Entry(){
					public Object getKey(){
						return columnNames[p];
					}
					public Object getValue(){
						try {
                                                       return unwrap(rs.getObject(p + 1));
						} catch (SQLException e){
							handleSQLException(e);
							return null;
						}
					}
					public Object setValue(Object value){
						try {
							rs.updateObject(p + 1, value);
							updated = true;
                                                        return null;
						} catch (SQLException e){
							handleSQLException(e);
							return null;
						}
					}
					public String toString() {
						return getKey() + "=" + getValue();
					}
				};
		}

		public void remove(){
			throw new UnsupportedOperationException();
		}
	}

	private class EntrySet extends AbstractSet {
		public Iterator iterator() {
			return new EntryIterator();
		}

		public boolean contains(Object o) {
			if (!(o instanceof Map.Entry)){
				return false;
			}
			Map.Entry e = (Map.Entry)o;
			for (Iterator it = iterator(); it.hasNext();){
				if (it.next().equals(e)){
					return true;
				}
			}
			return false;
		}

		public boolean remove(Object o) {
			throw new UnsupportedOperationException();
		}

		public int size() {
			return columnNames.length;
		}

		public void clear() {
			throw new UnsupportedOperationException();
		}
	}
}
