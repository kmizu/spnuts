/*
 * @(#)PnutsCompiler.java 1.4 05/06/02
 *
 * Copyright (c) 1997-2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.tools;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import java.net.URL;
import java.net.MalformedURLException;

import pnuts.compiler.ClassFile;
import pnuts.compiler.Compiler;
import pnuts.compiler.Constants;
import pnuts.compiler.FileWriterHandler;
import pnuts.compiler.Opcode;
import pnuts.compiler.ZipWriterHandler;
import pnuts.lang.ParseException;
import pnuts.lang.Pnuts;

/**
 * A batch compiler for Pnuts. It reads Pnuts scripts and generates class files
 * or a JAR file.
 * 
 * <pre>
 * 
 *  Usage:
 *     pnutsc [ -d destination_directory ] [ -o jar_file ] [ -v ] [ -no_proxy ] [ -prefix name ] [ -main className ] [ -m module ] [ -impl pnutsImplClassName ] [ -encoding encoding_name ] [ -C dir ] { script_file | jar_file } ...
 *  
 * </pre>
 */
public class PnutsCompiler {

	private boolean verbose = false;
	private boolean useDynamicProxy = true;
	private boolean includeMain = false;
	private boolean includeLineNumber = true;

	static String file_sep = System.getProperty("file.separator");
	static String prefix;

	public PnutsCompiler() {
	}

	/**
	 * Set verbose mode
	 * 
	 * @param flag
	 *            If true, verbose message is printed
	 */
	public void setVerbose(boolean flag) {
		this.verbose = flag;
	}

	/**
	 * @param flag
	 *            If true, main() method is generated.
	 */
	public void includeMainMethod(boolean flag) {
		this.includeMain = flag;
	}

	public void includeLineNumber(boolean flag) {
		this.includeLineNumber = flag;
	}

	Compiler getCompiler(String name) {
		Compiler compiler = new Compiler(name, false, useDynamicProxy);
		compiler.includeMainMethod(includeMain);
		compiler.includeLineNo(includeLineNumber);
		return compiler;
	}

	/**
	 * Switch dynamic proxy generation
	 * 
	 * @param flag
	 *            If true dynamic proxy is generated for every
	 *            method/constructor call.
	 */
	public void useDynamicProxy(boolean flag) {
		useDynamicProxy = flag;
	}

	/**
	 * Set the prefix of the class name.
	 * 
	 * @param prefix
	 *            the prefix. Default is null.
	 */
	public static void setClassPrefix(String p) {
		prefix = p;
	}

	/**
	 * Compile a parsed expression and save the compiled code to a Zip file
	 * 
	 * @param p
	 *            a parsed expression
	 * @param name
	 *            the class name of the compiled code
	 * @param zout
	 *            a ZipOutputStream to which the compiled code is written
	 */
	public void compileToZip(Pnuts p, String name, ZipOutputStream zout) {
		ZipWriterHandler handler = new ZipWriterHandler(zout);
		if (verbose) {
			handler.setVerbose(true);
		}
		getCompiler(name).compile(p, handler);
	}

	/**
	 * Compile a parsed expression and save the compiled code to class files.
	 * 
	 * @param p
	 *            a parsed expression
	 * @param name
	 *            the class name of the compiled code
	 * @param dir
	 *            the directory in which the class files are saved
	 */
	public void compileToFile(Pnuts p, String name, File dir) {
		FileWriterHandler handler = new FileWriterHandler(dir);
		if (verbose) {
			handler.setVerbose(true);
		}
		getCompiler(name).compile(p, handler);
	}

