/*
 * TigerConfiguration.java
 */

package pnuts.lang;

import java.util.Enumeration;
import java.util.Iterator;
import java.math.BigDecimal;
/**
 *
 */
class TigerConfiguration extends MerlinConfiguration {
	
	/**
	 * Constructor
	 */
	TigerConfiguration() {
	}
	
	TigerConfiguration(Class stopClass){
		super(stopClass);
	}
	
       BigDecimal longToBigDecimal(long lval){
	   return new BigDecimal(lval);
       }

	public Enumeration toEnumeration(Object obj) {
		Enumeration en = super.toEnumeration(obj);
		if (en == null) 		
			if (obj instanceof Iterable){
				Iterator itr = ((Iterable)obj).iterator();
				en = new Java2Configuration.ItrEnum(itr);
		}
		return en;
	}
}
