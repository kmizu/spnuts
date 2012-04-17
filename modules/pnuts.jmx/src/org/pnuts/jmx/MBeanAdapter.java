package org.pnuts.jmx;

import pnuts.lang.*;
import java.util.*;
import java.io.*;
import javax.management.*;

public class MBeanAdapter implements AbstractData {
    private MBeanServerConnection server;
    private ObjectName name;
    private MBeanInfo beanInfo;
    private Map/*<String,String[]>*/ operations = new HashMap/*<String,String[]>*/();

    public MBeanAdapter(MBeanServerConnection server, String name)
	throws JMException, IOException
    {
	this(server, new ObjectName(name));
    }

    public MBeanAdapter(MBeanServerConnection server, ObjectName name)
	throws JMException, IOException
    {
	this.server = server;
	this.name = name;
	this.beanInfo = server.getMBeanInfo(name);
        MBeanOperationInfo[] operationInfos = beanInfo.getOperations();
        for (int i = 0; i < operationInfos.length; i++) {
            MBeanOperationInfo info = operationInfos[i];
            String signature[] = createSignature(info);
            String operationKey = createOperationKey(info.getName(), signature.length);
            operations.put(operationKey, signature);
        }

    }

    public void set(String property, Object value, Context context){
        try {
            server.setAttribute(name, new Attribute(property, value));
        } catch (MBeanException e) {
            throw new RuntimeException("Could not set property: " + property + ". Reason: " + e, e.getTargetException());
        } catch (Exception e) {
            throw new RuntimeException("Could not set property: " + property + ". Reason: " + e, e);
        }
    }

    public Object get(String property, Context context){
        try {
            return server.getAttribute(name, property);
        } catch (MBeanException e) {
            throw new RuntimeException("Could not access property: " + property + ". Reason: " + e, e.getTargetException());
        } catch (Exception e) {
            throw new RuntimeException("Could not access property: " + property + ". Reason: " + e, e);
        }
    }

    public Object invoke(String method, Object[] arguments, Context context) {
        String operationKey = createOperationKey(method, arguments.length);
        String[] signature = (String[]) operations.get(operationKey);
        if (signature != null) {
            try {
                return server.invoke(name, method, arguments, signature);
            } catch (MBeanException e) {
                throw new RuntimeException("Could not invoke method: " + method + ". Reason: " + e, e.getTargetException());
            } catch (Exception e) {
                throw new RuntimeException("Could not invoke method: " + method + ". Reason: " + e, e);
            }
        }
	throw new RuntimeException("no such operation");
    }

    protected String[] createSignature(MBeanOperationInfo info) {
        MBeanParameterInfo[] params = info.getSignature();
        String[] answer = new String[params.length];
        for (int i = 0; i < params.length; i++) {
            answer[i] = params[i].getType();
        }
        return answer;
    }

    protected String createOperationKey(String operation, int params) {
        return operation + "_" + params;
    }

    public static MBeanInfo getMBeanInfo(MBeanServerConnection server, ObjectName name)
	throws InstanceNotFoundException, IntrospectionException, ReflectionException, IOException
    {
	return server.getMBeanInfo(name);
    }
    
    public static MBeanInfo getMBeanInfo(MBeanServerConnection server, String name)
	throws JMException, IOException 
    {
	return server.getMBeanInfo(new ObjectName(name));
    }
    
    public String toString(){
	return name.getCanonicalName();
    }
}
