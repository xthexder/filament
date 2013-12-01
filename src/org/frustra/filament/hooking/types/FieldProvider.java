package org.frustra.filament.hooking.types;

import org.frustra.filament.hooking.BadHookException;
import org.frustra.filament.hooking.FilamentClassNode;
import org.objectweb.asm.tree.FieldNode;

public abstract class FieldProvider extends ClassProvider {
	protected FieldNode cField = null;

	public boolean match(FilamentClassNode node) throws BadHookException {
		return true;
	}

	public abstract boolean match(FilamentClassNode node, FieldNode f) throws BadHookException;
	protected void complete(FilamentClassNode node) throws BadHookException {}
	protected abstract void complete(FilamentClassNode node, FieldNode f) throws BadHookException;

	public void reset() {
		super.reset();
		cField = null;
	}

	public boolean doMatch(FilamentClassNode node, FieldNode f) {
		try {
			if (match(node, f)) {
				if (FieldProvider.class.equals(getClass().getSuperclass())) {
					cNode = node;
					cField = f;
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
		complete(cNode, cField);
	}
}
