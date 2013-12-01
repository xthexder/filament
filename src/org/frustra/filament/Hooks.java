package org.frustra.filament;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.frustra.filament.hooking.BadHookException;
import org.frustra.filament.hooking.FilamentClassNode;
import org.frustra.filament.hooking.types.ClassProvider;
import org.frustra.filament.hooking.types.ConstantProvider;
import org.frustra.filament.hooking.types.FieldProvider;
import org.frustra.filament.hooking.types.HookProvider;
import org.frustra.filament.hooking.types.HookingPass;
import org.frustra.filament.hooking.types.InstructionProvider;
import org.frustra.filament.hooking.types.MethodProvider;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * The Hooks class defines all the code for processing hooks.
 * Each hook can be set and read with methods in this class.
 * Hooks are generally defined by a {@link HookProvider}, which will set their value based on a matched class, method, field, etc...
 * <p>
 * Hook providers can be loaded by calling <code>Hooks.load(packageName)</code>, but must be loaded after a {@link FilamentClassLoader} is created.
 * <p>
 * Hooks are mapped to strings and should follow the naming convention listed bellow: <blockquote>
 * 
 * <pre>
 *     ClassName
 *     ClassName.fieldName
 *     ClassName.methodName
 *     ClassName.someProperty
 *     globalProperty
 * </pre>
 * 
 * </blockquote>Hooks that are prefixed with a class hook will automatically be handled when using utility functions within {@link HookUtil}.
 * See {@link HookProvider} for more information on defining hooks and hook providers.
 * 
 * @author Jacob Wirth
 * @see HookUtil
 * @see HookProvider
 */
public final class Hooks {
	private Hooks() {}
	
	private static HashMap<String, Object> hooks = new HashMap<String, Object>();

	/**
	 * Load all hook providers contained within the specified package, and run them on the global Filament instance.
	 * A FilamentClassLoader must be created in order to initialize the Filament instance.
	 * <p>
	 * A hook provider is any class that extends {@link HookProvider}.
	 * Hook providers have a {@link HookingPass} annotation to define what order they are processed in.
	 * Any hook providers loaded with a previous call to load will be overwritten.
	 * 
	 * @param packageName the name of the package containing hook providers
	 * @throws ReflectiveOperationException if the package or any classes cannot be loaded
	 * @throws BadHookException if there is an error while processing the hooks
	 * @throws IOException if one of the provider classes could not be read
	 * @see FilamentClassLoader
	 * @see HookProvider
	 * @see HookingPass
	 */
	public static final void load(String packageName) throws ReflectiveOperationException, IOException, BadHookException {
		String[] hooks = Filament.filament.classLoader.listPackage(packageName);
		Filament.filament.hooks.clear();
		for (String name : hooks) {
			Class<?> cls = Filament.filament.classLoader.loadClass(name);
			if (HookProvider.class.isAssignableFrom(cls)) {
				addHook((HookProvider) cls.newInstance());

				if (Filament.filament.debug) {
					System.out.println("Loaded Hook: " + cls.getSimpleName());
				}
			}
		}
		doHooking();
	}
	
	private static void addHook(HookProvider hook) {
		HookingPass hookingPass = hook.getClass().getAnnotation(HookingPass.class);
		int pass = 1;
		if (hookingPass != null) pass = hookingPass.value();
		ArrayList<HookProvider> hooks = Filament.filament.hooks.get(pass);
		if (hooks == null) hooks = new ArrayList<HookProvider>();
		hooks.add(hook);
		Filament.filament.hooks.put(pass, hooks);
	}

