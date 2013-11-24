package org.frustra.filament.hooking.types;

import org.frustra.filament.hooking.BadHookException;
import org.frustra.filament.hooking.FilamentClassNode;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

public abstract class InstructionHook extends MethodHook {
	protected AbstractInsnNode cInsn = null;

	protected boolean match(FilamentClassNode node) throws BadHookException {
		return true;
	}

	protected boolean match(FilamentClassNode node, MethodNode m) throws BadHookException {
		return true;
	}

	protected abstract boolean match(FilamentClassNode node, MethodNode m, AbstractInsnNode insn) throws BadHookException;

	protected void onComplete(FilamentClassNode node) {}

	protected void onComplete(FilamentClassNode node, MethodNode m) {}

	protected abstract void onComplete(FilamentClassNode node, MethodNode m, AbstractInsnNode insn);

	public void reset() {
		super.reset();
		cInsn = null;
	}

	public void doMatch(FilamentClassNode node, MethodNode m, AbstractInsnNode insn) {
		if (!valid || completed) return;
		try {
			if (!match(node)) return;
			if (!match(node, m)) return;
			if (!match(node, m, insn)) return;
		} catch (Throwable e) {
			error = e;
			valid = false;
			return;
		}
		if (matched) {
			valid = false;
		} else {
			cNode = node;
			cMethod = m;
			cInsn = insn;
			matched = true;
		}
	}

	public void doComplete() throws BadHookException {
		if (completed) return;
		assertValid();
		completed = true;
		onComplete(cNode);
		onComplete(cNode, cMethod);
		onComplete(cNode, cMethod, cInsn);
	}
}
