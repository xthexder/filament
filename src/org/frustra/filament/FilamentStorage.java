package org.frustra.filament;

import java.util.ArrayList;
import java.util.HashMap;

import org.frustra.filament.hooking.CustomClassNode;
import org.frustra.filament.hooking.types.ClassHook;
import org.frustra.filament.hooking.types.ConstantHook;
import org.frustra.filament.hooking.types.FieldHook;
import org.frustra.filament.hooking.types.HookingPass;
import org.frustra.filament.hooking.types.MethodHook;
import org.frustra.filament.injection.ClassInjector;

public class FilamentStorage {
	public static FilamentStorage store;

	public boolean debug;

	public HashMap<String, CustomClassNode> classes = new HashMap<String, CustomClassNode>();
	public HashMap<String, String> urlconfig = new HashMap<String, String>();

	public ClassLoader classLoader = null;

	public int passTwoHooks = 0;
	public int passThreeHooks = 0;
	public ArrayList<HookingPass> allHooks = new ArrayList<HookingPass>();
	public ArrayList<ClassHook> classHooks = new ArrayList<ClassHook>();
	public ArrayList<ConstantHook> constantHooks = new ArrayList<ConstantHook>();
	public ArrayList<FieldHook> fieldHooks = new ArrayList<FieldHook>();
	public ArrayList<MethodHook> methodHooks = new ArrayList<MethodHook>();

	public ArrayList<ClassInjector> injectors = new ArrayList<ClassInjector>();

	public FilamentStorage(ClassLoader loader, boolean debug) {
		this.classLoader = loader;
		this.debug = debug;
	}
}
