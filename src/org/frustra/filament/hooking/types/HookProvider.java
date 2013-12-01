package org.frustra.filament.hooking.types;

import org.frustra.filament.hooking.BadHookException;

public abstract class HookProvider {
	protected int matches = 0;
	protected Throwable error = null;

	protected abstract void complete() throws BadHookException;
	
	public final void doComplete() throws BadHookException {
		if (error != null) {
			throw new BadHookException("Error in matcher", this, error);
		} else if (matches < 1) {
			throw new BadHookException("Provider found no matches", this);
		} else if (matches > 1) {
			throw new BadHookException("Provider found multiple matches", this);
		}
		try {
			complete();
		} catch (Throwable e) {
			error = e;
			throw new BadHookException("Error on completion", this, error);
		}
	}

	public void reset() {
		matches = 0;
		error = null;
	}

	public String toString() {
		return this.getClass().getName();
	}
}
