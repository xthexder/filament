package org.frustra.filament.injection.annotations;

import java.lang.reflect.Field;
import java.util.HashMap;

import org.frustra.filament.FilamentStorage;
import org.frustra.filament.hooking.CustomClassNode;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;

public class AnnotationHelper {
	public String annotation;
	public HashMap<String, Object> values = new HashMap<String, Object>();
	public static String hookPackage = null;

	public AnnotationHelper(AnnotationNode node) {
		annotation = Type.getType(node.desc).getClassName();
		if (node.values != null) {
			for (int i = 0; i < node.values.size(); i += 2) {
				values.put((String) node.values.get(i), node.values.get(i + 1));
			}
		}
	}

	public static AnnotationHelper[] getAnnotations(CustomClassNode node) {
		if (node == null || node.visibleAnnotations == null) return new AnnotationHelper[0];
		AnnotationHelper[] annos = new AnnotationHelper[node.visibleAnnotations.size()];
		for (int i = 0; i < annos.length; i++) {
			annos[i] = new AnnotationHelper((AnnotationNode) node.visibleAnnotations.get(i));
		}
		return annos;
	}

	public static AnnotationHelper[] getAnnotations(MethodNode m) {
		if (m == null || m.visibleAnnotations == null) return new AnnotationHelper[0];
		AnnotationHelper[] annos = new AnnotationHelper[m.visibleAnnotations.size()];
		for (int i = 0; i < annos.length; i++) {
			annos[i] = new AnnotationHelper((AnnotationNode) m.visibleAnnotations.get(i));
		}
		return annos;
	}

	public Object getHook() {
		return getHook("value");
	}

	public Object getHook(String name) {
		try {
			String[] hook = ((String) values.get(name)).split("\\.");
			if (hook.length != 2) return null;
			Class<?> cls = FilamentStorage.store.classLoader.loadClass(hookPackage + "." + hook[0]);
			Field f = cls.getDeclaredField(hook[1]);
			f.setAccessible(true);
			return f.get(null);
		} catch (Exception e) {
			return null;
		}
	}
}