	void compileToZip(File file, ZipOutputStream zout, Vector classNames,
			String encoding) throws IOException, ParseException {
		Reader reader;
		String fileName = file.getName();
		int idx = fileName.lastIndexOf('.');
		if (idx > 0) {
			String ext = fileName.substring(idx + 1).toLowerCase();
			if ("jar".equals(ext) || "zip".equals(ext)) {
				ZipFile zfile = new ZipFile(file);
				for (Enumeration e = zfile.entries(); e.hasMoreElements();) {
					ZipEntry entry = (ZipEntry) e.nextElement();
					String entryName = entry.getName();
					if (entryName.endsWith(".pnut")) {
						if (verbose) {
							System.out.println(entryName);
						}
						InputStream in = zfile.getInputStream(entry);
						if (encoding == null) {
							reader = new InputStreamReader(in);
						} else {
							reader = new InputStreamReader(in, encoding);
						}
						Pnuts p = Pnuts.parse(reader);
						try {
							p.setScriptSource(new URL("jar:" + file.toURL() + "!/" + entryName));
						} catch (MalformedURLException mue){
							// skip
						}
						String className = className(entryName);
						compileToZip(p, className, zout);
						classNames.addElement(className);
					} else if (entryName.endsWith(".pnc")){
						if (verbose) {
							System.out.println(entryName);
						}
						InputStream in = zfile.getInputStream(entry);
						if (encoding == null) {
							reader = new InputStreamReader(in);
						} else {
							reader = new InputStreamReader(in, encoding);
						}
						ZipWriterHandler handler = new ZipWriterHandler(zout);
						if (verbose) {
						    handler.setVerbose(true);
						}
						URL url = new URL("jar:" + file.toURL() + "!/" + entryName);
						String className = className(entryName);
						getCompiler(className).compileClassScript(reader, url, handler);
					}
				}
				return;
			} else if ("pnc".equals(ext)){
			    InputStream in = new FileInputStream(file);
			    if (encoding == null) {
				reader = new InputStreamReader(in);
			    } else {
				reader = new InputStreamReader(in, encoding);
			    }
			    ZipWriterHandler handler = new ZipWriterHandler(zout);
			    if (verbose) {
				handler.setVerbose(true);
			    }
			    URL url = file.toURL();
			    String className = className(fileName);
			    getCompiler(className).compileClassScript(reader, url, handler);
			    return;
			}
		}
		if (encoding == null) {
			reader = new FileReader(file);
		} else {
			reader = new InputStreamReader(new FileInputStream(file), encoding);
		}
		Pnuts pn = Pnuts.parse(reader);
		pn.setScriptSource(file.toURL());
		String className = className(fileName);
		compileToZip(pn, className, zout);
		classNames.addElement(className);
	}

	void compileToFile(File file, File dir, Vector classNames, String encoding)
			throws IOException, ParseException {
		Reader reader;
		String fileName = file.getName();
		int idx = fileName.lastIndexOf('.');
		if (idx > 0) {
			String ext = fileName.substring(idx + 1).toLowerCase();
			if ("jar".equals(ext) || "zip".equals(ext)) {
				ZipFile zfile = new ZipFile(file);
				for (Enumeration e = zfile.entries(); e.hasMoreElements();) {
					ZipEntry entry = (ZipEntry) e.nextElement();
					String entryName = entry.getName();
					if (entryName.endsWith(".pnut")) {
						if (verbose) {
							System.out.println(entryName);
						}
						InputStream in = zfile.getInputStream(entry);
						if (encoding == null) {
							reader = new InputStreamReader(in);
						} else {
							reader = new InputStreamReader(in, encoding);
						}
						Pnuts p = Pnuts.parse(reader);
						try {
							p.setScriptSource(new URL("jar:" + file.toURL() + "!/" + entryName));
						} catch (MalformedURLException mue){
							// skip
						}
						String className = className(entryName);
						compileToFile(p, className, dir);
						classNames.addElement(className);
					} else if (entryName.endsWith(".pnc")){
						if (verbose) {
							System.out.println(entryName);
						}
						InputStream in = zfile.getInputStream(entry);
						if (encoding == null) {
							reader = new InputStreamReader(in);
						} else {
							reader = new InputStreamReader(in, encoding);
						}
						FileWriterHandler handler = new FileWriterHandler(dir);
						if (verbose) {
						    handler.setVerbose(true);
						}
						URL url = new URL("jar:" + file.toURL() + "!/" + entryName);
						String className = className(entryName);
						getCompiler(className).compileClassScript(reader, url, handler);
					}
				}
				return;
			} else if ("pnc".equals(ext)){
			    InputStream in = new FileInputStream(file);
			    if (encoding == null) {
				reader = new InputStreamReader(in);
			    } else {
				reader = new InputStreamReader(in, encoding);
			    }
			    FileWriterHandler handler = new FileWriterHandler(dir);
			    if (verbose) {
				handler.setVerbose(true);
			    }
			    URL url = file.toURL();
			    String className = className(fileName);
			    getCompiler(className).compileClassScript(reader, url, handler);
			    return;
			}
		}
		if (encoding == null) {
			reader = new FileReader(file);
		} else {
			reader = new InputStreamReader(new FileInputStream(file), encoding);
		}
		Pnuts pn = Pnuts.parse(reader);
		pn.setScriptSource(file.toURL());
		String className = className(fileName);
		compileToFile(pn, className, dir);
		classNames.addElement(className);
	}

