package org.frustra.filament.hooking.types;

import org.frustra.filament.hooking.BadHookException;
import org.frustra.filament.hooking.CustomClassNode;
import org.objectweb.asm.tree.FieldNode;

public abstract class FieldHook extends ClassHook {
	protected FieldNode cField = null;

	protected boolean match(CustomClassNode node) {
		return true;
	}

	protected abstract boolean match(CustomClassNode node, FieldNode f);

	protected void onComplete(CustomClassNode node, FieldNode f) {
		onComplete(node);
	}

	public void reset() {
		super.reset();
		cField = null;
	}

	public void doMatch(CustomClassNode node, FieldNode f) {
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
		onComplete(cNode, cField);
	}
}
