package org.frustra.filament.hooking.types;

import org.frustra.filament.hooking.BadHookException;
import org.frustra.filament.hooking.FilamentClassNode;
import org.objectweb.asm.tree.MethodNode;

public abstract class MethodProvider extends ClassProvider {
	protected MethodNode cMethod = null;

	public boolean match(FilamentClassNode node) throws BadHookException {
		return true;
	}

	public abstract boolean match(FilamentClassNode node, MethodNode m) throws BadHookException;
	protected void complete(FilamentClassNode node) throws BadHookException {}
	protected abstract void complete(FilamentClassNode node, MethodNode m) throws BadHookException;

	public void reset() {
		super.reset();
		cMethod = null;
	}

	public boolean doMatch(FilamentClassNode node, MethodNode m) {
		try {
			if (match(node, m)) {
				if (MethodProvider.class.equals(getClass().getSuperclass())) {
					cNode = node;
					cMethod = m;
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
		super.complete();
		complete(cNode, cMethod);
	}
}
