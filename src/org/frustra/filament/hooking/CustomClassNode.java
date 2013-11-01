package org.frustra.filament.hooking;

import java.util.ArrayList;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

public class CustomClassNode extends ClassNode {
	public ArrayList<String> constants = new ArrayList<String>();
	public ArrayList<Type> references = new ArrayList<Type>();

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
