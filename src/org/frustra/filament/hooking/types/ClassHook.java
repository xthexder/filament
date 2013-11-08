package org.frustra.filament.hooking.types;

import org.frustra.filament.hooking.BadHookException;
import org.frustra.filament.hooking.CustomClassNode;

public abstract class ClassHook extends Hook {
	protected CustomClassNode cNode = null;
	protected CustomClassNode lastClass = null;

	protected abstract boolean match(CustomClassNode node) throws BadHookException;

	protected abstract void onComplete(CustomClassNode node) throws BadHookException;

	public void reset() {
		super.reset();
		cNode = null;
	}

	public void doMatch(CustomClassNode node) {
		if (!valid || completed) return;
		if (conditions != null) {
			if (lastClass != node) {
				conditions = new boolean[conditions.length];
				lastClass = node;
			}
		}
		try {
			if (!match(node)) return;
		} catch (Throwable e) {
			error = e;
			valid = false;
			return;
		}
		if (matched) {
			valid = false;
		} else {
			cNode = node;
			matched = true;
		}
	}

	public void doComplete() throws BadHookException {
		if (completed) return;
		assertValid();
		completed = true;
		onComplete(cNode);
	}
}
