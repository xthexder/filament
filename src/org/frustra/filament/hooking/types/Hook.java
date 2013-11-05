package org.frustra.filament.hooking.types;

import org.frustra.filament.hooking.BadHookException;

public abstract class Hook {
	protected boolean valid = true;
	protected boolean matched = false;
	protected boolean completed = false;
	protected Throwable error = null;
	protected boolean[] conditions = null;

	public Hook() {
		HookConditions anno = this.getClass().getAnnotation(HookConditions.class);
		if (anno != null) {
			conditions = new boolean[anno.value()];
		}
	}

	public abstract void doComplete() throws BadHookException;

	public void reset() {
		valid = true;
		matched = false;
		completed = false;
		error = null;
		if (conditions != null) {
			conditions = new boolean[conditions.length];
		}
	}

	protected boolean checkConditions() {
		if (conditions == null) return false;
		for (boolean b : conditions) {
			if (!b) return false;
		}
		return true;
	}

	protected void assertValid() throws BadHookException {
		if (!valid) {
			if (error != null) throw new BadHookException("Error in matcher", this, error);
			else if (matched) throw new BadHookException("Hook found multiple matches", this);
			else throw new BadHookException("Unknown error, hook invalid", this);
		} else if (!matched) throw new BadHookException("No matches found", this);
	}

	public String toString() {
		return this.getClass().getSimpleName();
	}
}
