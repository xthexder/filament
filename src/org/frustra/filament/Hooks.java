package org.frustra.filament;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.frustra.filament.hooking.BadHookException;
import org.frustra.filament.hooking.FilamentClassNode;
import org.frustra.filament.hooking.types.ClassHook;
import org.frustra.filament.hooking.types.ConstantHook;
import org.frustra.filament.hooking.types.FieldHook;
import org.frustra.filament.hooking.types.Hook;
import org.frustra.filament.hooking.types.HookingPass;
import org.frustra.filament.hooking.types.HookingPassOne;
import org.frustra.filament.hooking.types.HookingPassThree;
import org.frustra.filament.hooking.types.HookingPassTwo;
import org.frustra.filament.hooking.types.InstructionHook;
import org.frustra.filament.hooking.types.MethodHook;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * The Hooks class defines all the code for processing hooks.
 * The results of each hook can be set and read with methods in this class. 
 * <p>
 * Hook results are mapped to strings, and generally have one of the following formats:
 * <blockquote><pre>
 *     ClassName
 *     ClassName.fieldName
 *     ClassName.methodName
 *     ClassName.someProperty
 * </pre></blockquote>
 * Hooks that are prefixed with a class will automatically be handled when using utility functions within {@link HookUtil}
 * 
 * @author Jacob Wirth
 * @see HookUtil
 */
public class Hooks {
	private static HashMap<String, Object> hooks = new HashMap<String, Object>();

	/**
	 * Get the value of a hook casted to a {@link FilamentClassNode}. See above for hook name formats.
	 * 
	 * @param name the name of a hook
	 * @return the hook's value
	 */
	public static final FilamentClassNode getClass(String name) { return (FilamentClassNode) get(name); }
	
	/**
	 * Get the name of a class hook's class. See above for hook name formats.
	 * <p>
	 * The hook's value must be a {@link FilamentClassNode}.
	 * 
	 * @param name the name of a hook
	 * @return the hook's value
	 */
	public static final String getClassName(String name) { return ((FilamentClassNode) get(name)).name; }
	
	/**
	 * Get the value of a hook casted to a {@link FieldNode}. See above for hook name formats.
	 * 
	 * @param name the name of a hook
	 * @return the hook's value
	 */
	public static final FieldNode getField(String name) { return (FieldNode) get(name); }
	
	/**
	 * Get the value of a hook casted to a {@link MethodNode}. See above for hook name formats.
	 * 
	 * @param name the name of a hook
	 * @return the hook's value
	 */
	public static final MethodNode getMethod(String name) { return (MethodNode) get(name); }
	
	/**
	 * Get the value of a hook casted to a {@link String}. See above for hook name formats.
	 * 
	 * @param name the name of a hook
	 * @return the hook's value
	 */
	public static final String getString(String name) { return (String) get(name); }
	
	/**
	 * Get the value of a hook without casting its type. See above for hook name formats.
	 * 
	 * @param name the name of a hook
	 * @return the hook's value
	 */
	public static final Object get(String name) {
		return hooks.get(name);
	}
	
	/**
	 * Set the value of a hook. See above for hook name formats.
	 * 
	 * @param name the name of a hook
	 * @param value the hook's value
	 */
	public static final void set(String name, Object value) {
		hooks.put(name, value);
	}
	
	/**
	 * Load all hooks contained within the specified package, and run them on the global Filament instance.
	 * A FilamentClassLoader must be created in order to initialize the Filament instance.
	 * <p>
	 * A hook is any class that extends {@link Hook}. Hooks will not be run unless they implement a {@link HookingPass}.
	 * <p>
	 * Any hooks loaded with a previous call to loadHooks will be overwritten.
	 * 
	 * @param packageName the name of the package containing hooks
	 * @throws ReflectiveOperationException if the package or any classes cannot be loaded
	 * @throws BadHookException if there is an error while processing the hooks
	 * @throws IOException if one of the hook classes could not be read
	 * @see Hook
	 * @see HookingPass
	 */
	public static final void loadHooks(String packageName) throws ReflectiveOperationException, BadHookException, IOException {
		String[] hooks = Filament.filament.classLoader.listPackage(packageName);
		Filament.filament.passTwoHooks = 0;
		Filament.filament.passThreeHooks = 0;
		Filament.filament.allHooks.clear();
		Filament.filament.classHooks.clear();
		Filament.filament.constantHooks.clear();
		Filament.filament.fieldHooks.clear();
		Filament.filament.methodHooks.clear();
		Filament.filament.instructionHooks.clear();
		for (String name : hooks) {
			Class<?> cls = Filament.filament.classLoader.loadClass(name);
			if (Hook.class.isAssignableFrom(cls)) {
				Hook hook = (Hook) cls.newInstance();
				if (hook instanceof InstructionHook) {
					Filament.filament.instructionHooks.add((InstructionHook) hook);
				} else if (hook instanceof MethodHook) {
					Filament.filament.methodHooks.add((MethodHook) hook);
				} else if (hook instanceof FieldHook) {
					Filament.filament.fieldHooks.add((FieldHook) hook);
				} else if (hook instanceof ConstantHook) {
					Filament.filament.constantHooks.add((ConstantHook) hook);
				} else if (hook instanceof ClassHook) {
					Filament.filament.classHooks.add((ClassHook) hook);
				}
				if (hook instanceof HookingPass) {
					if (hook instanceof HookingPassTwo) {
						Filament.filament.passTwoHooks++;
					}
					if (hook instanceof HookingPassThree) {
						Filament.filament.passThreeHooks++;
					}
					Filament.filament.allHooks.add((HookingPass) hook);
				}

				if (Filament.filament.debug) {
					System.out.println("Loaded Hook: " + cls.getSimpleName());
				}
			}
		}
		doHooking();
	}

