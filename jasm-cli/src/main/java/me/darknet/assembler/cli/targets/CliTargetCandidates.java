package me.darknet.assembler.cli.targets;

import me.darknet.assembler.target.TargetId;

import java.util.Iterator;

/**
 * Picocli completion candidates sourced from the bundled target registry.
 */
public final class CliTargetCandidates implements Iterable<String> {
	@Override
	public Iterator<String> iterator() {
		return CliTargets.registry().ids().stream().map(TargetId::value).iterator();
	}
}
