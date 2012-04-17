/*
 * @(#)PackagePermission.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.security;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.security.BasicPermission;
import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * This class represents access to a Package in Pnuts. A PackagePermission consists
 * of a package name and a set of actions.
 *
 * @see <a href="../../../doc/permission.html">Pnuts User's Guide</a>
 * @version 1.1
 */
public final class PackagePermission extends BasicPermission {

	private static final int WRITE = 1;
	private static final int ADD = 2;
	private static final int REMOVE = 4;

	private transient int mask = 0;
	private transient String actions;
	private transient boolean wildcard = false;
	private transient String path;

	public PackagePermission(String name){
		super(name);
	}

	public PackagePermission(String name, String actions){
		super(name, actions);
		this.actions = actions;
		init(actions);
	}

	void init(String actions){
		if (actions.indexOf("write") >= 0){
			mask |= WRITE;
		}
		if (actions.indexOf("add") >= 0){
			mask |= ADD;
		}
		if (actions.indexOf("remove") >= 0){
			mask |= REMOVE;
		}
		String name = getName();
		if (name.endsWith("::*")){
			wildcard = true;
			path = name.substring(0, name.length() - 2);
		} else if (name.endsWith(".*")){
			wildcard = true;
			path = name.substring(0, name.length() - 1);
		} else if (name.equals("*")) {
			wildcard = true;
			path = "";
			path = name.substring(0, name.length() - 2);
		} else {
			path = name;
		}
	}

	public String getActions(){
		String s = "";
		boolean first = true;
		if ((mask & WRITE) != 0){
			s += "write";
			first = false;
		}
		if ((mask & ADD) != 0){
			if (!first){
				s += ", ";
			}
			s += "add";
			first = false;
		}
		if ((mask & REMOVE) != 0){
			if (!first){
				s += ", ";
			}
			s += "remove";
		}
		return s;
	}

	public boolean implies(Permission p) {
		if (!(p instanceof PackagePermission)){
			return false;
		}
		PackagePermission pp = (PackagePermission)p;
		int m = pp.getMask();
		if ((m & this.mask) != m){
			return false;
		}

		if (this.wildcard) {
			if (pp.wildcard){
				return pp.path.startsWith(path);
			} else {
				return (pp.path.length() > this.path.length()) && pp.path.startsWith(this.path);
			}
		} else {
			if (pp.wildcard) {
				return false;
			} else {
				return this.path.equals(pp.path);
			}
		}
	}

	int getMask() {
		return mask;
	}

	public PermissionCollection newPermissionCollection() {
		return new PackagePermissionCollection();
	}

	private void readObject(ObjectInputStream s)
		throws IOException, ClassNotFoundException
		{
			s.defaultReadObject();
			init(actions);
		}
}


final class PackagePermissionCollection
	extends PermissionCollection implements Serializable
{
	private Hashtable permissions;
	private boolean all_allowed;

	public PackagePermissionCollection() {
		permissions = new Hashtable(10);
		all_allowed = false;
	}

	public void add(Permission permission){
		if (! (permission instanceof PackagePermission)){
			throw new IllegalArgumentException("invalid permission: "+ permission);
		}
		PackagePermission pp = (PackagePermission)permission;
		permissions.put(pp.getName(), permission);
		if (!all_allowed) {
			if (pp.getName().equals("*")){
				all_allowed = true;
			}
		}
	}

	public boolean implies(Permission permission){
		if (!(permission instanceof PackagePermission)){
			return false;
		}

		PackagePermission pp = (PackagePermission)permission;
		if (all_allowed){
			return true;
		}
		String path = pp.getName();
		Permission x = (Permission) permissions.get(path);
		if (x != null) {
			return x.implies(permission);
		}

		int offset = path.length() - 1;

		while (true){
			int last1 = path.lastIndexOf("::", offset);
			int last2 = path.lastIndexOf(".", offset);
			if (last1 == -1 && last2 == -1){
				break;
			}
			int last;
			if (last2 > last1){
				last = last2;
			} else {
				last = last1;
			}
			path = path.substring(0, last + 1) + "*";
			x = (Permission) permissions.get(path);
			if (x != null) {
				return x.implies(permission);
			}
			offset = last - 1;
		}
		return false;
	}

	public Enumeration elements(){
		return permissions.elements();
	}
}
