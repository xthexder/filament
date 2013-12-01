package org.frustra.filament.hooking;

import org.frustra.filament.HookUtil;
import org.frustra.filament.Hooks;
import org.frustra.filament.hooking.types.HookProvider;

/**
 * BadHookException is thrown by various parts of filament if there was a problem processing a hook, or if such a hook is referenced.
 * 
 * @author Jacob Wirth
 * @see Hooks
 * @see HookUtil
 */
public class BadHookException extends Exception {
	private String problem;
	private HookProvider provider;

	/**
	 * Create a BadHookException from a problem string.
	 * 
	 * @param problem the String representation of the error that occurred
	 */
	public BadHookException(String problem) {
		super(problem);
		this.problem = problem;
		this.provider = null;
	}

	/**
	 * Create a BadHookException from a problem string and cause exception.
	 * 
	 * @param problem the String representation of the error that occurred
	 * @param cause the Throwable cause of the exception
	 */
	public BadHookException(String problem, Throwable cause) {
		super(problem, cause);
		this.problem = problem;
		this.provider = null;
	}

	/**
	 * Create a BadHookException from a problem string and hook provider.
	 * 
	 * @param problem the String representation of the error that occurred
	 * @param providor the {@link HookProvider} class that processes the hook
	 */
	public BadHookException(String problem, HookProvider provider) {
		this(problem, provider, null);
	}

	/**
	 * Create a BadHookException from a problem string, hook provider, and cause exception.
	 * 
	 * @param problem the String representation of the error that occurred
	 * @param providor the {@link HookProvider} class that processes the hook
	 * @param cause the Throwable cause of the exception
	 */
	public BadHookException(String problem, HookProvider providor, Throwable cause) {
		super(providor + ": " + problem, cause);
		this.problem = problem;
		this.provider = providor;
	}

	/**
	 * Get the error message associated with this exception.
	 * 
	 * @return the String representation of the error that occurred
	 */
	public String getProblem() {
		return problem;
	}

	/**
	 * Get the hook provider associated with this exception.
	 * 
	 * @return the {@link HookProvider} class that processes the hook
	 */
	public HookProvider getProvider() {
		return provider;
	}
}
