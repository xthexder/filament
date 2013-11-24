package org.frustra.filament.hooking.types;

import org.frustra.filament.hooking.BadHookException;
import org.frustra.filament.hooking.FilamentClassNode;

public abstract class ConstantHook extends ClassHook {
	protected String cConstant = null;

	protected boolean match(FilamentClassNode node) {
		return true;
	}

	protected abstract boolean match(FilamentClassNode node, String constant);
	
	protected void onComplete(FilamentClassNode node) {}
	protected abstract void onComplete(FilamentClassNode node, String constant);

	public void reset() {
		super.reset();
		cConstant = null;
	}

	public void doMatch(FilamentClassNode node, String constant) {
		if (!valid || completed) return;
		try {
			if (!match(node)) return;
			if (!match(node, constant)) return;
		} catch (Throwable e) {
			error = e;
			valid = false;
			return;
		}
		if (matched) {
			valid = false;
		} else {
			cNode = node;
			cConstant = constant;
			matched = true;
		}
	}

	public void doComplete() throws BadHookException {
		if (completed) return;
		assertValid();
		completed = true;
		onComplete(cNode);
		onComplete(cNode, cConstant);
	}
}
