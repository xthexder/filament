package org.frustra.filament.hooking;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.List;

import org.frustra.filament.FilamentStorage;
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

public class Hooking {
	public static String hooksPackage = null;
	
	public static void loadHooks(String packageName) throws InstantiationException, IllegalAccessException, BadHookException, ClassNotFoundException, IOException, URISyntaxException {
		Hooking.hooksPackage = packageName;
		String[] hooks = FilamentStorage.store.classLoader.listPackage(packageName);
		FilamentStorage.store.passTwoHooks = 0;
		FilamentStorage.store.passThreeHooks = 0;
		for (String name : hooks) {
			Class<?> cls = FilamentStorage.store.classLoader.loadClass(name);
			if (Hook.class.isAssignableFrom(cls)) {
				Hook hook = (Hook) cls.newInstance();
				if (hook instanceof InstructionHook) {
					FilamentStorage.store.instructionHooks.add((InstructionHook) hook);
				} else if (hook instanceof MethodHook) {
					FilamentStorage.store.methodHooks.add((MethodHook) hook);
				} else if (hook instanceof FieldHook) {
					FilamentStorage.store.fieldHooks.add((FieldHook) hook);
				} else if (hook instanceof ConstantHook) {
					FilamentStorage.store.constantHooks.add((ConstantHook) hook);
				} else if (hook instanceof ClassHook) {
					FilamentStorage.store.classHooks.add((ClassHook) hook);
				}
				if (hook instanceof HookingPass) {
					if (hook instanceof HookingPassTwo) {
						FilamentStorage.store.passTwoHooks++;
					}
					if (hook instanceof HookingPassThree) {
						FilamentStorage.store.passThreeHooks++;
					}
					FilamentStorage.store.allHooks.add((HookingPass) hook);
				}

				if (FilamentStorage.store.debug) {
					System.out.println("Loaded Hook: " + cls.getSimpleName());
				}
			}
		}
		try {
			Class<?> cls = FilamentStorage.store.classLoader.loadClass(Hooking.class.getName());
			Method doHooking = cls.getDeclaredMethod("doHooking");
			doHooking.invoke(null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void doHooking() throws BadHookException {
		if (FilamentStorage.store.debug) {
			System.out.println();
			System.out.println("Executing hooks...");
		}

		for (HookingPass hook : FilamentStorage.store.allHooks) {
			hook.reset();
		}
		Hooks.reset();

		doHookingPass(HookingPassOne.class);

		if (FilamentStorage.store.passTwoHooks > 0) {
			doHookingPass(HookingPassTwo.class);
		}

		if (FilamentStorage.store.passThreeHooks > 0) {
			doHookingPass(HookingPassThree.class);
		}

		if (FilamentStorage.store.debug) {
			Hooks.outputHooks();
			System.out.println("Hooking complete");
			System.out.println();
		}
	}

	@SuppressWarnings("unchecked")
	public static void doHookingPass(Class<?> hookingPass) throws BadHookException {
		boolean errors = false;
		for (CustomClassNode node : FilamentStorage.store.classes.values()) {
			if (FilamentStorage.store.classHooks.size() > 0) {
				for (ClassHook hook : FilamentStorage.store.classHooks) {
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
			if (FilamentStorage.store.constantHooks.size() > 0) {
				for (String constant : node.constants) {
					for (ConstantHook hook : FilamentStorage.store.constantHooks) {
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
			if (FilamentStorage.store.fieldHooks.size() > 0) {
				for (FieldNode f : (List<FieldNode>) node.fields) {
					for (FieldHook hook : FilamentStorage.store.fieldHooks) {
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
			if (FilamentStorage.store.methodHooks.size() > 0 || FilamentStorage.store.instructionHooks.size() > 0) {
				for (MethodNode m : (List<MethodNode>) node.methods) {
					if (FilamentStorage.store.methodHooks.size() > 0) {
						for (MethodHook hook : FilamentStorage.store.methodHooks) {
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

					if (FilamentStorage.store.instructionHooks.size() > 0) {
						AbstractInsnNode insn = m.instructions.getFirst();
						while (insn != null) {
							for (InstructionHook hook : FilamentStorage.store.instructionHooks) {
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
		for (HookingPass hook : FilamentStorage.store.allHooks) {
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
}
