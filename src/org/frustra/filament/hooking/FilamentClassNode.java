package org.frustra.filament.hooking;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.frustra.filament.FilamentClassLoader;
import org.frustra.filament.Hooks;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

/**
 * FilamentClassNode is an ASM {@link ClassNode} that contains extra information about a class used by filament.
 * FilamentClassNode is the class used when loading, hooking, and injecting classes in a {@link FilamentClassLoader}.
 * 
 * @author Jacob Wirth
 * @see FilamentClassLoader
 */
public class FilamentClassNode extends ClassNode {
	private ArrayList<String> constants = new ArrayList<String>();
	private ArrayList<Type> references = new ArrayList<Type>();
	
	public FilamentClassNode() {
		super(Opcodes.ASM5);
	}

	/**
	 * Define a FilamentClassNode from an input stream, such as a file.
	 * This is the method used by {@link FilamentClassLoader} when loading classes to be hooked and injected.
	 * 
	 * @param stream an input stream of a binary java class
	 * @return a new FilamentClassNode defining the class from the input stream
	 * @throws IOException if a class cannot be read from the input stream
	 */
	public static FilamentClassNode loadFromStream(InputStream stream) throws IOException {
		FilamentClassNode node = new FilamentClassNode();
		ClassReader reader = new ClassReader(stream);
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
			} catch (Exception e) {}
		}
		node.access &= ~(Opcodes.ACC_FINAL | Opcodes.ACC_PROTECTED | Opcodes.ACC_PRIVATE);
		node.access |= Opcodes.ACC_PUBLIC;
		return node;
	}
	
	/**
	 * Check if this class contains a specific String constant.
	 * 
	 * @param str a String constant to check for
	 * @return <code>true</code> if this class contains the specified String
	 */
	public boolean containsConstant(String str) {
		return constants.contains(str);
	}
	
	/**
	 * Get a list of String constants contained within this class.
	 * 
	 * @return a list of String constants
	 */
	public List<String> getConstants() {
		return constants;
	}
	
	/**
	 * Get a list of all ASM types referenced by this class.
	 * 
	 * @return a list of referenced types 
	 * @see Type
	 */
	public List<Type> getReferences() {
		return references;
	}

	/**
	 * Check if this class matches a specified class hook.
	 * 
	 * @param hook a class hook to compare
	 * @return <code>true</code> if the classes have the same name and package
	 * @throws BadHookException if the specified hook is undefined or is the wrong type
	 * @see Hooks
	 */
	public boolean matches(String hook) throws BadHookException {
		FilamentClassNode node = Hooks.getClass(hook);
		return this.equals(node);
	}
	
	/**
	 * Get the ASM {@link Type} of this class.
	 * 
	 * @return an ASM {@link Type} representing this class. 
	 * @see Type
	 */
	public Type getType() {
		return Type.getObjectType(name);
	}

	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (obj instanceof FilamentClassNode) {
			return this.name.equals(((FilamentClassNode) obj).name);
		} else if (obj instanceof Type) {
			String typeName = null;
			try {
				typeName = ((Type) obj).getInternalName();
			} catch (Exception e) {}
			return this.name.equals(typeName);
		} else if (obj instanceof Class) {
			return this.name.equals(Type.getInternalName((Class<?>) obj));
		} else return false;
	}

	public int hashCode() {
		return super.hashCode();
	}
	
	public String toString() {
		return this.name.replace('/', '.');
	}
}
