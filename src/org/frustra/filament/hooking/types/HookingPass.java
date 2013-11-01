package org.frustra.filament.hooking.types;

import org.frustra.filament.hooking.BadHookException;

public interface HookingPass {
	public void reset();

	public void doComplete() throws BadHookException;
}
