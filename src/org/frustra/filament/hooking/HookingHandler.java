package org.frustra.filament.hooking;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.frustra.filament.FilamentStorage;
import org.frustra.filament.hooking.types.ClassHook;
import org.frustra.filament.hooking.types.ConstantHook;
import org.frustra.filament.hooking.types.FieldHook;
import org.frustra.filament.hooking.types.Hook;
import org.frustra.filament.hooking.types.HookingPass;
import org.frustra.filament.hooking.types.HookingPassOne;
import org.frustra.filament.hooking.types.HookingPassThree;
import org.frustra.filament.hooking.types.HookingPassTwo;
import org.frustra.filament.hooking.types.MethodHook;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

public class HookingHandler {
	public static void loadHooks(Class<?>[] hooks) throws InstantiationException, IllegalAccessException {
		FilamentStorage.store.passTwoHooks = 0;
		FilamentStorage.store.passThreeHooks = 0;
		for (Class<?> cls : hooks) {
			Hook hook = (Hook) cls.newInstance();
			if (hook instanceof MethodHook) {
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
	
	public static void loadJar(JarFile jar) {
		loadJar(jar, FilamentStorage.store.classes);
	}

	public static void loadJar(JarFile jar, HashMap<String, CustomClassNode> classes) {
		try {
			classes.clear();
			Enumeration<JarEntry> entries = jar.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				if (entry != null && entry.getName().endsWith(".class")) {
					CustomClassNode node = new CustomClassNode();
					ClassReader reader = new ClassReader(jar.getInputStream(entry));
					reader.accept(node, ClassReader.SKIP_DEBUG);
					char[] buf = new char[reader.getMaxStringLength()];
					for (int i = 0; i < reader.getItemCount(); i++) {
						try {
							Object constant = reader.readConst(i, buf);
							if (constant instanceof String) {
								node.constants.add((String) constant);
							} else if (constant instanceof Type) {
								node.references.add((Type) constant);
							}
						} catch (Exception e) {} // Jump targets are also stored here, which do not point to anything.
					}
					node.access &= ~(Opcodes.ACC_FINAL | Opcodes.ACC_PROTECTED | Opcodes.ACC_PRIVATE);
					node.access |= Opcodes.ACC_PUBLIC;
					String name = node.name.replaceAll("/", ".");
					classes.put(name, node);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void doHooking() throws BadHookException {
		for (HookingPass hook : FilamentStorage.store.allHooks) {
			hook.reset();
		}

		doHookingPass(HookingPassOne.class);

		if (FilamentStorage.store.passTwoHooks > 0) {
			doHookingPass(HookingPassTwo.class);
		}

		if (FilamentStorage.store.passThreeHooks > 0) {
			doHookingPass(HookingPassThree.class);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static void doHookingPass(Class<?> hookingPass) throws BadHookException {
		boolean errors = false;
		for (CustomClassNode node : FilamentStorage.store.classes.values()) {
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
			for (MethodNode m : (List<MethodNode>) node.methods) {
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

	public static Class<?> lookupClass(CustomClassNode node) {
		try {
			return FilamentStorage.store.classLoader.loadClass(node.name.replace('/', '.'));
		} catch (Throwable e) {
			e.printStackTrace();
			return null;
		}
	}

	public static Field lookupField(CustomClassNode node, FieldNode field) {
		try {
			Class<?> cls = lookupClass(node);
			Field f = cls.getDeclaredField(field.name);
			f.setAccessible(true);
			return f;
		} catch (Throwable e) {
			e.printStackTrace();
			return null;
		}
	}

	public static Method lookupMethod(CustomClassNode node, MethodNode method) {
		try {
			Class<?> cls = lookupClass(node);
			Type[] params = Type.getArgumentTypes(method.desc);
			Class<?>[] params2 = new Class<?>[params.length];
			for (int i = 0; i < params.length; i++) {
				params2[i] = cls.getClassLoader().loadClass(params[i].getClassName());
			}
			Method m = cls.getDeclaredMethod(method.name, params2);
			m.setAccessible(true);
			return m;
		} catch (Throwable e) {
			return null;
		}
	}

	public static CustomClassNode getClassNode(String name) {
		CustomClassNode node = FilamentStorage.store.classes.get(name);
		return node;
	}

	public static MethodNode getMethodNode(String node, String name, String desc) {
		CustomClassNode node2 = getClassNode(node);
		if (node2 == null) return null;
		return getMethodNode(node2, name, desc);
	}

	@SuppressWarnings("unchecked")
	public static MethodNode getMethodNode(CustomClassNode node, String name, String desc) {
		for (MethodNode m : (List<MethodNode>) node.methods) {
			if (m.name.equals(name) && m.desc.equals(desc)) return m;
		}
		return null;
	}

	public static FieldNode getFieldNode(String node, String name, String desc) {
		CustomClassNode node2 = getClassNode(node);
		if (node2 == null) return null;
		return getFieldNode(node2, name, desc);
	}

	@SuppressWarnings("unchecked")
	public static FieldNode getFieldNode(CustomClassNode node, String name, String desc) {
		for (FieldNode f : (List<FieldNode>) node.fields) {
			if (f.name.equals(name) && f.desc.equals(desc)) return f;
		}
		return null;
	}

	public static boolean compareFieldNode(FieldNode a, FieldNode b) {
		if (a == null || b == null) return false;
		return a.name.equals(b.name) && a.desc.equals(b.desc);
	}

	public static boolean compareMethodNode(MethodNode a, MethodNode b) {
		if (a == null || b == null) return false;
		return a.name.equals(b.name) && a.desc.equals(b.desc);
	}
}
