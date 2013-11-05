package org.frustra.filament;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.frustra.filament.hooking.CustomClassNode;
import org.frustra.filament.injection.InjectionHandler;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

public abstract class FilamentClassLoader extends URLClassLoader {
	public FilamentStorage filament;
	private ClassLoader parent;

	public FilamentClassLoader(boolean debug) throws IOException {
		this(debug, FilamentClassLoader.class.getClassLoader());
	}

	public FilamentClassLoader(boolean debug, ClassLoader parent) throws IOException {
		super(new URL[0]);
		this.filament = new FilamentStorage(this, debug);
		this.parent = parent;
	}

	public void loadJar(File jarFile) throws IOException {
		JarFile jar = new JarFile(jarFile);
		try {
			filament.classes.clear();
			Enumeration<JarEntry> entries = jar.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				if (entry != null && entry.getName().endsWith(".class")) {
					CustomClassNode node = CustomClassNode.loadFromStream(jar.getInputStream(entry));
					String name = node.name.replaceAll("/", ".");
					filament.classes.put(name, node);
				}
			}
			addURL(jarFile.toURI().toURL());
		} finally {
			jar.close();
		}
	}

	public void loadPackage(String packageName) throws IOException, ClassNotFoundException {
		String[] classes = listPackage(packageName);
		for (String name : classes) {
			InputStream stream = FilamentClassLoader.class.getResourceAsStream("/" + name.replace('.', '/') + ".class");
			CustomClassNode node = CustomClassNode.loadFromStream(stream);
			filament.classes.put(name, node);
		}
	}

	public String[] listPackage(String packageName) throws IOException, ClassNotFoundException {
		ArrayList<String> classes = new ArrayList<String>();
		URL packageURL = FilamentClassLoader.class.getResource("/" + packageName.replace('.', '/'));
		if (packageURL.getProtocol().equals("file")) {
			File packageFolder = new File(packageURL.getFile());
			for (File f : packageFolder.listFiles()) {
				String name = f.getName();
				if (f.isFile() && !name.startsWith(".") && name.endsWith(".class")) {
					FileInputStream stream = new FileInputStream(f);
					ClassReader reader = new ClassReader(stream);
					stream.close();
					classes.add(reader.getClassName().replace('/', '.'));
				}
			}
		} else if (packageURL.getProtocol().equals("jar")) {
			String jarPath = packageURL.getFile();
			jarPath = jarPath.substring(5, jarPath.indexOf("!/"));
			JarFile file = new JarFile(jarPath);
			Enumeration<? extends JarEntry> entries = file.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				if (entry == null || entry.isDirectory()) continue;
				String entryPath = entry.getName();
				String fileName = entryPath.substring(entryPath.lastIndexOf("/") + 1);

				if (fileName.endsWith(".class")) {
					ClassReader reader = new ClassReader(file.getInputStream(entry));
					String className = reader.getClassName().replace('/', '.');
					String packageName2 = className.substring(0, className.lastIndexOf('.'));
					if (packageName.equals(packageName2)) classes.add(className);
				}
			}
			file.close();
		} else {
			System.out.println("Unsupported protocol: " + packageURL.getProtocol());
		}
		return classes.toArray(new String[0]);
	}

	protected Class<?> getPrimitiveType(String name) throws ClassNotFoundException {
		if (name.equals("byte") || name.equals("B")) return byte.class;
		if (name.equals("short") || name.equals("S")) return short.class;
		if (name.equals("int") || name.equals("I")) return int.class;
		if (name.equals("long") || name.equals("J")) return long.class;
		if (name.equals("char") || name.equals("C")) return char.class;
		if (name.equals("float") || name.equals("F")) return float.class;
		if (name.equals("double") || name.equals("D")) return double.class;
		if (name.equals("boolean") || name.equals("Z")) return boolean.class;
		if (name.equals("void") || name.equals("V")) return void.class;
		// new ClassNotFoundException(name).printStackTrace();
		throw new ClassNotFoundException(name);
	}

	HashMap<String, Class<?>> loaded = new HashMap<String, Class<?>>();

	public Class<?> loadClass(String name) throws ClassNotFoundException {
		Class<?> cls = loaded.get(name);
		if (cls == null) {
			cls = defineClass(name);
			if (cls != null) loaded.put(name, cls);
		}
		return cls;
	}

	protected abstract Class<?> defineClass(String name, byte[] buf);

	private Class<?> defineClass(String name) throws ClassNotFoundException {
		if (name == null) return null;
		try {
			byte[] buf = getClassBytes(name);
			if (buf != null) {
				return defineClass(name, buf);
			} else {
				try {
					return super.loadClass(name);
				} catch (Exception e1) {
					try {
						return parent.loadClass(name);
					} catch (Exception e2) {
						return getPrimitiveType(name);
					}
				}
			}
		} catch (Exception e) {
			throw new ClassNotFoundException(name, e);
		}
	}

	protected byte[] getClassBytes(String name) {
		CustomClassNode node = filament.classes.get(name);

		if (node != null) {
			InjectionHandler.doInjection(node);

			ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
			node.accept(writer);

			return writer.toByteArray();
		} else return null;
	}

	public InputStream getResourceAsStream(String name) {
		if (name.endsWith(".class")) {
			byte[] buf = getClassBytes(name.substring(0, name.length() - 6).replace('/', '.'));
			if (buf != null) return new ByteArrayInputStream(buf);
		}
		InputStream stream = null;
		try {
			stream = super.getResourceAsStream(name);
		} catch (Throwable e) {}
		if (stream != null) return stream;
		try {
			stream = parent.getResourceAsStream(name);
		} catch (Throwable e) {}
		return stream;
	}

	public URL findResource(String name) {
		byte[] buf = null;
		if (name.endsWith(".class")) buf = getClassBytes(name.substring(0, name.length() - 6).replace('/', '.'));
		URL url = null;
		if (buf == null) {
			try {
				url = super.findResource(name);
			} catch (Throwable e) {}
			if (url != null) return url;
			try {
				url = parent.getResource(name);
			} catch (Throwable e) {}
			return url;
		}
		final InputStream stream = new ByteArrayInputStream(buf);
		URLStreamHandler handler = new URLStreamHandler() {
			protected URLConnection openConnection(URL url) throws IOException {
				return new URLConnection(url) {
					public void connect() throws IOException {}

					public InputStream getInputStream() {
						return stream;
					}
				};
			}
		};
		try {
			return new URL(new URL("http://www.frustra.org/"), "", handler);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}
	}
}
