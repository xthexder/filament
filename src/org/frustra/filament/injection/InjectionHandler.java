package org.frustra.filament.injection;

import org.frustra.filament.FilamentStorage;
import org.frustra.filament.hooking.CustomClassNode;
import org.frustra.filament.injection.annotations.AnnotationInjector;

public class InjectionHandler {
	public static void loadInjectors(Class<ClassInjector>[] injectors) throws InstantiationException, IllegalAccessException {
		FilamentStorage.store.injectors.clear();
		FilamentStorage.store.injectors.add(new AnnotationInjector());
		for (Class<ClassInjector> cls : injectors) {
			FilamentStorage.store.injectors.add(cls.newInstance());
			if (FilamentStorage.store.debug) {
				System.out.println("Loaded Injector: " + cls.getSimpleName());
			}
		}
	}

	public static CustomClassNode doInjection(CustomClassNode node) {
		if (node == null) return null;
		for (ClassInjector injector : FilamentStorage.store.injectors) {
			try {
				injector.doInject(node);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		return node;
	}
}
