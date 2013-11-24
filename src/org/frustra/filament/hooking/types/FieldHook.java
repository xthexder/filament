package org.frustra.filament.hooking.types;

import org.frustra.filament.hooking.BadHookException;
import org.frustra.filament.hooking.FilamentClassNode;
import org.objectweb.asm.tree.FieldNode;

public abstract class FieldHook extends ClassHook {
	protected FieldNode cField = null;

	protected boolean match(FilamentClassNode node) throws BadHookException {
		return true;
	}

	protected abstract boolean match(FilamentClassNode node, FieldNode f) throws BadHookException;

	protected void onComplete(FilamentClassNode node) throws BadHookException {}
	protected abstract void onComplete(FilamentClassNode node, FieldNode f) throws BadHookException;

	public void reset() {
		super.reset();
		cField = null;
	}

	public void doMatch(FilamentClassNode node, FieldNode f) {
		if (!valid || completed) return;
		try {
			if (!match(node)) return;
			if (!match(node, f)) return;
		} catch (Throwable e) {
			error = e;
			valid = false;
			return;
		}
		if (matched) {
			valid = false;
		} else {
			cNode = node;
			cField = f;
			matched = true;
		}
	}

	public void doComplete() throws BadHookException {
		if (completed) return;
		assertValid();
		completed = true;
		onComplete(cNode);
		onComplete(cNode, cField);
	}
}