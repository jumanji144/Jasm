package dummy;

import dev.xdark.blw.util.LazyList;
import me.darknet.assembler.compile.analysis.jvm.AnalysisSimulation;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dummy class used to test {@link AnalysisSimulation}'s frame merging capability.
 * The 'c' variable should be inferred to be a non-null list by the end.
 */
public class Demo {
	@SuppressWarnings("all")
	public static void main(String[] args) {
		// Our variable is a collection, but with analysis we can
		// assert that it must be at least a List since all options
		// in the control flow path end up being a list.
		Collection c = null;
		switch (new Random().nextInt(5)) {
			case 0:
				c = new ArrayList();
				break;
			case 1:
				c = new CopyOnWriteArrayList();
				break;
			case 2:
				c = new LinkedList();
				break;
			case 3:
				c = new LazyList(() -> Collections.emptyList());
				break;
			case 4:
				c = Collections.emptyList();
				break;
			default:
				break;
		}

		// C should never be null
		if (c.isEmpty())
			c = List.of("Hello world");

		System.out.println("List: " + c);
	}
}
