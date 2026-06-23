package me.darknet.assembler.compiler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ReflectiveTypeAwareness implements TypeAwareness {
	public static final ReflectiveTypeAwareness INSTANCE =
			new ReflectiveTypeAwareness(ReflectiveTypeAwareness.class.getClassLoader());

	private static final Map<String, Boolean> cache = new ConcurrentHashMap<>();

	private final ClassLoader loader;

	public ReflectiveTypeAwareness(ClassLoader loader) {
		this.loader = loader;
	}

	@Override
	public boolean isAwareOf(String type) {
		return cache.computeIfAbsent(type, this::checkType);
	}

	@Override
	public String notifyUnknownType(String type) {
		return "Type '" + type + "' is not present and any StackMapTable references will degrade to 'java/lang/Object'";
	}

	public static Map<String, Boolean> getCachedResults() {
		return cache;
	}

	private Boolean checkType(String type) {
		try {
			Class.forName(type.replace('/', '.'), false, this.loader);
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}
}
