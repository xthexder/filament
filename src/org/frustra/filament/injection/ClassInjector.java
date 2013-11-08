package org.frustra.filament.injection;

import org.frustra.filament.hooking.BadHookException;
import org.frustra.filament.hooking.CustomClassNode;

public abstract class ClassInjector {
	public abstract boolean match(CustomClassNode node) throws BadHookException;

	public abstract void inject(CustomClassNode node) throws BadHookException;

	public void doInject(CustomClassNode node) {
		try {
			if (match(node)) inject(node);
		} catch (BadHookException e) {
			e.printStackTrace();
		}
	}
}
