package org.frustra.filament;

import java.util.ArrayList;
import java.util.HashMap;

import org.frustra.filament.hooking.CustomClassNode;
import org.frustra.filament.hooking.types.ClassHook;
import org.frustra.filament.hooking.types.ConstantHook;
import org.frustra.filament.hooking.types.FieldHook;
import org.frustra.filament.hooking.types.HookingPass;
import org.frustra.filament.hooking.types.InstructionHook;
import org.frustra.filament.hooking.types.MethodHook;
import org.frustra.filament.injection.ClassInjector;

public class Filament {
	protected static Filament filament;

	protected boolean debug;

	protected HashMap<String, CustomClassNode> classes = new HashMap<String, CustomClassNode>();
	protected FilamentClassLoader classLoader = null;

	protected int passTwoHooks = 0;
	protected int passThreeHooks = 0;
	protected ArrayList<HookingPass> allHooks = new ArrayList<HookingPass>();
	protected ArrayList<ClassHook> classHooks = new ArrayList<ClassHook>();
	protected ArrayList<ConstantHook> constantHooks = new ArrayList<ConstantHook>();
	protected ArrayList<FieldHook> fieldHooks = new ArrayList<FieldHook>();
	protected ArrayList<MethodHook> methodHooks = new ArrayList<MethodHook>();
	protected ArrayList<InstructionHook> instructionHooks = new ArrayList<InstructionHook>();

	protected ArrayList<ClassInjector> injectors = new ArrayList<ClassInjector>();

	protected Filament(FilamentClassLoader loader, boolean debug) {
		this.classLoader = loader;
		this.debug = debug;
		if (Filament.filament != null) throw new RuntimeException("An instance of Filament already exists!");
		Filament.filament = this;
	}
}
