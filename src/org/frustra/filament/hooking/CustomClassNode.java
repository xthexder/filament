package org.frustra.filament.hooking;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

public class CustomClassNode extends ClassNode {
	public ArrayList<String> constants = new ArrayList<String>();
	public ArrayList<Type> references = new ArrayList<Type>();

	public static CustomClassNode loadFromStream(InputStream stream) throws IOException {
		CustomClassNode node = new CustomClassNode();
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

	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (obj instanceof CustomClassNode) {
			return this.name.equals(((CustomClassNode) obj).name);
		} else return false;
	}

	public int hashCode() {
		return super.hashCode();
	}
}
