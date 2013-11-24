package org.frustra.filament.injection;

import org.frustra.filament.hooking.BadHookException;
import org.frustra.filament.hooking.FilamentClassNode;

public abstract class ClassInjector {
	public abstract boolean match(FilamentClassNode node) throws BadHookException;

	public abstract void inject(FilamentClassNode node) throws BadHookException;

	public void doInject(FilamentClassNode node) {
		try {
			if (match(node)) inject(node);
		} catch (BadHookException e) {
			e.printStackTrace();
		}
	}
}
