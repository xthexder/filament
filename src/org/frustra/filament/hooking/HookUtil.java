package org.frustra.filament.hooking;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.frustra.filament.FilamentStorage;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class HookUtil {
	public static Class<?> lookupType(Type type) {
		try {
			if (type == null) return null;
			String baseType = type.getClassName().replaceAll("\\[\\]", "");
			Class<?> cls = FilamentStorage.store.classLoader.loadClass(baseType);
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
	
	public static Class<?> lookupClass(String hook) throws BadHookException {
		return lookupClass(Hooks.getClass(hook));
	}

	public static Class<?> lookupClass(CustomClassNode node) {
		try {
			if (node == null) return null;
			return FilamentStorage.store.classLoader.loadClass(node.name.replace('/', '.'));
		} catch (Throwable e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static Field lookupField(String hook) throws BadHookException {
		return lookupField(Hooks.getClass(hook.substring(0, hook.lastIndexOf('.'))), Hooks.getField(hook));
	}

	public static Field lookupField(CustomClassNode node, FieldNode field) {
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
	
	public static Method lookupMethod(String hook) throws BadHookException {
		return lookupMethod(Hooks.getClass(hook.substring(0, hook.lastIndexOf('.'))), Hooks.getMethod(hook));
	}

	public static Method lookupMethod(CustomClassNode node, MethodNode method) {
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

	public static CustomClassNode getClassNode(String name) {
		CustomClassNode node = FilamentStorage.store.classes.get(name.replace('.', '/'));
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

	public static boolean compareFieldNode(FieldNode f, String hook) throws BadHookException {
		return compareFieldNode(f, Hooks.getField(hook));
	}

	public static boolean compareMethodNode(MethodNode a, MethodNode b) {
		if (a == null || b == null) return false;
		return a.name.equals(b.name) && a.desc.equals(b.desc);
	}

	public static boolean compareMethodNode(MethodNode m, String hook) throws BadHookException {
		return compareMethodNode(m, Hooks.getMethod(hook));
	}

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
		CustomClassNode node = Hooks.getClass(hook.substring(0, hook.lastIndexOf('.')));
		MethodNode m = Hooks.getMethod(hook);
		return new MethodInsnNode(opcode, node.name, m.name, m.desc);
	}
	
	public static FieldInsnNode createFieldInsnNode(int opcode, String hook) throws BadHookException {
		CustomClassNode node = Hooks.getClass(hook.substring(0, hook.lastIndexOf('.')));
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
