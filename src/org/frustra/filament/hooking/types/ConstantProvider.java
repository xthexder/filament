package org.frustra.filament.hooking.types;

import org.frustra.filament.hooking.BadHookException;
import org.frustra.filament.hooking.FilamentClassNode;

public abstract class ConstantProvider extends ClassProvider {
	protected String cConstant = null;

	public boolean match(FilamentClassNode node) throws BadHookException {
		return true;
	}

	public abstract boolean match(FilamentClassNode node, String constant) throws BadHookException;
	protected void complete(FilamentClassNode node) throws BadHookException {}
	protected abstract void complete(FilamentClassNode node, String constant) throws BadHookException;

	public void reset() {
		super.reset();
		cConstant = null;
	}

	public boolean doMatch(FilamentClassNode node, String constant) {
		try {
			if (match(node, constant)) {
				if (ConstantProvider.class.equals(getClass().getSuperclass())) {
					cNode = node;
					cConstant = constant;
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
		complete(cNode, cConstant);
	}
}
