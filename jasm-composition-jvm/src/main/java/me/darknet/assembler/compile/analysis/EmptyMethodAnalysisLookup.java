package me.darknet.assembler.compile.analysis;

import java.util.Collections;
import java.util.Map;

/**
 * Dummy lookup which provides no data.
 */
public class EmptyMethodAnalysisLookup implements MethodAnalysisLookup {
	private static final EmptyMethodAnalysisLookup instance = new EmptyMethodAnalysisLookup();

	/**
	 * @return Singleton instance.
	 */
	public static EmptyMethodAnalysisLookup instance() {
		return instance;
	}

	private EmptyMethodAnalysisLookup() {
	}

	@Override
	public Map<String, AnalysisResults> allResults() {
		return Collections.emptyMap();
	}

	@Override
	public AnalysisResults results(String name, String descriptor) {
		return null;
	}
}
