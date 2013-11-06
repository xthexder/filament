package org.frustra.filament.injection.annotations;

import java.util.List;

import org.frustra.filament.hooking.CustomClassNode;
import org.frustra.filament.hooking.Hooks;
import org.frustra.filament.injection.ClassInjector;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

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
									minsn.owner = Hooks.getClassName(anno.getValue());
								}
							}
							insn = insn.getNext();
						}
					} else if (anno.annotation.equals(OverrideMethod.class.getName())) {
						MethodNode parent = Hooks.getMethod(anno.getValue());
						m.access = parent.access & ~Opcodes.ACC_ABSTRACT;
						m.name = parent.name;
						m.desc = parent.desc;
					} else if (anno.annotation.equals(ProxyMethod.class.getName())) {
						String hook = anno.getValue();
						CustomClassNode targetNode = Hooks.getClass(hook.substring(0, hook.lastIndexOf('.')));
						MethodNode targetMethod = Hooks.getMethod(hook);

						if ((m.access & Opcodes.ACC_STATIC) == 0) {
							throw new Exception("ProxyMethod annotated methods must be static.");
						}
						m.access &= ~Opcodes.ACC_NATIVE;

						int invokeOpcode = 0;
						if ((targetMethod.access & Opcodes.ACC_STATIC) != 0) {
							invokeOpcode = Opcodes.INVOKESTATIC;
						} else if ((targetNode.access & Opcodes.ACC_INTERFACE) != 0) {
							invokeOpcode = Opcodes.INVOKEINTERFACE;
						} else if (targetMethod.name.startsWith("<")) {
							invokeOpcode = Opcodes.INVOKESPECIAL;
						} else {
							invokeOpcode = Opcodes.INVOKEVIRTUAL;
						}

						int paramid = 0;
						m.instructions.clear();
						if (invokeOpcode != Opcodes.INVOKESTATIC) {
							m.instructions.add(new VarInsnNode(Opcodes.ALOAD, paramid++));
							m.instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, targetNode.name));
						}
						for (Type param : Type.getArgumentTypes(targetMethod.desc)) {
							int opcode = param.getOpcode(Opcodes.ILOAD);
							m.instructions.add(new VarInsnNode(opcode, paramid++));
							if (opcode == Opcodes.ALOAD) {
								m.instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, param.getInternalName()));
							}
						}
						m.instructions.add(new MethodInsnNode(invokeOpcode, targetNode.name, targetMethod.name, targetMethod.desc));
						Type ret = Type.getReturnType(targetMethod.desc);
						m.instructions.add(new InsnNode(ret.getOpcode(Opcodes.IRETURN)));
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
					node.superName = Hooks.getClassName(anno.getValue());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
