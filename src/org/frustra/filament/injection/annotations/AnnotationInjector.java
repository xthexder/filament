package org.frustra.filament.injection.annotations;

import java.util.List;

import org.frustra.filament.hooking.CustomClassNode;
import org.frustra.filament.injection.ClassInjector;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class AnnotationInjector extends ClassInjector {
	public boolean match(CustomClassNode node) {
		return node.visibleAnnotations != null && node.visibleAnnotations.size() > 0;
	}

	@SuppressWarnings("unchecked")
	public void inject(CustomClassNode node) {
		for (MethodNode m : (List<MethodNode>) node.methods) {
			AnnotationHelper[] annos = AnnotationHelper.getAnnotations(m);
			for (AnnotationHelper anno : annos) {
				try {
					if (anno.annotation.equals(ReplaceSuperClass.class.getName())) {
						AbstractInsnNode insn = m.instructions.getFirst();
						while (insn != null) {
							if (insn.getOpcode() == Opcodes.INVOKESPECIAL) {
								MethodInsnNode minsn = (MethodInsnNode) insn;
								if (minsn.owner.equals(node.superName)) {
									minsn.owner = ((CustomClassNode) anno.getHook()).name;
								}
							}
							insn = insn.getNext();
						}
					} else if (anno.annotation.equals(OverrideMethod.class.getName())) {
						MethodNode parent = (MethodNode) anno.getHook();
						m.access = parent.access & ~Opcodes.ACC_ABSTRACT;
						m.name = parent.name;
						m.desc = parent.desc;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		AnnotationHelper[] annos = AnnotationHelper.getAnnotations(node);
		for (AnnotationHelper anno : annos) {
			try {
				if (anno.annotation.equals(ReplaceSuperClass.class.getName())) {
					node.superName = ((CustomClassNode) anno.getHook()).name;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
