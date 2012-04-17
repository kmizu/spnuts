/*
 * @(#)Builtin.java 1.4 05/05/25
 *
 * Copyright (c) 1997-2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.lang;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import org.pnuts.util.*;

/**
 * Builtin functions
 */
final class Builtin extends PnutsFunction {

        private final static boolean requireLatestScript = "true".equalsIgnoreCase(Runtime.getProperty("pnuts.lang.requireLatestScript"));    
    
	protected Builtin(String name) {
		super(name);
	}

	protected Object exec(Object[] args, Context context) {
		if (name == _getContext) {
			if (args.length == 0) {
				return context;
			}
		} else if (name == _package) {
			if (args.length == 1) {
				Package pkg;
				if (args[0] instanceof Package) {
					pkg = (Package) args[0];
				} else if (args[0] != null) {
					pkg = Package.getPackage((String) args[0], context);
				} else {
					pkg = Package.getInstance(null,
							context.currentPackage.root, context);
				}
				context.setCurrentPackage(pkg);
				return null;
			} else if (args.length == 0) {
				return context.currentPackage;
			}
		} else if (name == _import) {
			if (args.length == 1) {
				if (args[0] == null) {
					context.importEnv = new ImportEnv();
					return null;
				} else {
					String s = (String) args[0];
					if (!s.endsWith("*")) {
						context.addClassToImport(s);
					} else {
						int idx = s.lastIndexOf('.');
						if (idx > 0) {
							context.addPackageToImport(s.substring(0, idx));
						} else {
							context.addPackageToImport("");
						}
					}
					return null;
				}
			} else if (args.length == 0) {
				return context.importEnv.list();
			}
		} else if (name == _throw) {
			if (args.length == 1) {
				Object arg = args[0];
				if (arg instanceof PnutsException) {
					throw (PnutsException) arg;
				} else if (arg instanceof Throwable) {
					throw new PnutsException((Throwable) arg, context);
				} else {
					throw new PnutsException(arg.toString(), context);
				}
			}
		} else if (name == _defined) {
			if (args.length == 1) {
				return Boolean.valueOf(context.defined((String) args[0]));
			}
		} else if (name == _quit) {
			if (args.length == 0) {
				throw new Escape();
			} else if (args.length == 1) {
				throw new Escape(args[0]);
			}
		} else if (name == _eval) {
			Context c = null;
			if (args.length == 1) {
//				c = (Context) ((Context) context).clone(false, true);
			        c = context;
//				c.eval = true;
				if (c.stackFrame != null){
				    Cell prev = c.evalFrameStack;
				    Cell cell = new Cell();
				    c.evalFrameStack = cell;
				    cell.object = c.stackFrame;
				    cell.next = prev;
				}
				c.stackFrame = new StackFrame();
				return Pnuts.eval((String) args[0], c);
			} else if (args.length == 2) {
				if (args[1] instanceof Context) {
					c = (Context) ((Context) args[1]).clone(false, true);
				} else if (args[1] instanceof Package) {
					c = (Context) context.clone(false, true);
					c.currentPackage = (Package) args[1];
				} else {
					c = (Context) context.clone(false, true);
					c.currentPackage = Package.getPackage((String) args[1],
							context);
				}
//				c.eval = true;
				return c.pnutsImpl.eval((String) args[0], c);
			}
		} else if (name == _loadFile) {
			try {
				Context c = null;
				Object arg0 = args[0];
				if (args.length > 0) {
					if (args.length > 1) {
						c = (Context) ((Context) args[1]).clone(false, true);
					} else {
						c = (Context) context.clone();
					}
					if (arg0 instanceof File) {
						return Pnuts.loadFile(((File) arg0).getPath(), c);
					} else {
						return Pnuts.loadFile((String) arg0, c);
					}
				}
			} catch (FileNotFoundException e) {
				throw new PnutsException(e, context);
			}
		} else if (name == _load) {
			try {
				Context c = null;
				if (args.length > 0) {
					if (args.length > 1) {
						c = (Context) args[1];
						c = (Context) c.clone(false, true);
					} else {
						c = (Context) context.clone();
					}
					if (args[0] instanceof InputStream) {
						return Pnuts.load((InputStream) args[0], c);
					} else if (args[0] instanceof Reader) {
						return Pnuts.load((Reader) args[0], c);
					} else if (args[0] instanceof URL) {
						return Pnuts.load((URL) args[0], c);
					} else {
						return Pnuts.load((String) args[0], c);
					}
				}
			} catch (FileNotFoundException e) {
				throw new PnutsException(e, context);
			}
		} else if (name == _autoload) {
			if (args.length == 2) {
				context.currentPackage.autoload((String) args[0],
						(String) args[1], context);
				return null;
			}
		} else if (name == _require) {
                    try {
                        if (args.length == 1) {
                            Context c = (Context) context.clone();
                            Pnuts.require((String) args[0], c, requireLatestScript);
                            return null;
                        }
                    } catch (FileNotFoundException e) {
                        throw new PnutsException(e, context);
                    }
		} else if (name == _class) {
			if (args.length == 1) {
				Object arg = args[0];
				if (arg instanceof Class){
					return arg;
				} else {
					try {
						Class cls = Pnuts.loadClass((String) arg, context);
						cls.getClass(); // initialize the class
						return cls;
					} catch (ClassNotFoundException e) {
						return null;
					}
				}
			}
		} else if (name == _use) {
			if (args.length == 0) {
				return context.usedPackages();
			} else if (args.length == 1) {
				Object arg0 = args[0];
				if (arg0 == null) {
					context.clearPackages();
					return null;
				} else if (arg0 instanceof Package) {
					boolean ret = context.usePackage((Package) arg0, false);
					return (ret ? Boolean.TRUE : Boolean.FALSE);
				} else if (arg0 instanceof String) {
					boolean ret = context.usePackage((String) arg0, false);
					return (ret ? Boolean.TRUE : Boolean.FALSE);
				}
			}
		} else if (name == _unuse) {
			if (args.length == 1) {
				Object arg0 = args[0];
				Package pkg = null;
				if (arg0 instanceof String) {
					pkg = Package.getPackage((String) arg0, context);
				} else if (arg0 instanceof Package) {
					pkg = (Package) arg0;
				}
				if (pkg != null) {
					boolean ret = context.unusePackage(pkg);
					return (ret ? Boolean.TRUE : Boolean.FALSE);
				}
			}
		}
		throw new PnutsException("function.notDefined", new Object[] { name,
				new Integer(args.length) }, context);
	}

	public String toString() {
		return "<builtin " + name + ">";
	}

	public String unparse(int narg) {
		return null;
	}

	public String[] getImportEnv(int narg) {
		return null;
	}

	public boolean isBuiltin() {
		return true;
	}
}
