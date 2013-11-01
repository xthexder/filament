package org.frustra.filament.hooking.types;

import org.frustra.filament.hooking.BadHookException;
import org.frustra.filament.hooking.CustomClassNode;
import org.objectweb.asm.tree.MethodNode;

public abstract class MethodHook extends ClassHook {
	protected MethodNode cMethod = null;

	protected boolean match(CustomClassNode node) {
		return true;
	}

	protected abstract boolean match(CustomClassNode node, MethodNode m);

	protected void onComplete(CustomClassNode node, MethodNode m) {
		onComplete(node);
	}

	public void reset() {
		super.reset();
		cMethod = null;
	}

	public void doMatch(CustomClassNode node, MethodNode m) {
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
		onComplete(cNode, cMethod);
	}
}
