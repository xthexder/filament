package org.frustra.filament.hooking.types;

import org.frustra.filament.hooking.BadHookException;
import org.frustra.filament.hooking.FilamentClassNode;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

public abstract class InstructionProvider extends MethodProvider {
	protected AbstractInsnNode cInsn = null;

	public boolean match(FilamentClassNode node) throws BadHookException {
		return true;
	}

	public boolean match(FilamentClassNode node, MethodNode m) throws BadHookException {
		return true;
	}

	public abstract boolean match(FilamentClassNode node, MethodNode m, AbstractInsnNode insn) throws BadHookException;
	protected void complete(FilamentClassNode node) throws BadHookException {}
	protected void complete(FilamentClassNode node, MethodNode m) throws BadHookException {}
	protected abstract void complete(FilamentClassNode node, MethodNode m, AbstractInsnNode insn) throws BadHookException;

	public void reset() {
		super.reset();
		cInsn = null;
	}

	public boolean doMatch(FilamentClassNode node, MethodNode m, AbstractInsnNode insn) {
		try {
			if (match(node, m, insn)) {
				if (InstructionProvider.class.equals(getClass().getSuperclass())) {
					cNode = node;
					cMethod = m;
					cInsn = insn;
					matches++;
				}
				return true;
			} else return false;
		} catch (Throwable e) {
			error = e;
			return false;
		}
	}

	public void complete() throws BadHookException {
		super.complete();
		complete(cNode, cMethod, cInsn);
	}
}
