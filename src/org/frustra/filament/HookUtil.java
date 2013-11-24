package org.frustra.filament;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.frustra.filament.hooking.BadHookException;
import org.frustra.filament.hooking.FilamentClassNode;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * The HookUtil class contains methods for looking up ASM objects and using hooks.
 * An object can only be found by these methods if they are loaded into the {@link FilamentClassLoader} instance.
 * 
 * @author Jacob Wirth
 * @see Hooks
 * @see FilamentClassLoader
 * @see FilamentClassNode
 */
public final class HookUtil {
	/**
	 * Get the java reflected class associated with an ASM {@link Type}.
	 * 
	 * @param type the ASM type to lookup
	 * @return the java class described by the type
	 */
	public static Class<?> lookupType(Type type) {
		try {
			if (type == null) return null;
			String baseType = type.getClassName().replaceAll("\\[\\]", "");
			Class<?> cls = Filament.filament.classLoader.loadClass(baseType);
			int dimensions = 0;
			try {
				if (type.getClassName().contains("[]")) dimensions = type.getDimensions();
			} catch (Exception e) {}
			if (dimensions > 0) cls = Array.newInstance(cls, new int[dimensions]).getClass();
			return cls;
		} catch (Throwable e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Get the java reflected class associated with a class hook.
	 * 
	 * @param hook the name of a class hook
	 * @return the java class described by the hook
	 * @see Hooks
	 */
	public static Class<?> lookupClass(String hook) {
		return lookupClass(Hooks.getClass(hook));
	}

	/**
	 * Get the java reflected class associated with a {@link FilamentClassNode}.
	 * 
	 * @param node a {@link FilamentClassNode}
	 * @return the java class defined by the {@link FilamentClassNode}
	 */
	public static Class<?> lookupClass(FilamentClassNode node) {
		try {
			if (node == null) return null;
			return Filament.filament.classLoader.loadClass(node.name.replace('/', '.'));
		} catch (Throwable e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Get the java reflected field associated with a field hook.
	 * 
	 * @param hook the name of a field hook
	 * @return the java field described by the hook
	 */
	public static Field lookupField(String hook) {
		return lookupField(Hooks.getClass(hook.substring(0, hook.lastIndexOf('.'))), Hooks.getField(hook));
	}

	/**
	 * Get the java reflected field associated with a {@link FieldNode}.
	 * 
	 * @param node the {@link FilamentClassNode} containing the field
	 * @param field a {@link FieldNode}
	 * @return the java field defined by the {@link FieldNode}
	 */
	public static Field lookupField(FilamentClassNode node, FieldNode field) {
		try {
			Class<?> cls = lookupClass(node);
			if (cls == null) return null;
			Field f = cls.getDeclaredField(field.name);
			f.setAccessible(true);
			return f;
		} catch (Throwable e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Get the java reflected method associated with a method hook.
	 * 
	 * @param hook the name of a method hook
	 * @return the java method described by the hook
	 */
	public static Method lookupMethod(String hook) throws BadHookException {
		return lookupMethod(Hooks.getClass(hook.substring(0, hook.lastIndexOf('.'))), Hooks.getMethod(hook));
	}

	/**
	 * Get the java reflected method associated with a {@link MethodNode}.
	 * 
	 * @param node the {@link FilamentClassNode} containing the method
	 * @param method a {@link MethodNode}
	 * @return the java method defined by the {@link MethodNode}
	 */
	public static Method lookupMethod(FilamentClassNode node, MethodNode method) {
		try {
			Class<?> cls = lookupClass(node);
			Type[] params = Type.getArgumentTypes(method.desc);
			Class<?>[] params2 = new Class<?>[params.length];
			for (int i = 0; i < params.length; i++) {
				params2[i] = lookupType(params[i]);
			}
			Method m = cls.getDeclaredMethod(method.name, params2);
			m.setAccessible(true);
			return m;
		} catch (Throwable e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Get a {@link FilamentClassNode} by name. The name argument can be either a java class name or an ASM internal name.
	 * 
	 * @param name the name of a class
	 * @return the {@link FilamentClassNode} with the specified name
	 */
	public static FilamentClassNode getClassNode(String name) {
		FilamentClassNode node = Filament.filament.classes.get(name.replace('.', '/'));
		return node;
	}

	/**
	 * Get a {@link MethodNode} by its name, description, and the name of its containing class.
	 * <p>
	 * This is equivalent to calling <code>getMethodNode(getClassNode(node), name, desc);</code>
	 * 
	 * @param node the name of the {@link FilamentClassNode} containing the method
	 * @param name the name of a method
	 * @param desc the ASM description of the method
	 * @return the {@link FilamentClassNode} with the specified name and description
	 */
	public static MethodNode getMethodNode(String node, String name, String desc) {
		FilamentClassNode node2 = getClassNode(node);
		if (node2 == null) return null;
		return getMethodNode(node2, name, desc);
	}

	/**
	 * Get a {@link MethodNode} by its name, description, and containing class.
	 * 
	 * @param node the {@link FilamentClassNode} containing the method
	 * @param name the name of a method
	 * @param desc the ASM description of the method
	 * @return the {@link FilamentClassNode} with the specified name and description
	 */
	@SuppressWarnings("unchecked")
	public static MethodNode getMethodNode(FilamentClassNode node, String name, String desc) {
		for (MethodNode m : (List<MethodNode>) node.methods) {
			if (m.name.equals(name) && m.desc.equals(desc)) return m;
		}
		return null;
	}

	/**
	 * Get a {@link FieldNode} by its name, description, and the name of its containing class.
	 * <p>
	 * This is equivalent to calling <code>getFieldNode(getClassNode(node), name, desc);</code>
	 * 
	 * @param node the name of the {@link FilamentClassNode} containing the field
	 * @param name the name of a field
	 * @param desc the ASM description of the field's type
	 * @return the {@link FieldNode} with the specified name and description
	 */
	public static FieldNode getFieldNode(String node, String name, String desc) {
		FilamentClassNode node2 = getClassNode(node);
		if (node2 == null) return null;
		return getFieldNode(node2, name, desc);
	}

	/**
	 * Get a {@link FieldNode} by its name, description, and containing class.
	 * 
	 * @param node the {@link FilamentClassNode} containing the field
	 * @param name the name of a field
	 * @param desc the ASM description of the field's type
	 * @return the {@link FieldNode} with the specified name and description
	 */
	@SuppressWarnings("unchecked")
	public static FieldNode getFieldNode(FilamentClassNode node, String name, String desc) {
		for (FieldNode f : (List<FieldNode>) node.fields) {
			if (f.name.equals(name) && f.desc.equals(desc)) return f;
		}
		return null;
	}

	/**
	 * Compare two {@link FieldNode} objects to see if they have the same signature.
	 * 
	 * @param a a {@link FieldNode}
	 * @param b a {@link FieldNode}
	 * @return <code>true</code> if both fields have the same name and ASM description
	 */
	public static boolean compareFieldNode(FieldNode a, FieldNode b) {
		if (a == null || b == null) return false;
		return a.name.equals(b.name) && a.desc.equals(b.desc);
	}


	/**
	 * Compare a field hook with a {@link FieldNode} to see if they have the same signature.
	 * 
	 * @param f a {@link FieldNode}
	 * @param hook the name of a field hook
	 * @return <code>true</code> if both fields have the same name and ASM description
	 */
	public static boolean compareFieldNode(FieldNode f, String hook) throws BadHookException {
		return compareFieldNode(f, Hooks.getField(hook));
	}

	/**
	 * Compare two {@link MethodNode} objects to see if they have the same signature.
	 * 
	 * @param a a {@link MethodNode}
	 * @param b a {@link MethodNode}
	 * @return <code>true</code> if both methods have the same name and ASM description
	 */
	public static boolean compareMethodNode(MethodNode a, MethodNode b) {
		if (a == null || b == null) return false;
		return a.name.equals(b.name) && a.desc.equals(b.desc);
	}

	/**
	 * Compare a method hook with a {@link MethodNode} to see if they have the same signature.
	 * 
	 * @param m a {@link MethodNode}
	 * @param hook the name of a method hook
	 * @return <code>true</code> if both method have the same name and ASM description
	 */
	public static boolean compareMethodNode(MethodNode m, String hook) throws BadHookException {
		return compareMethodNode(m, Hooks.getMethod(hook));
	}

	/**
	 * Compare a class hook with an ASM {@link Type} to see if they reference the same class.
	 * 
	 * @param type an ASM {@link Type}
	 * @param hook the name of a class hook
	 * @return <code>true</code> if both classes have the same name and package
	 */
	public static boolean compareType(Type type, String hook) throws BadHookException {
		return Hooks.getClass(hook).equals(type);
	}
	
	public static MethodInsnNode createMethodInsnNode(int opcode, Object cls, String name, Object ret, Object... params) throws BadHookException {
		if (cls == null || name == null || ret == null) return null;
		String clsName = null;
		if (cls instanceof String) {
			clsName = Hooks.getClassName((String) cls);
		} else if (cls instanceof Class<?>) {
			clsName = Type.getInternalName((Class<?>) cls);
		} else if (cls instanceof Type) {
			clsName = ((Type) cls).getInternalName();
		} else if (cls instanceof ClassNode) {
			clsName = ((ClassNode) cls).name;
		} else throw new ClassCastException("Class param must be a String, Type, Class or ClassNode, got " + cls.getClass().getName());
		Type retType = null;
		if (ret instanceof String) {
			retType = Type.getObjectType(Hooks.getClassName((String) ret));
		} else if (ret instanceof Class<?>) {
			retType = Type.getType((Class<?>) ret);
		} else if (ret instanceof Type) {
			retType = (Type) ret;
		} else if (ret instanceof ClassNode) {
			retType = Type.getObjectType(((ClassNode) ret).name);
		} else throw new ClassCastException("Return type must be a String, Type, Class or ClassNode, got " + ret.getClass().getName());
		ArrayList<Type> paramTypes = new ArrayList<Type>();
		for (Object param : params) {
			Type tmpType = null;
			if (param instanceof String) {
				tmpType = Type.getObjectType(Hooks.getClassName((String) param));
			} else if (param instanceof Class<?>) {
				tmpType = Type.getType((Class<?>) param);
			} else if (param instanceof Type) {
				tmpType = (Type) param;
			} else if (param instanceof ClassNode) {
				tmpType = Type.getObjectType(((ClassNode) param).name);
			} else throw new ClassCastException("Param types must be String, Type, Class or ClassNode, got " + param.getClass().getName());
			paramTypes.add(tmpType);
		}
		String desc = Type.getMethodDescriptor(retType, paramTypes.toArray(new Type[0]));
		if (clsName == null) throw new NullPointerException("Class name resolved to null!");
		if (desc == null) throw new NullPointerException("Method descriptor resolved to null!");
		return new MethodInsnNode(opcode, clsName, name, desc);
	}
	
	public static MethodInsnNode createMethodInsnNode(int opcode, String hook) throws BadHookException {
		FilamentClassNode node = Hooks.getClass(hook.substring(0, hook.lastIndexOf('.')));
		MethodNode m = Hooks.getMethod(hook);
		return new MethodInsnNode(opcode, node.name, m.name, m.desc);
	}
	
	public static FieldInsnNode createFieldInsnNode(int opcode, String hook) throws BadHookException {
		FilamentClassNode node = Hooks.getClass(hook.substring(0, hook.lastIndexOf('.')));
		FieldNode f = Hooks.getField(hook);
		return new FieldInsnNode(opcode, node.name, f.name, f.desc);
	}
	public static FieldInsnNode createFieldInsnNode(int opcode, Object cls, String name, Object type) throws BadHookException {
		if (cls == null || name == null || type == null) return null;
		String clsName = null;
		if (cls instanceof String) {
			clsName = Hooks.getClassName((String) cls);
		} else if (cls instanceof Class<?>) {
			clsName = Type.getInternalName((Class<?>) cls);
		} else if (cls instanceof Type) {
			clsName = ((Type) cls).getInternalName();
		} else if (cls instanceof ClassNode) {
			clsName = ((ClassNode) cls).name;
		} else throw new ClassCastException("Class param must be a String, Type, Class or ClassNode, got " + cls.getClass().getName());
		String desc = null;
		if (type instanceof String) {
			desc = Type.getObjectType(Hooks.getClassName((String) type)).getDescriptor();
		} else if (type instanceof Class<?>) {
			desc = Type.getType((Class<?>) type).getDescriptor();
		} else if (type instanceof Type) {
			desc = ((Type) type).getDescriptor();
		} else if (type instanceof ClassNode) {
			desc = Type.getObjectType(((ClassNode) type).name).getDescriptor();
		} else throw new ClassCastException("Field type must be a String, Type, Class or ClassNode, got " + type.getClass().getName());
		if (clsName == null) throw new NullPointerException("Class name resolved to null!");
		if (desc == null) throw new NullPointerException("Field type resolved to null!");
		return new FieldInsnNode(opcode, clsName, name, desc);
	}
}
