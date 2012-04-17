package pnuts.security;

import java.security.*;
import java.util.Enumeration;

public class FilterPermissions extends PermissionCollection {

    private PermissionCollection base;
    private PermissionCollection notPermitted;

    public FilterPermissions(PermissionCollection base, PermissionCollection notPermitted){
	this.base = base;
	this.notPermitted = notPermitted;
    }

    public FilterPermissions(PermissionCollection base, Permission notPermitted){
	this.base = base;
	PermissionCollection col = new Permissions();
	col.add(notPermitted);
	this.notPermitted = col;
    }

    public void add(Permission permission) {
	base.add(permission);
    }

    public boolean implies(Permission permission){
	if (notPermitted.implies(permission)){
	    return false;
	}
	return base.implies(permission);
    }

    public Enumeration elements() {
	return base.elements();
    }

    public String toString(){
	StringBuffer sbuf = new StringBuffer();
	sbuf.append(getClass().getName());
	sbuf.append("@");
	sbuf.append(hashCode());
	sbuf.append("(\n");
	for (Enumeration e = base.elements(); e.hasMoreElements();){
	    sbuf.append(e.nextElement());
	    sbuf.append("\n");
	}
	sbuf.append("\n");
	for (Enumeration e = notPermitted.elements(); e.hasMoreElements();){
	    sbuf.append(e.nextElement());
	    sbuf.append("\n");
	}
	sbuf.append(")");
	return sbuf.toString();
    }
}