	private static void doHooking() throws BadHookException {
		if (Filament.filament.debug) {
			System.out.println();
			System.out.println("Executing hooks...");
		}

		hooks.clear();

		try {
			for (Integer pass : Filament.filament.hooks.keySet()) {
				doHookingPass(pass);
			}
		} finally {
			if (Filament.filament.debug) {
				debugHooks();
				System.out.println("Hooking complete");
				System.out.println();
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static void doHookingPass(int pass) throws BadHookException {
		ArrayList<HookProvider> hooks = Filament.filament.hooks.get(pass);
		ArrayList<HookProvider> classHooks = new ArrayList<HookProvider>();
		ArrayList<HookProvider> methodHooks = new ArrayList<HookProvider>();
		for (HookProvider hook : hooks) {
			hook.reset();
		}
		for (FilamentClassNode node : Filament.filament.classes.values()) {
			classHooks = (ArrayList<HookProvider>) hooks.clone();
			for (HookProvider hook : classHooks.toArray(new HookProvider[0])) {
				if (hook instanceof ClassProvider) {
					if (!((ClassProvider) hook).doMatch(node)) classHooks.remove(hook);
				}
			}
			if (classHooks.size() > 0) {
				for (String constant : node.getConstants()) {
					for (HookProvider hook : classHooks) {
						if (hook instanceof ConstantProvider) {
							((ConstantProvider) hook).doMatch(node, constant);
						}
					}
				}
			}
			if (classHooks.size() > 0) {
				for (FieldNode f : (List<FieldNode>) node.fields) {
					for (HookProvider hook : classHooks) {
						if (hook instanceof FieldProvider) {
							((FieldProvider) hook).doMatch(node, f);
						}
					}
				}
			}
			if (classHooks.size() > 0) {
				for (MethodNode m : (List<MethodNode>) node.methods) {
					methodHooks = (ArrayList<HookProvider>) classHooks.clone();
					for (HookProvider hook : methodHooks.toArray(new HookProvider[0])) {
						if (hook instanceof MethodProvider) {
							if (!((MethodProvider) hook).doMatch(node, m)) methodHooks.remove(hook);
						}
					}

					if (methodHooks.size() > 0) {
						AbstractInsnNode insn = m.instructions.getFirst();
						while (insn != null) {
							for (HookProvider hook : methodHooks) {
								if (hook instanceof InstructionProvider) {
									((InstructionProvider) hook).doMatch(node, m, insn);
								}
							}
							insn = insn.getNext();
						}
					}
				}
			}
		}
		Exception firstError = null;
		for (HookProvider hook : hooks) {
			try {
				hook.doComplete();
			} catch (BadHookException e) {
				if (firstError == null) firstError = e;
				if (Filament.filament.debug) System.err.println("Failed hook provider: " + hook + ", Cause: " + e.getProblem());
			}
		}
		if (firstError != null) {
			throw new BadHookException("Errors occured while processing hooking pass " + pass, firstError);
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

	/**
	 * Get the value of a hook casted to a {@link FilamentClassNode}. See above for hook name formats.
	 * 
	 * @param hook the name of a hook
	 * @return the hook's value
	 * @throws BadHookException if the specified hook is undefined or is the wrong type
	 */
	public static final FilamentClassNode getClass(String hook) throws BadHookException {
		try {
			return (FilamentClassNode) get(hook);
		} catch (ClassCastException e) {
			throw new BadHookException("Referenced hook is wrong type: " + hook, e);
		}
	}

	/**
	 * Get the name of a class hook's class. See above for hook name formats.
	 * <p>
	 * The hook's value must be a {@link FilamentClassNode}.
	 * 
	 * @param hook the name of a hook
	 * @return the hook's value
	 * @throws BadHookException if the specified hook is undefined or is the wrong type
	 */
	public static final String getClassName(String hook) throws BadHookException {
		try {
			return ((FilamentClassNode) get(hook)).name;
		} catch (ClassCastException e) {
			throw new BadHookException("Referenced hook is wrong type: " + hook, e);
		}
	}

	/**
	 * Get the value of a hook casted to a {@link FieldNode}. See above for hook name formats.
	 * 
	 * @param hook the name of a hook
	 * @return the hook's value
	 * @throws BadHookException if the specified hook is undefined or is the wrong type
	 */
	public static final FieldNode getField(String hook) throws BadHookException {
		try {
			return (FieldNode) get(hook);
		} catch (ClassCastException e) {
			throw new BadHookException("Referenced hook is wrong type: " + hook, e);
		}
	}

	/**
	 * Get the value of a hook casted to a {@link MethodNode}. See above for hook name formats.
	 * 
	 * @param hook the name of a hook
	 * @return the hook's value
	 * @throws BadHookException if the specified hook is undefined or is the wrong type
	 */
	public static final MethodNode getMethod(String hook) throws BadHookException {
		try {
			return (MethodNode) get(hook);
		} catch (ClassCastException e) {
			throw new BadHookException("Referenced hook is wrong type: " + hook, e);
		}
	}

	/**
	 * Get the value of a hook casted to a {@link String}. See above for hook name formats.
	 * 
	 * @param hook the name of a hook
	 * @return the hook's value
	 * @throws BadHookException if the specified hook is undefined or is the wrong type
	 */
	public static final String getString(String hook) throws BadHookException {
		try {
			return (String) get(hook);
		} catch (ClassCastException e) {
			throw new BadHookException("Referenced hook is wrong type: " + hook, e);
		}
	}

	/**
	 * Get the value of a hook without casting its type. See above for hook name formats.
	 * 
	 * @param hook the name of a hook
	 * @return the hook's value
	 * @throws BadHookException if the specified hook is undefined
	 */
	public static final Object get(String hook) throws BadHookException {
		Object val = hooks.get(hook);
		if (val == null) throw new BadHookException("Referenced undefined hook: " + hook);
		return val;
	}

	/**
	 * Set the value of a hook. See above for hook name formats.
	 * 
	 * @param hook the name of a hook
	 * @param value the hook's value
	 */
	public static final void set(String hook, Object value) {
		hooks.put(hook, value);
	}
}
