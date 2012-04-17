package org.pnuts.jmx;

import pnuts.lang.Package;
import pnuts.lang.PnutsFunction;
import pnuts.lang.Context;
import java.util.*;
import javax.management.*;

/*
 * functions in a map represents JMX operations
 * non-function objects in a map represents JMX attributes
 */
public class DynamicMBeanFactory  {

    public static DynamicMBean create(Map/*<String,Object>*/ map, Context context){
	return create(map, null, context);
    }

    public static DynamicMBean create(Map/*<String,Object>*/ map, Set monitoredVars, Context context){
	return new MBean(map, monitoredVars, context);
    }

    static class MBean extends NotificationBroadcasterSupport implements DynamicMBean {
	private Map/*<String,Object>*/ map;
	private Context context;
	private String description;
	private Set monitoredVars;
	private long sequenceNumber = 0L;

	MBean(Map/*<String,Object>*/ map, Set monitoredVars, Context context){
	    this.map = map;
	    this.context = context;
	    this.monitoredVars = monitoredVars;
	}

	public Object getAttribute(String attribute)
	    throws AttributeNotFoundException,
		   MBeanException,
		   ReflectionException
	{
	    Object value = map.get(attribute);
	    if (value != null && !(value instanceof PnutsFunction)){
		return value;
	    }
	    throw new AttributeNotFoundException();
	}

	public void setAttribute(Attribute attribute)
	    throws AttributeNotFoundException,
		   InvalidAttributeValueException,
		   MBeanException,
		   ReflectionException
	{
	    String name = attribute.getName();
	    Object newValue = attribute.getValue();
	    Object oldValue = map.put(name, newValue);
	    if (oldValue == null || !oldValue.equals(newValue)){
		checkMonitoredVars(name, oldValue, newValue);
	    }
	}

	public AttributeList getAttributes(String[] attributes)
	{
	    AttributeList list = new AttributeList();
	    for (int i = 0, len = attributes.length; i < len; i++){
		String name = attributes[i];
		Object value = map.get(name);
		if (value != null && !(value instanceof PnutsFunction)){
		    list.add(new Attribute(name, value));
		}
	    }
	    return list;
	}

	public AttributeList setAttributes(AttributeList attributes)
	{
	    AttributeList list = new AttributeList();
//	    for (Attribute attr : attributes){
	    for (Iterator it = attributes.iterator(); it.hasNext();){
		Attribute attr = (Attribute)it.next();
		String name = attr.getName();		
		Object newValue = attr.getValue();
		Object oldValue = map.put(name, newValue);
		if (oldValue == null || !oldValue.equals(newValue)){
		    list.add(attr);
		    checkMonitoredVars(name, oldValue, newValue);
		}
	    }
	    return list;
	}

	public Object invoke(String actionName, Object params[], String signature[])
	    throws MBeanException, ReflectionException
	{
	    try {
		Object value = map.get(actionName);

		if (value instanceof PnutsFunction){
		    PnutsFunction func = (PnutsFunction)value;
		    try {
			return func.call(params, context);
		    } catch (Exception e){
			throw new MBeanException(e);
		    }
		}
	    } catch (Exception e){
		throw new ReflectionException(e);
	    }
	    return null;
	}

	protected void checkMonitoredVars(String name, Object oldValue, Object newValue){
	    if (monitoredVars != null && monitoredVars.contains(name)){
		Notification n =
		    new AttributeChangeNotification(this,
						    sequenceNumber++,
						    System.currentTimeMillis(),
						    "",
						    name,
						    "java.lang.Object",
						    oldValue,
						    newValue);
		sendNotification(n);
	    }
	}

	public void setDescription(String description){
	    this.description = description;
	}

	private MBeanAttributeInfo[] attributeInfo(){
	    ArrayList/*<MBeanAttributeInfo>*/ list = new ArrayList/*<MBeanAttributeInfo>*/();
//	    for (Map.Entry<String,Object> entry : map.entrySet()){
	    for (Iterator it = map.entrySet().iterator(); it.hasNext();){
		Map.Entry entry = (Map.Entry)it.next();
		String name = (String)entry.getKey();
		Object value = entry.getValue();
		if (!(value instanceof PnutsFunction)){
		    list.add(new MBeanAttributeInfo(name,
						    "java.lang.Object",
						    null,
						    true,
						    true,
						    false));
		}
	    }
	    return (MBeanAttributeInfo[])list.toArray(new MBeanAttributeInfo[list.size()]);
	}

	private MBeanOperationInfo[] operationInfo(){
	    ArrayList/*<MBeanOperationInfo>*/ list = new ArrayList/*<MBeanOperationInfo>*/();
//	    for (Map.Entry<String,Object> entry : map.entrySet()){
	    for (Iterator it = map.entrySet().iterator(); it.hasNext();){
		Map.Entry entry = (Map.Entry)it.next();
		String name = (String)entry.getKey();
		Object value = entry.getValue();
		if (value instanceof PnutsFunction){
		    PnutsFunction func = (PnutsFunction)value;
		    list.add(new MBeanOperationInfo(name,
						    null,
						    new MBeanParameterInfo[0],
						    "java.lang.Object",
						    MBeanOperationInfo.UNKNOWN));
		}
	    }
	    return (MBeanOperationInfo[])list.toArray(new MBeanOperationInfo[list.size()]);
	}

	public MBeanInfo getMBeanInfo(){
	    MBeanAttributeInfo[] attributes = attributeInfo();
	    MBeanConstructorInfo[] constructors = null;
	    MBeanOperationInfo[] operations = operationInfo();
	    MBeanNotificationInfo[] notifications = null;

	    return new MBeanInfo(this.getClass().getName(),
				 description,
				 attributes,
				 constructors,
				 operations,
				 notifications);
	}

    }
}
