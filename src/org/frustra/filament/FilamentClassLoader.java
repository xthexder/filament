package org.frustra.filament;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.frustra.filament.hooking.FilamentClassNode;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/**
 * FilamentClassLoader is the {@link URLClassLoader} used by filament when hooking and injecting classes.
 * It is also used for managing resources associated with those classes.
 * <p>
 * FilamentClassLoader creates a global Filament instance, which contains all the hook providers, injectors, and classes loaded into it.
 * There can only be one instance of FilamentClassLoader defined per application.
 * <p>
 * An example program using filament could consist of: <blockquote>
 * 
 * <pre>
 * loader = new FilamentClassLoader(true);
 * loader.loadPackage(&quot;org.frustra.example.target&quot;);
 * Thread.currentThread().setContextClassLoader(loader);
 * 
 * Hooking.loadHooks(&quot;org.frustra.example.hooks&quot;);
 * Injection.loadInjectors(&quot;org.frustra.example.injectors&quot;);
 * 
 * Class&lt;?&gt; cls = loader.loadClass(&quot;org.frustra.example.target.Main&quot;);
 * Method entryPoint = cls.getDeclaredMethod(&quot;main&quot;, new Class[] { String[].class });
 * entryPoint.invoke(null, new Object[] { new String[0] });
 * </pre>
 * 
 * </blockquote>
 * 
 * @author Jacob Wirth
 * @see Hooks
 * @see Injectors
 */
public class FilamentClassLoader extends URLClassLoader {
	private ClassLoader parent;
	private HashMap<String, Class<?>> loaded = new HashMap<String, Class<?>>();

	/**
	 * Create a FilamentClassLoader
	 * 
	 * @param debug <code>true</code> if filament should log hooking and injection info
	 */
	public FilamentClassLoader(boolean debug) {
		this(debug, FilamentClassLoader.class.getClassLoader());
	}

	/**
	 * Create a FilamentClassLoader with the specified parent
	 * 
	 * @param debug <code>true</code> if filament should log hooking and injection info
	 * @param parent a ClassLoader to be set as the parent
	 */
	public FilamentClassLoader(boolean debug, ClassLoader parent) {
		super(new URL[0]);
		new Filament(this, debug);
		this.parent = parent;
	}

	/**
	 * Load the contents of a jar into the filament class loader so that they can be hooked and injected.
	 * 
	 * @param jarFile a jar to be loaded
	 * @throws IOException if the jar couldn't be loaded
	 */
	public final void loadJar(File jarFile) throws IOException {
		JarFile jar = new JarFile(jarFile);
		try {
			loadJar(jar, jarFile.toURI().toURL());
		} finally {
			jar.close();
		}
	}

	/**
	 * Load the contents of a jar into the filament class loader so that they can be hooked and injected.
	 * 
	 * @param jarFile a jar to be loaded
	 * @throws IOException if the jar couldn't be loaded
	 * @throws URISyntaxException if the jar {@link URL} is not a valid file
	 */
	public final void loadJar(URL jarFile) throws IOException, URISyntaxException {
		JarFile jar = new JarFile(new File(jarFile.toURI()));
		try {
			loadJar(jar, jarFile);
		} finally {
			jar.close();
		}
	}

	/**
	 * Load the contents of a jar into the filament class loader
	 * so that they can be hooked and injected.
	 * <p>
	 * If the url argument is <code>null</code>, only classes will be loaded. This means other resources won't be available through the class loader.
	 * 
	 * @param jar a JarFile to be loaded
	 * @param url the URL that the JarFile was loaded from
	 * @throws IOException if the jar couldn't be loaded
	 */
	public final void loadJar(JarFile jar, URL url) throws IOException {
		Enumeration<JarEntry> entries = jar.entries();
		while (entries.hasMoreElements()) {
			JarEntry entry = entries.nextElement();
			if (entry != null && entry.getName().endsWith(".class")) {
				FilamentClassNode node = FilamentClassNode.loadFromStream(jar.getInputStream(entry));
				String name = node.name.replaceAll("/", ".");
				Filament.filament.classes.put(name, node);
			}
		}
		if (url != null) addURL(url);
	}

	/**
	 * Load the classes contained within a package into the filament class loader so that they can be hooked and injected.
	 * This function will overwrite any previously loaded classes.
	 * 
	 * @param packageName a String representing the name of a package
	 * @throws IOException if a class can't be read
	 * @throws ClassNotFoundException if the package couldn't be resolved
	 */
	public final void loadPackage(String packageName) throws IOException, ClassNotFoundException {
		String[] classes = listPackage(packageName);
		for (String name : classes) {
			InputStream stream = getResourceAsStream(name.replace('.', '/') + ".class");
			if (stream == null) throw new IOException("Couldn't find resource: " + name);
			FilamentClassNode node = FilamentClassNode.loadFromStream(stream);
			Filament.filament.classes.put(name, node);
		}
	}

