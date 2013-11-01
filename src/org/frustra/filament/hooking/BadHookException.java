package org.frustra.filament.hooking;

import org.frustra.filament.hooking.types.Hook;

public class BadHookException extends Exception {
	private String problem;
	private Hook provider;

	public BadHookException(String problem, Hook provider) {
		this(problem, provider, null);
	}

	public BadHookException(String problem, Hook providor, Throwable cause) {
		super(providor + ": " + problem, cause);
		this.problem = problem;
		this.provider = providor;
	}

	public String getProblem() {
		return problem;
	}

	public Hook getProvider() {
		return provider;
	}
}
