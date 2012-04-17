/*
 * AttributeMap.java
 */

package pnuts.xml;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 *
 */
public class DOMAttributeMap extends AbstractMap {
	private NamedNodeMap map;
	private Element element;
	private transient Set entrySet = null;
	transient int size;
	
	/**
	 * Constructor
	 */
	public DOMAttributeMap(Element element, NamedNodeMap map) {
		this.element = element;
		this.map = map;
	}
	
	public Set entrySet(){
	        Set es = entrySet;
		return (es != null ? es : (entrySet = (Set) new EntrySet()));
	}

	public Object get(Object key) {
		Node item = getNamedItem((String)key);
		if (item instanceof Attr){
			return ((Attr)item).getValue();
		}
		return null;
	}
	
	public Object put(Object key, Object value){
		if (!(key instanceof String)){
			throw new IllegalArgumentException(String.valueOf(key));
		}
		if (!(value instanceof String)){
			throw new IllegalArgumentException(String.valueOf(value));
		}
		String name = (String)key;
		Node old = map.getNamedItem(name);
		if (old instanceof Attr){
			((Attr)old).setValue((String)value);
		} else {
			if (element != null){
				element.setAttribute(name, (String)value);
			}
		}
		return old;
	}
	
	public Object remove(Object key){
		return map.removeNamedItem((String)key);
	}
	
	Node getNamedItem(String name){
		return map.getNamedItem(name);
	}
	
	private class AttrEntry implements Map.Entry {
		private Attr attr;

		AttrEntry(Attr attr){
			this.attr = attr;
		}
		public Object getKey(){
			return attr.getName();
		}
		public Object getValue(){
			return attr.getValue();
		}
		public Object setValue(Object value){
			Object old = attr.getValue();
			attr.setValue((String)value);
			return old;
		}
		public int hashCode(){
			return attr.hashCode();
		}
		
		public boolean equals(Object obj){
			if (obj instanceof AttrEntry){
				return attr.equals(((AttrEntry)obj).attr);
			}
			return false;
		}
		public String toString(){
			return getKey() + "=" + getValue();
		}
	}
	
	private class AttrIterator implements Iterator {
		int idx = 0;
		int len = map.getLength();
		Map.Entry nextEntry(){
			Attr attr = (Attr)map.item(idx++);
			return new AttrEntry(attr);
		}
		public Object next() {
			return nextEntry();
		}		
		public boolean hasNext(){
			return idx < len;
		}
		public void remove(){
			Attr attr = (Attr)map.item(idx);
			map.removeNamedItem(attr.getName());
		}
	}

	private class EntrySet extends AbstractSet {
		public Iterator iterator() {
			return new AttrIterator();
		}
		public boolean contains(Object o) {
			if (!(o instanceof AttrEntry))
				return false;
			AttrEntry e = (AttrEntry) o;
			Object key = e.getKey();
			Object value = e.getValue();
			if (!(key instanceof String)){
				return false;
			}
			if (!(value instanceof String)){
				return false;
			}
			if (value == null){
				return getNamedItem((String)key) == null;
			} else {
				return value.equals(getNamedItem((String)key));
			}
		}
		public boolean remove(Object o) {
			if (!(o instanceof AttrEntry)){
				return false;
			}
			AttrEntry e = (AttrEntry)o;
			Object key = e.getKey();
			Object value = e.getValue();
			Node item = getNamedItem((String)key);
			if (item instanceof Attr){
				Attr attr = (Attr)item;
				if (value == null){
					if (attr.getValue() == null){
						map.removeNamedItem((String)key);
						return true;
					}
				} else {
					if (value.equals(attr.getValue())){
						map.removeNamedItem((String)key);
						return true;
					}
				}
			}
			return false;
		}
		
		public int size() {
			return size;
		}
		
		public void clear() {
			DOMAttributeMap.this.clear();
		}
	}

}