	static String className(String name) {
		int pos = name.lastIndexOf('.');
		String cname = name.replace('-', '_');
		if (pos > 0) {
			cname = cname.substring(0, pos).replace(file_sep.charAt(0), '.');
			if (!"/".equals(file_sep)) {
				cname = cname.replace('/', '.');
			}
		}
		if (prefix != null) {
			if (!"".equals(prefix) && !prefix.endsWith(".")) {
				prefix = prefix + ".";
			}
			cname = prefix + cname;
		}
		return cname;
	}

	/**
	 * <pre>
	 * public class mainClassName extends Runtime {
	 * public Object run(Context ctx){
	 *       ctx.usePackage(module1);
	 *       ...
	 *       new className1().run((Context)ctx.clone());
	 *       ...
	 *    }
	 * 	public static void main(String args[]) {
	 * 		Context context = new Context();
	 * 		context.getCurrentPackage().set(&quot;$args&quot;.intern(), args);
	 * 		new mainClassName().run(context);
	 * 	}
	 * }
	 * </pre>
	 */
	public static ClassFile generateMainClass(String mainClassName,
			String arg0, String[] modules, String[] classNames,
			String pnutsImplClassName) {
		ClassFile cf = new ClassFile(mainClassName, "pnuts.lang.Runtime", null,
				Constants.ACC_PUBLIC);
		cf.openMethod("<init>", "()V", Constants.ACC_PUBLIC);
		cf.add(Opcode.ALOAD_0);
		cf.add(Opcode.INVOKESPECIAL, "pnuts.lang.Runtime", "<init>", "()", "V");
		cf.add(Opcode.RETURN);
		cf.closeMethod();

		cf.openMethod("run", "(Lpnuts/lang/Context;)Ljava/lang/Object;",
				Constants.ACC_PUBLIC);
		cf.add(Opcode.ALOAD_1);
		int ctx = cf.getLocal();
		cf.storeLocal(ctx);

		for (int i = 0; i < modules.length; i++) {
			cf.loadLocal(ctx);
			cf.add(Opcode.LDC, cf.addConstant(modules[i]));
			cf.add(Opcode.INVOKEVIRTUAL, "pnuts.lang.Context", "usePackage",
					"(Ljava/lang/String;)", "Z");
			cf.add(Opcode.POP);
		}

		for (int i = 0; i < classNames.length; i++) {
			if (i > 0) {
				cf.add(Opcode.POP);
			}
			String name = classNames[i];
			cf.add(Opcode.NEW, name);
			cf.add(Opcode.DUP);
			cf.add(Opcode.INVOKESPECIAL, name, "<init>", "()", "V");
			cf.loadLocal(ctx);
			cf.add(Opcode.INVOKEVIRTUAL, "pnuts.lang.Context", "clone", "()",
					"Ljava/lang/Object;");
			cf.add(Opcode.CHECKCAST, "pnuts.lang.Context");
			cf.add(Opcode.INVOKEVIRTUAL, name, "run", "(Lpnuts/lang/Context;)",
					"Ljava/lang/Object;");
		}
		cf.add(Opcode.ARETURN);
		cf.closeMethod();

		cf.openMethod("main", "([Ljava/lang/String;)V",
				(short) (Constants.ACC_PUBLIC | Constants.ACC_STATIC));

		cf.add(Opcode.ALOAD_0);
		cf.add(Opcode.ARRAYLENGTH);
		int len = cf.getLocal();
		cf.istoreLocal(len);
		cf.iloadLocal(len);
		cf.add(Opcode.ICONST_1);
		cf.add(Opcode.IADD);
		cf.add(Opcode.ANEWARRAY, "java.lang.String");
		int array = cf.getLocal();
		cf.storeLocal(array);
		cf.add(Opcode.ALOAD_0);
		cf.add(Opcode.ICONST_0);
		cf.loadLocal(array);
		cf.add(Opcode.ICONST_1);
		cf.iloadLocal(len);
		cf.add(Opcode.INVOKESTATIC, "java.lang.System", "arraycopy",
				"(Ljava/lang/Object;ILjava/lang/Object;II)", "V");

		cf.loadLocal(array);
		cf.pushInteger(0);
		cf.add(Opcode.LDC, cf.addConstant(arg0));
		cf.add(Opcode.AASTORE);

		cf.add(Opcode.NEW, "pnuts.lang.Context");
		cf.add(Opcode.DUP);
		cf.add(Opcode.INVOKESPECIAL, "pnuts.lang.Context", "<init>", "()", "V");
		ctx = cf.getLocal();
		cf.storeLocal(ctx);
		cf.loadLocal(ctx);
		cf.add(Opcode.INVOKEVIRTUAL, "pnuts.lang.Context", "getCurrentPackage",
				"()", "Lpnuts/lang/Package;");

		cf.add(Opcode.LDC, cf.addConstant("$args"));
		cf.add(Opcode.INVOKEVIRTUAL, "java.lang.String", "intern", "()",
				"Ljava/lang/String;");
		cf.loadLocal(array);
		cf.add(Opcode.INVOKEVIRTUAL, "pnuts.lang.Package", "set",
				"(Ljava/lang/String;Ljava/lang/Object;)", "V");

		cf.add(Opcode.NEW, mainClassName);
		cf.add(Opcode.DUP);
		cf.add(Opcode.INVOKESPECIAL, mainClassName, "<init>", "()", "V");

		if (pnutsImplClassName != null) {
			cf.loadLocal(ctx);
			cf.add(Opcode.NEW, pnutsImplClassName);
			cf.add(Opcode.DUP);
			cf.add(Opcode.INVOKESPECIAL, pnutsImplClassName, "<init>", "()",
					"V");
			cf.add(Opcode.INVOKEVIRTUAL, "pnuts.lang.Context",
					"setImplementation", "(Lpnuts/lang/Implementation;)", "V");
		}

		cf.loadLocal(ctx);
		cf.add(Opcode.INVOKEINTERFACE, "pnuts.lang.Executable", "run",
				"(Lpnuts/lang/Context;)", "Ljava/lang/Object;");

		cf.add(Opcode.POP);
		cf.add(Opcode.RETURN);
		cf.closeMethod();
		return cf;
	}

