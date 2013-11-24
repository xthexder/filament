package org.frustra.filament.hooking;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.frustra.filament.Hooks;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

public class FilamentClassNode extends ClassNode {
	public ArrayList<String> constants = new ArrayList<String>();
	public ArrayList<Type> references = new ArrayList<Type>();

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
	
	public boolean matches(String hook) throws BadHookException {
		FilamentClassNode node = Hooks.getClass(hook);
		return this.equals(node);
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
		} else return false;
	}

	public int hashCode() {
		return super.hashCode();
	}
}
