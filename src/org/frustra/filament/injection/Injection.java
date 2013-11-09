package org.frustra.filament.injection;

import java.io.IOException;
import java.net.URISyntaxException;

import org.frustra.filament.FilamentStorage;
import org.frustra.filament.hooking.CustomClassNode;
import org.frustra.filament.injection.annotations.AnnotationInjector;

public class Injection {
	public static void loadInjectors(String packageName) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, URISyntaxException {
		String[] injectors = FilamentStorage.store.classLoader.listPackage(packageName);
		FilamentStorage.store.injectors.clear();
		FilamentStorage.store.injectors.add(new AnnotationInjector());
		for (String name : injectors) {
			Class<?> cls = FilamentStorage.store.classLoader.loadClass(name);
			FilamentStorage.store.injectors.add((ClassInjector) cls.newInstance());
			if (FilamentStorage.store.debug) {
				System.out.println("Loaded Injector: " + cls.getSimpleName());
			}
		}
	}

	public static void injectClass(CustomClassNode node) {
		if (node == null) return;
		for (ClassInjector injector : FilamentStorage.store.injectors) {
			try {
				injector.doInject(node);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}
}