	static void setProperty(String name, String value) {
		Properties prop = System.getProperties();
		prop.put(name, value);
		System.setProperties(prop);
	}

	/**
	 * Compile Pnuts scripts and save the generated code into a ZIP file.
	 * 
	 * @param zout
	 *            the ZipOutputStream to which the generated byte code is
	 *            written
	 * @param files
	 *            the script files
	 * @param dirs
	 *            the directories in which each script file resides
	 * @param modules
	 *            used modules
	 * @param mainClassName
	 *            the main class name
	 */
	public void compileToZip(ZipOutputStream zout, String[] files,
			String[] dirs, String[] modules, String mainClassName,
			String pnutsImplClassName, String encoding) throws IOException,
			ParseException {
		if (mainClassName != null) {
			ZipEntry meta_inf = new ZipEntry("meta-inf/manifest.mf");
			String manifest = "Manifest-Version: 1.0\nMain-Class: "
					+ mainClassName + "\nCreated-By: pnutsc\n";
			zout.putNextEntry(meta_inf);
			zout.write(manifest.getBytes());
		}
		String currentDir = System.getProperty("user.dir");
		try {
			Vector classNames = new Vector();
			for (int i = 0; i < files.length; i++) {
				String fileName = files[i];
				setProperty("user.dir", new File(dirs[i]).getCanonicalPath());
				compileToZip(new File(fileName), zout, classNames, encoding);
			}
			if (mainClassName != null) {
				String[] classNameArray = new String[classNames.size()];
				for (int i = 0; i < classNameArray.length; i++) {
					classNameArray[i] = (String) classNames.elementAt(i);
				}
				ZipEntry entry = new ZipEntry(mainClassName.replace('.', '/')
						+ ".class");
				zout.putNextEntry(entry);
				ClassFile cf = generateMainClass(mainClassName, files[0],
						modules, classNameArray, pnutsImplClassName);
				cf.write(zout);
			}
		} finally {
			setProperty("user.dir", currentDir);
		}
	}

	/**
	 * Compile Pnuts scripts and save the generated code into class files.
	 * 
	 * @param dir
	 *            the directory in which the generated byte code is saved
	 * @param files
	 *            the script files
	 * @param dirs
	 *            the directories in which each script file resides
	 * @param modules
	 *            used modules
	 * @param mainClassName
	 *            the main class name
	 */
	public void compileToFile(File dir, String[] files, String[] dirs,
			String[] modules, String mainClassName, String pnutsImplClassName,
			String encoding) throws IOException, ParseException {
		if (mainClassName != null) {
			File meta_dir = new File(dir, "meta-inf");
			if (!meta_dir.exists()) {
				meta_dir.mkdirs();
			}
			String manifest = "Manifest-Version: 1.0\nMain-Class: "
					+ mainClassName + "\nCreated-By: pnutsc\n";
			FileOutputStream fout = new FileOutputStream(new File(meta_dir,
					"manifest.mf"));
			fout.write(manifest.getBytes());
			fout.close();
		}
		Vector classNames = new Vector();
		String currentDir = System.getProperty("user.dir");
		try {
			for (int i = 0; i < files.length; i++) {
				String fileName = files[i];
				setProperty("user.dir", new File(dirs[i]).getCanonicalPath());
				compileToFile(new File(fileName), dir, classNames, encoding);
			}
			if (mainClassName != null) {
				String[] classNameArray = new String[classNames.size()];
				for (int i = 0; i < classNameArray.length; i++) {
					classNameArray[i] = (String) classNames.elementAt(i);
				}
				FileOutputStream main = new FileOutputStream(new File(dir,
						mainClassName.replace('.', '/') + ".class"));
				ClassFile cf = generateMainClass(mainClassName, files[0],
						modules, classNameArray, pnutsImplClassName);
				cf.write(main);
				main.close();
			}
		} finally {
			setProperty("user.dir", currentDir);
		}
	}

