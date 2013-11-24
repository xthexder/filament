package org.frustra.filament.hooking.types;

import org.frustra.filament.hooking.BadHookException;
import org.frustra.filament.hooking.FilamentClassNode;
import org.objectweb.asm.tree.MethodNode;

public abstract class MethodHook extends ClassHook {
	protected MethodNode cMethod = null;

	protected boolean match(FilamentClassNode node) throws BadHookException {
		return true;
	}

	protected abstract boolean match(FilamentClassNode node, MethodNode m) throws BadHookException;

	protected void onComplete(FilamentClassNode node) throws BadHookException {}
	protected abstract void onComplete(FilamentClassNode node, MethodNode m) throws BadHookException;

	public void reset() {
		super.reset();
		cMethod = null;
	}

	public void doMatch(FilamentClassNode node, MethodNode m) {
		if (!valid || completed) return;
		try {
			if (!match(node)) return;
			if (!match(node, m)) return;
		} catch (Throwable e) {
			error = e;
			valid = false;
			return;
		}
		if (matched) {
			valid = false;
		} else {
			cNode = node;
			cMethod = m;
			matched = true;
		}
	}

	public void doComplete() throws BadHookException {
		if (completed) return;
		assertValid();
		completed = true;
		onComplete(cNode);
		onComplete(cNode, cMethod);
	}
}
