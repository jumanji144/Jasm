package me.darknet.assembler.test;

import java.util.Map;

/**
 * Classloader implementation for loading a class from raw {@code byte[]}.
 */
public class ClassDefiner extends ClassLoader {
	private final Map<String, byte[]> classes;

	/**
	 * @param name
	 * 		Name of class.
	 * @param bytecode
	 * 		Bytecode of class.
	 */
	public ClassDefiner(String name, byte[] bytecode) {
		this(Map.of(name, bytecode));
	}

	/**
	 * @param classes
	 * 		Map of classes.
	 */
	public ClassDefiner(Map<String, byte[]> classes) {
		super(ClassDefiner.class.getClassLoader());
		this.classes = classes;
	}

	@Override
	protected final Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		// We must override this to prevent parent-first delegation.
		Class<?> c;
		if ((c = findLoadedClass(name)) != null) {
			return c;
		}
		synchronized (getClassLoadingLock(name)) {
			if ((c = findLoadedClass(name)) != null) {
				return c;
			}
			try {
				c = findClass(name);
			} catch (ClassNotFoundException e) {
				c = super.loadClass(name, resolve);
			}
			return c;
		}
	}

	@Override
	public final Class<?> findClass(String name) throws ClassNotFoundException {
		byte[] bytecode = classes.get(name);
		if (bytecode != null)
			return defineClass(name, bytecode, 0, bytecode.length, null);
		return super.findClass(name);
	}
}