	public static void main(String args[]) throws Throwable {
		int nargs = args.length;

		if (nargs == 0) {
			Main.printHelp("pnutsc");
			return;
		}

		PnutsCompiler compiler = new PnutsCompiler();

		String dest = System.getProperty("user.dir");
		String base = dest;
		String jar = null;
		Vector files = new Vector();
		Vector dirs = new Vector();
		Vector modules = new Vector();
		boolean module = false;
		String mainClassName = null;
		String pnutsImpl = null;
		String encoding = null;

		for (int i = 0; i < nargs; i++) {
			if ("-d".equals(args[i])) {
				dest = args[++i];
			} else if ("-no_proxy".equals(args[i])) {
				compiler.useDynamicProxy(false);
			} else if ("-o".equals(args[i])) {
				jar = args[++i];
			} else if ("-v".equals(args[i])) {
				compiler.setVerbose(true);
			} else if ("-O".equals(args[i])) {
				compiler.includeLineNumber(false);
			} else if ("-C".equals(args[i])) {
				base = args[++i];
			} else if ("-prefix".equals(args[i])) {
				PnutsCompiler.prefix = args[++i];
			} else if ("-main".equals(args[i])) {
				mainClassName = args[++i];
			} else if ("-m".equals(args[i])) {
				modules.addElement(args[++i]);
			} else if ("-impl".equals(args[i])) {
				pnutsImpl = args[++i];
			} else if ("-encoding".equals(args[i])) {
				encoding = args[++i];
			} else if ("-help".equals(args[i])) {
				Main.printHelp("pnutsc");
				System.exit(0);
			} else {
				files.addElement(args[i]);
				dirs.addElement(base);
			}
		}

		File dir = new File(dest);
		if (!dir.exists()) {
			System.err.println("Directory " + dest + " does not exist");
		}

		if (files.size() == 0) {
			Main.printHelp("pnutsc");
			return;
		}

		String[] fileArray = new String[files.size()];
		for (int i = 0; i < fileArray.length; i++) {
			fileArray[i] = (String) files.elementAt(i);
		}
		String[] dirArray = new String[dirs.size()];
		for (int i = 0; i < dirArray.length; i++) {
			dirArray[i] = (String) dirs.elementAt(i);
		}
		String[] moduleArray = new String[modules.size()];
		for (int i = 0; i < moduleArray.length; i++) {
			moduleArray[i] = (String) modules.elementAt(i);
		}

		if (jar != null) {
			if (new File(jar).exists()) {
				ByteArrayOutputStream bout = new ByteArrayOutputStream();
				ZipOutputStream zout = new ZipOutputStream(bout);

				ZipFile zfile = new ZipFile(jar);
				byte[] buf = new byte[512];

				for (Enumeration e = zfile.entries(); e.hasMoreElements();) {
					ZipEntry entry = (ZipEntry) e.nextElement();
					InputStream in = zfile.getInputStream(entry);
					zout.putNextEntry((ZipEntry) entry.clone());
					int n;
					while ((n = in.read(buf)) != -1) {
						zout.write(buf, 0, n);
					}
				}
				compiler.compileToZip(zout, fileArray, dirArray, moduleArray,
						mainClassName, pnutsImpl, encoding);
				zout.finish();
				FileOutputStream fout = new FileOutputStream(jar);
				bout.writeTo(fout);
				fout.close();
			} else {
				ZipOutputStream zout = new ZipOutputStream(
						new FileOutputStream(jar));
				compiler.compileToZip(zout, fileArray, dirArray, moduleArray,
						mainClassName, pnutsImpl, encoding);
				zout.close();
			}
		} else {
			compiler.compileToFile(dir, fileArray, dirArray, moduleArray,
					mainClassName, pnutsImpl, encoding);
		}
	}
}