	private static void doHooking() throws BadHookException {
		if (Filament.filament.debug) {
			System.out.println();
			System.out.println("Executing hooks...");
		}

		for (HookingPass hook : Filament.filament.allHooks) {
			hook.reset();
		}
		hooks.clear();

		doHookingPass(HookingPassOne.class);

		if (Filament.filament.passTwoHooks > 0) {
			doHookingPass(HookingPassTwo.class);
		}

		if (Filament.filament.passThreeHooks > 0) {
			doHookingPass(HookingPassThree.class);
		}

		if (Filament.filament.debug) {
			debugHooks();
			System.out.println("Hooking complete");
			System.out.println();
		}
	}

	@SuppressWarnings("unchecked")
	private static void doHookingPass(Class<?> hookingPass) throws BadHookException {
		boolean errors = false;
		for (FilamentClassNode node : Filament.filament.classes.values()) {
			if (Filament.filament.classHooks.size() > 0) {
				for (ClassHook hook : Filament.filament.classHooks) {
					if (hookingPass.isInstance(hook)) {
						try {
							hook.doMatch(node);
						} catch (Exception e) {
							e.printStackTrace();
							errors = true;
						}
					}
				}
			}
			if (Filament.filament.constantHooks.size() > 0) {
				for (String constant : node.constants) {
					for (ConstantHook hook : Filament.filament.constantHooks) {
						if (hookingPass.isInstance(hook)) {
							try {
								hook.doMatch(node, constant);
							} catch (Exception e) {
								e.printStackTrace();
								errors = true;
							}
						}
					}
				}
			}
			if (Filament.filament.fieldHooks.size() > 0) {
				for (FieldNode f : (List<FieldNode>) node.fields) {
					for (FieldHook hook : Filament.filament.fieldHooks) {
						if (hookingPass.isInstance(hook)) {
							try {
								hook.doMatch(node, f);
							} catch (Exception e) {
								e.printStackTrace();
								errors = true;
							}
						}
					}
				}
			}
			if (Filament.filament.methodHooks.size() > 0 || Filament.filament.instructionHooks.size() > 0) {
				for (MethodNode m : (List<MethodNode>) node.methods) {
					if (Filament.filament.methodHooks.size() > 0) {
						for (MethodHook hook : Filament.filament.methodHooks) {
							if (hookingPass.isInstance(hook)) {
								try {
									hook.doMatch(node, m);
								} catch (Exception e) {
									e.printStackTrace();
									errors = true;
								}
							}
						}
					}

					if (Filament.filament.instructionHooks.size() > 0) {
						AbstractInsnNode insn = m.instructions.getFirst();
						while (insn != null) {
							for (InstructionHook hook : Filament.filament.instructionHooks) {
								if (hookingPass.isInstance(hook)) {
									try {
										hook.doMatch(node, m, insn);
									} catch (Exception e) {
										e.printStackTrace();
										errors = true;
									}
								}
							}
							insn = insn.getNext();
						}
					}
				}
			}
		}
		for (HookingPass hook : Filament.filament.allHooks) {
			if (hookingPass.isInstance(hook)) {
				try {
					hook.doComplete();
				} catch (Exception e) {
					e.printStackTrace();
					errors = true;
				}
			}
		}
		if (errors) {
			throw new BadHookException("Errors occured while processing " + hookingPass.getSimpleName(), null);
		}
	}
	
	private static void debugHooks() {
		try {
			ArrayList<String> output = new ArrayList<String>();
			for (Entry<String, Object> entry : hooks.entrySet()) {
				String key = entry.getKey();
				Object value = entry.getValue();
				if (value instanceof FilamentClassNode) {
					output.add(key + " = " + ((FilamentClassNode) value).name);
				} else if (value instanceof FieldNode) {
					output.add(key + " = " + ((FieldNode) value).name);
				} else if (value instanceof MethodNode) {
					output.add(key + " = " + ((MethodNode) value).name + ((MethodNode) value).desc);
				} else {
					output.add(key + " = " + value);
				}
			}
			String[] output2 = output.toArray(new String[0]);
			Arrays.sort(output2);
			for (String line : output2) {
				System.out.println(line);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