	/**
	 * Load a list of classes into the filament class loader so that they can be hooked and injected.
	 * This function will overwrite any previously loaded classes.
	 * 
	 * @param classes an array of classes to be loaded
	 * @throws IOException if a class can't be read
	 */
	public final void loadClasses(Class<?>... classes) throws IOException {
		for (Class<?> cls : classes) {
			InputStream stream = null;
			try {
				URL root = cls.getProtectionDomain().getCodeSource().getLocation();
				URL url = new URL(root, cls.getName().replace('.', '/') + ".class");
				stream = url.openStream();
			} catch (Exception e) {
				throw new IOException("Couldn't find resource: " + cls.getName(), e);
			}
			if (stream == null) throw new IOException("Couldn't find resource: " + cls.getName());
			FilamentClassNode node = FilamentClassNode.loadFromStream(stream);
			Filament.filament.classes.put(cls.getName(), node);
		}
	}

	/**
	 * List the names of the classes contained within a package
	 * 
	 * @param packageName a String representing the name of a package
	 * @return an array containing the full name of each class as a String
	 * @throws IOException if a class can't be read
	 * @throws ClassNotFoundException if the package couldn't be resolved
	 */
	public final String[] listPackage(String packageName) throws IOException, ClassNotFoundException {
		ArrayList<String> classes = new ArrayList<String>();
		URL codeRoot = this.getClass().getProtectionDomain().getCodeSource().getLocation();
		if (codeRoot == null) throw new ClassNotFoundException("Couldn't determine code root!");
		File root = null;
		try {
			root = new File(codeRoot.toURI().getPath());
		} catch (URISyntaxException e) {
			throw new ClassNotFoundException("Couldn't determine code root!");
		}
		if (root.isDirectory()) {
			URL packageURL = getResource(packageName.replace('.', '/'));
			if (packageURL == null) throw new ClassNotFoundException("Couldn't load package location: " + packageName);
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
		} else if (root.getAbsolutePath().endsWith(".jar")) {
			JarFile file = new JarFile(root);
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
			System.out.println("Unknown source type: " + root.getAbsolutePath());
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

	public final Class<?> loadClass(String name) throws ClassNotFoundException {
		Class<?> cls = loaded.get(name);
		if (cls == null) {
			cls = defineClass(name);
			if (cls != null) {
				loaded.put(name, cls);
			}
		}
		return cls;
	}

	/**
	 * Define a class from a byte array.
	 * Can be overridden to manually set the ProtectionDomain.
	 * 
	 * @param name the name of a class to define
	 * @param buf the bytes representing a class
	 * @return the java Class represented by the byte array
	 */
	protected Class<?> defineClass(String name, byte[] buf) {
		return defineClass(name, buf, 0, buf.length);
	}

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

	/**
	 * Get the bytes representing a class.
	 * If the class has relevant injectors, they will be run on the class.
	 * 
	 * @param name the name of a class
	 * @return a byte array representing the class or <code>null</code> if the class is not loaded for modification
	 */
	public byte[] getClassBytes(String name) {
		FilamentClassNode node = Filament.filament.classes.get(name);

		if (node != null) {
			Injectors.injectClass(node);

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
		return getResourceAsStreamOriginal(name);
	}
	
	public InputStream getResourceAsStreamOriginal(String name) {
		InputStream stream = null;
		try {
			stream = super.getResourceAsStream(name);
		} catch (Throwable e) {}
		if (stream != null) return stream;
		try {
			stream = parent.getResourceAsStream(name);
		} catch (Throwable e) {}
		if (stream != null) return stream;
		try {
			stream = FilamentClassLoader.class.getResourceAsStream("/" + name);
		} catch (Throwable e) {}
		return stream;
	}

	public URL findResource(String name) {
		byte[] buf = null;
		if (name.endsWith(".class")) buf = getClassBytes(name.substring(0, name.length() - 6).replace('/', '.'));
		if (buf == null) {
			URL url = null;
			try {
				url = super.findResource(name);
			} catch (Throwable e) {}
			if (url != null) return url;
			try {
				url = parent.getResource(name);
			} catch (Throwable e) {}
			if (url != null) return url;
			try {
				url = FilamentClassLoader.class.getResource("/" + name);
			} catch (Throwable e) {}
			return url;
		}
		return getResourceFromBytes(name, buf);
	}

	/**
	 * Gets the URL associated with a resource.
	 * The resource does not have to exist as an actual file on the system,
	 * for example classes returned by getClassBytes(name).
	 * 
	 * @param name the name of the resource
	 * @param buf the bytes defining the resource
	 * @return a URL that can be used to read the resource
	 */
	public URL getResourceFromBytes(String name, byte[] buf) {
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
