package org.frustra.filament;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

import org.frustra.filament.hooking.FilamentClassNode;
import org.frustra.filament.hooking.types.HookProvider;
import org.frustra.filament.injection.ClassInjector;

public final class Filament {
	protected static Filament filament;

	protected boolean debug;

	protected HashMap<String, FilamentClassNode> classes = new HashMap<String, FilamentClassNode>();
	protected FilamentClassLoader classLoader = null;

	protected TreeMap<Integer, ArrayList<HookProvider>> hooks = new TreeMap<Integer, ArrayList<HookProvider>>();
	protected ArrayList<ClassInjector> injectors = new ArrayList<ClassInjector>();

	protected Filament(FilamentClassLoader loader, boolean debug) {
		this.classLoader = loader;
		this.debug = debug;
		if (Filament.filament != null) throw new RuntimeException("An instance of Filament already exists!");
		Filament.filament = this;
	}
}
