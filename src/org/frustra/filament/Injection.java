package org.frustra.filament;

import java.io.IOException;
import java.net.URISyntaxException;

import org.frustra.filament.hooking.FilamentClassNode;
import org.frustra.filament.injection.ClassInjector;
import org.frustra.filament.injection.annotations.AnnotationInjector;

public class Injection {
	public static void loadInjectors(String packageName) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, URISyntaxException {
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

	public static void injectClass(FilamentClassNode node) {
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
