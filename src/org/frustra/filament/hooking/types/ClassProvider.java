package org.frustra.filament.hooking.types;

import org.frustra.filament.hooking.BadHookException;
import org.frustra.filament.hooking.FilamentClassNode;

public abstract class ClassProvider extends HookProvider {
	protected FilamentClassNode cNode = null;

	public abstract boolean match(FilamentClassNode node) throws BadHookException;
	protected abstract void complete(FilamentClassNode node) throws BadHookException;

	public void reset() {
		super.reset();
		cNode = null;
	}

	public boolean doMatch(FilamentClassNode node) {
		try {
			if (match(node)) {
				if (ClassProvider.class.equals(this.getClass().getSuperclass())) {
					cNode = node;
					matches++;
				}
				return true;
			} else return false;
		} catch (Throwable e) {
			error = e;
			return false;
		}
	}

	public void complete() throws BadHookException {
		complete(cNode);
	}
}
