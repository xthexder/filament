package org.frustra.filament.injection;

import org.frustra.filament.hooking.CustomClassNode;

public abstract class ClassInjector {
	public abstract boolean match(CustomClassNode node);

	public abstract void inject(CustomClassNode node);

	public void doInject(CustomClassNode node) {
		if (match(node)) inject(node);
	}
}
