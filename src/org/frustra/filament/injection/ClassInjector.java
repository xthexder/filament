package org.frustra.filament.injection;

import org.frustra.filament.hooking.BadHookException;
import org.frustra.filament.hooking.FilamentClassNode;

public abstract class ClassInjector {
	protected abstract boolean match(FilamentClassNode node) throws BadHookException;
	protected abstract void inject(FilamentClassNode node) throws BadHookException;

	public void doInject(FilamentClassNode node) {
		try {
			if (!match(node)) return;
		} catch (BadHookException e1) {
			return;
		}
		try {
			inject(node);
		} catch (BadHookException e) {
			e.printStackTrace();
		}
	}
}
