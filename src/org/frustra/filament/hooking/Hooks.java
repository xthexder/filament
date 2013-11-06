package org.frustra.filament.hooking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;

import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

public class Hooks {
	private static HashMap<String, Object> hooks = new HashMap<String, Object>();
	
	public static void reset() {
		hooks.clear();
	}
	
	public static void outputHooks() {
		try {
			ArrayList<String> output = new ArrayList<String>();
			for (Entry<String, Object> entry : hooks.entrySet()) {
				String key = entry.getKey();
				Object value = entry.getValue();
				if (value instanceof CustomClassNode) {
					output.add(key + " = " + ((CustomClassNode) value).name);
				} else if (value instanceof FieldNode) {
					output.add(key + " = " + ((FieldNode) value).name);
				} else if (value instanceof MethodNode) {
					output.add(key + " = " + ((MethodNode) value).name + ((MethodNode) value).desc);
				} else {
					output.add(key + " = " + value);
				}
			}
			String[] output2 = output.toArray(new String[0]);
			Arrays.sort(output2);
			for (String line : output2) {
				System.out.println(line);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static CustomClassNode getClass(String name) { return (CustomClassNode) get(name); }
	public static String getClassName(String name) { return ((CustomClassNode) get(name)).name; }
	public static FieldNode getField(String name) { return (FieldNode) get(name); }
	public static MethodNode getMethod(String name) { return (MethodNode) get(name); }
	
	public static Object get(String name) {
		return hooks.get(name);
	}
	
	public static void set(String name, Object value) {
		hooks.put(name, value);
	}
}
