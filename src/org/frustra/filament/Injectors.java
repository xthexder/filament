package org.frustra.filament;

import java.io.IOException;

import org.frustra.filament.hooking.FilamentClassNode;
import org.frustra.filament.injection.AnnotationInjector;
import org.frustra.filament.injection.ClassInjector;

/**
 * The Injectors class defines all the code for processing injectors.
 * Every class loaded through the {@link FilamentClassLoader} will be processed by all matching injectors.
 * <p>
 * Injectors can be loaded by calling <code>Injectors.load(packageName)</code>, but must be loaded after a {@link FilamentClassLoader} is created.
 * <p>
 * There is one build-in injector called the {@link AnnotationInjector}. This will be loaded along with any user-specified injectors. 
 * 
 * @author Jacob Wirth
 * @see FilamentClassLoader
 * @see ClassInjector
 * @see AnnotationInjector
 */
public final class Injectors {
	private Injectors() {}
	
	/**
	 * Load all injectors contained within the specified package into the global Filament instance.
	 * All injectors in this package will be automatically run on any matching classes loaded through the {@link FilamentClassLoader}
	 * The {@link AnnotationInjector} will also be loaded into the Filament instance.
	 * A FilamentClassLoader must be created prior to loading injectors.
	 * <p>
	 * An injector is any class that extends {@link ClassInjector}. Any injectors loaded with a previous call to load will be overwritten.
	 * 
	 * @param packageName the name of the package containing injectors
	 * @throws ReflectiveOperationException if the package or any classes cannot be loaded
	 * @throws IOException if one of the injector classes could not be read
	 * @see ClassInjector
	 */
	public static void load(String packageName) throws ReflectiveOperationException, IOException {
		String[] injectors = Filament.filament.classLoader.listPackage(packageName);
		Filament.filament.injectors.clear();
		Filament.filament.injectors.add(new AnnotationInjector());
		for (String name : injectors) {
			Class<?> cls = Filament.filament.classLoader.loadClass(name);
			Filament.filament.injectors.add((ClassInjector) cls.newInstance());
			if (Filament.filament.debug) {
				System.out.println("Loaded Injector: " + cls.getSimpleName());
			}
		}
	}

	protected static void injectClass(FilamentClassNode node) {
		if (node == null) return;
		for (ClassInjector injector : Filament.filament.injectors) {
			try {
				injector.doInject(node);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}
}
