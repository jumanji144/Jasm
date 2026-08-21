package me.darknet.assembler.parser.processor;

import me.darknet.assembler.ast.primitive.ASTDeclaration;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.TreeMap;

/**
 * Registry of declaration handlers.
 */
public final class DeclarationRegistry {
	private static final DeclarationHandler DEFAULT_HANDLER = (context, declaration) -> {
		ASTIdentifier keyword = declaration.keyword();
		String content = keyword == null ? "<missing>" : keyword.content();
		context.throwError("Unknown declaration: " + content, declaration.location());
		return null;
	};

	private final Map<String, DeclarationHandler> handlers = new TreeMap<>();

	/**
	 * @return Registry with all default handlers registered.
	 *
	 * @see AnnotationDeclarationParser
	 * @see ClassAttributeParsers
	 * @see FieldDeclarationParser
	 * @see MethodDeclarationParser
	 * @see ClassDeclarationParser
	 */
	public static @NotNull DeclarationRegistry createDefault() {
		DeclarationRegistry registry = new DeclarationRegistry();
		AnnotationDeclarationParser.register(registry);
		ClassAttributeParsers.register(registry);
		FieldDeclarationParser.register(registry);
		MethodDeclarationParser.register(registry);
		ClassDeclarationParser.register(registry);
		return registry;
	}

	/**
	 * Register a declaration handler for the given keyword.
	 *
	 * @param keyword
	 * 		Keyword to register the handler for.
	 * 		Should not include the leading {@code '.'}.
	 * @param handler
	 * 		Handler to register.
	 */
	public void register(@NotNull String keyword, @NotNull DeclarationHandler handler) {
		handlers.put(keyword, handler);
	}

	/**
	 * @param declaration
	 * 		Declaration to get the handler for.
	 *
	 * @return Handler for the given declaration, or a default handler that throws an error if the declaration's keyword is not registered.
	 */
	public @NotNull DeclarationHandler get(ASTDeclaration declaration) {
		return handlers.getOrDefault(keyword(declaration), DEFAULT_HANDLER);
	}

	/**
	 * @param declaration
	 * 		Declaration to get the keyword for.
	 *
	 * @return Keyword of the given declaration, without the leading {@code '.'}, or an empty string if the declaration has no keyword.
	 */
	public static @NotNull String keyword(@NotNull ASTDeclaration declaration) {
		if (declaration.keyword() == null)
			return "";
		return declaration.keyword().content().substring(1);
	}
}
