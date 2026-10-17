package me.darknet.assembler.printer;

import me.darknet.assembler.ast.MetaComment;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTCode;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.primitive.ASTLabel;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.ast.specific.ASTField;
import me.darknet.assembler.ast.specific.ASTMember;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Extracts comments from an AST and maps them to their respective disassembly sites.
 */
public final class DisassemblyCommentExtractor {
    /** Pattern of generated try-catch ranges hint comments. */
    private static final Pattern GENERATED_TRY_RANGE = Pattern.compile(
            "^\\s*try-(?:start|end|handler):\\s+range=\\[[^\\r\\n]+\\]\\s+handler=\\S+:\\S+\\s*$");

    private DisassemblyCommentExtractor() {}

    /**
     * Extracts comments from the given AST elements and maps them to their respective disassembly sites.
     *
     * @param processedDeclarations
     *        The list of AST elements to extract comments from.
     *
     * @return A {@link DisassemblyCommentMap} containing the extracted comments and their corresponding sites.
     */
    public static @NotNull DisassemblyCommentMap extract(@NotNull List<ASTElement> processedDeclarations) {
        return extract(processedDeclarations, null);
    }

    /**
     * Extracts comments from the given AST elements and maps them to their respective disassembly sites.
     *
     * @param processedDeclarations
     *        The list of AST elements to extract comments from.
     * @param topLevelOwnerInternalName
     *        The internal name of the top-level owner class, if applicable.
     *        This is used for top-level fields and methods.
     *
     * @return A {@link DisassemblyCommentMap} containing the extracted comments and their corresponding sites.
     */
    public static @NotNull DisassemblyCommentMap extract(@NotNull List<ASTElement> processedDeclarations,
                                                          @Nullable String topLevelOwnerInternalName) {
        Objects.requireNonNull(processedDeclarations, "processedDeclarations");
        Extractor extractor = new Extractor(topLevelOwnerInternalName);
        List<ASTMember> topLevelMembers = processedDeclarations.stream()
                .filter(element -> element instanceof ASTField || element instanceof ASTMethod)
                .map(element -> (ASTMember) element)
                .toList();

        for (ASTElement declaration : processedDeclarations) {
            if (declaration instanceof ASTClass clazz) {
                extractor.processClass(clazz);
            } else if (declaration instanceof ASTField field) {
                extractor.processTopLevelMember(field, topLevelMembers);
            } else if (declaration instanceof ASTMethod method) {
                extractor.processTopLevelMember(method, topLevelMembers);
            }
        }
        return extractor.finish();
    }

    /**
     * Placement of a comment relative to an AST element.
     */
    private enum Placement { LEADING, TRAILING, END }

    /**
     * Represents a comment and its associated disassembly site.
     */
    private record Entry(DisassemblyCommentSite site, MetaComment comment) {}

    /**
     * Represents a reference to an AST member and its associated disassembly site.
     */
    private record MemberRef(ASTMember member, DisassemblyCommentSite site) {}

    /**
     * Helper class to extract comments from AST elements and map them to their respective disassembly sites.
     */
    private static final class Extractor {
        private final @Nullable String topLevelOwner;
        private final List<Entry> entries = new ArrayList<>();

        private Extractor(@Nullable String topLevelOwner) {
            this.topLevelOwner = topLevelOwner;
        }

        private void processClass(ASTClass clazz) {
            String owner = clazz.getName().content();
            DisassemblyCommentSite.ClassSite classSite = new DisassemblyCommentSite.ClassSite(owner);
            DisassemblyCommentSite.ClassEndSite classEnd = new DisassemblyCommentSite.ClassEndSite(owner);
            List<MemberRef> members = clazz.contents().stream()
                    .filter(element -> element instanceof ASTField || element instanceof ASTMethod)
                    .map(element -> memberRef((ASTMember) element, owner))
                    .toList();

            Location classLocation = clazz.getName().location();
            for (MetaComment comment : clazz.leadingComments()) {
                if (classLocation != null && comment.location().compareTo(classLocation) < 0)
                    add(classSite, comment);
                else
                    add(classScopeSite(Placement.LEADING, comment, members, classSite, classEnd), comment);
            }
            for (MetaComment comment : clazz.trailingComments()) {
                if (classLocation != null && comment.location().line() == classLocation.line())
                    add(classEnd, comment);
                else
                    add(classScopeSite(Placement.TRAILING, comment, members, classSite, classEnd), comment);
            }
            addAll(classEnd, clazz.endComments());

            Set<ASTElement> visited = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
            for (ASTElement child : clazz.children()) {
                if (!visited.add(child))
                    continue;
                if (child instanceof ASTField || child instanceof ASTMethod) {
                    MemberRef ref = findRef(members, (ASTMember) child);
                    if (ref != null)
                        processMember(ref, members, true);
                } else {
                    processClassMetadata(child, members, classSite, classEnd, visited);
                }
            }
        }

        private void processClassMetadata(ASTElement element, List<MemberRef> members,
                                          DisassemblyCommentSite.ClassSite classSite,
                                          DisassemblyCommentSite.ClassEndSite classEnd,
                                          Set<ASTElement> visited) {
            addByPlacement(element, Placement.LEADING,
                    comment -> classScopeSite(Placement.LEADING, comment, members, classSite, classEnd));
            addByPlacement(element, Placement.TRAILING,
                    comment -> classScopeSite(Placement.TRAILING, comment, members, classSite, classEnd));
            addAll(classEnd, element.endComments());
            for (ASTElement child : element.children()) {
                if (visited.add(child))
                    processClassMetadata(child, members, classSite, classEnd, visited);
            }
        }

        private void processTopLevelMember(ASTMember member, List<ASTMember> allMembers) {
            if (hasComments(member) && topLevelOwner == null)
                throw new IllegalArgumentException("Top-level field or method comments require a class owner");
            if (topLevelOwner == null)
                return;
            List<MemberRef> refs = allMembers.stream().map(topMember -> memberRef(topMember, topLevelOwner)).toList();
            MemberRef ref = findRef(refs, member);
            if (ref != null)
                processMember(ref, refs, false);
        }

        private void processMember(MemberRef ref, List<MemberRef> siblings, boolean classContained) {
            ASTMember member = ref.member();
            DisassemblyCommentSite memberSite = ref.site();
            DisassemblyCommentSite fallback = memberSite instanceof DisassemblyCommentSite.FieldSite fieldSite
                    ? new DisassemblyCommentSite.ClassSite(fieldSite.classInternalName())
                    : memberSite instanceof DisassemblyCommentSite.MethodSite methodSite
                    ? new DisassemblyCommentSite.ClassSite(methodSite.classInternalName())
                    : memberSite;
            for (MetaComment comment : member.leadingComments()) {
                add(classContained ? memberScopeSite(Placement.LEADING, comment, siblings, ref, fallback)
                        : rootMemberScopeSite(Placement.LEADING, comment, siblings, ref), comment);
            }
            for (MetaComment comment : member.trailingComments()) {
                add(classContained ? memberScopeSite(Placement.TRAILING, comment, siblings, ref, fallback)
                        : rootMemberScopeSite(Placement.TRAILING, comment, siblings, ref), comment);
            }
            if (member instanceof ASTMethod method) {
                DisassemblyCommentSite.MethodSite methodSite = (DisassemblyCommentSite.MethodSite) memberSite;
                addAll(new DisassemblyCommentSite.MethodEndSite(methodSite), member.endComments());
                CodeSites codeSites = method.getCode() == null ? null : new CodeSites(methodSite, method.getCode());
                walkMember(method, ref, codeSites, null);
            } else {
                if (classContained) {
                    addAll(memberSite, member.endComments());
                } else {
                    addAll(new DisassemblyCommentSite.ClassEndSite(topLevelOwner), member.endComments());
                }
                walkMember(member, ref, null, null);
            }
        }

        private void walkMember(ASTElement element, MemberRef ref, @Nullable CodeSites codeSites,
                                @Nullable ASTInstruction currentInstruction) {
            ASTMember member = ref.member();
            if (element != member) {
                if (codeSites != null && element == codeSites.code) {
                    addAll(ref.site(), element.leadingComments());
                    addAll(ref.site(), element.trailingComments());
                    addAll(new DisassemblyCommentSite.MethodEndSite((DisassemblyCommentSite.MethodSite) ref.site()),
                            element.endComments());
                } else if (currentInstruction != null || element instanceof ASTInstruction) {
                    ASTInstruction instruction = element instanceof ASTInstruction current ? current : currentInstruction;
                    if (instruction != null && codeSites != null)
                        addInstructionComments(element, instruction, codeSites);
                } else {
                    addAll(ref.site(), element.leadingComments());
                    addAll(ref.site(), element.trailingComments());
                    addAll(ref.site(), element.endComments());
                }
            }

            ASTInstruction nestedInstruction = element instanceof ASTInstruction instruction
                    ? instruction : currentInstruction;
            for (ASTElement child : element.children())
                walkMember(child, ref, codeSites, nestedInstruction);
        }

        private void addInstructionComments(ASTElement element, ASTInstruction instruction, CodeSites sites) {
            addByPlacement(element, Placement.LEADING,
                    comment -> sites.siteFor(instruction, Placement.LEADING));
            addByPlacement(element, Placement.TRAILING,
                    comment -> sites.siteFor(instruction, Placement.TRAILING));
            addByPlacement(element, Placement.END,
                    comment -> sites.siteFor(instruction, Placement.END));
        }

        private DisassemblyCommentSite classScopeSite(Placement placement, MetaComment comment,
                                                       List<MemberRef> members,
                                                       DisassemblyCommentSite.ClassSite classSite,
                                                       DisassemblyCommentSite.ClassEndSite classEnd) {
            return switch (placement) {
                case LEADING -> nextMember(comment.location(), members) != null
                        ? nextMember(comment.location(), members).site() : classEnd;
                case TRAILING -> previousMember(comment.location(), members) != null
                        ? previousMember(comment.location(), members).site() : classSite;
                case END -> classEnd;
            };
        }

        private DisassemblyCommentSite memberScopeSite(Placement placement, MetaComment comment,
                                                        List<MemberRef> siblings, MemberRef current,
                                                        DisassemblyCommentSite fallback) {
            return switch (placement) {
                case LEADING -> {
                    MemberRef next = nextMember(comment.location(), siblings);
                    yield next == null ? current.site() : next.site();
                }
                case TRAILING -> {
                    MemberRef previous = previousMember(comment.location(), siblings);
                    yield previous == null ? fallback : previous.site();
                }
                case END -> current.site();
            };
        }

        private DisassemblyCommentSite rootMemberScopeSite(Placement placement, MetaComment comment,
                                                           List<MemberRef> siblings, MemberRef current) {
            return switch (placement) {
                case LEADING -> {
                    MemberRef next = nextMember(comment.location(), siblings);
                    yield next == null ? current.site() : next.site();
                }
                case TRAILING -> {
                    MemberRef previous = previousMember(comment.location(), siblings);
                    yield previous == null ? current.site() : previous.site();
                }
                case END -> current.site();
            };
        }

        private static @Nullable MemberRef nextMember(Location location, List<MemberRef> members) {
            return members.stream()
                    .filter(member -> member.member().location() != null
                            && member.member().location().compareTo(location) >= 0)
                    .min(Comparator.comparing(member -> member.member().location()))
                    .orElse(null);
        }

        private static @Nullable MemberRef previousMember(Location location, List<MemberRef> members) {
            return members.stream()
                    .filter(member -> member.member().location() != null
                            && member.member().location().compareTo(location) <= 0)
                    .max(Comparator.comparing(member -> member.member().location()))
                    .orElse(null);
        }

        private static @Nullable MemberRef findRef(List<MemberRef> refs, ASTMember member) {
            for (MemberRef ref : refs) {
                if (ref.member() == member)
                    return ref;
            }
            return null;
        }

        private static MemberRef memberRef(ASTMember member, String owner) {
            if (member instanceof ASTField field)
                return new MemberRef(field, new DisassemblyCommentSite.FieldSite(owner,
                        field.getName().content(), field.getDescriptor().content()));
            ASTMethod method = (ASTMethod) member;
            return new MemberRef(method, new DisassemblyCommentSite.MethodSite(owner,
                    method.getName().content(), method.getDescriptor().content()));
        }

        private void addByPlacement(ASTElement element, Placement placement,
                                    java.util.function.Function<MetaComment, DisassemblyCommentSite> site) {
            List<MetaComment> comments = switch (placement) {
                case LEADING -> element.leadingComments();
                case TRAILING -> element.trailingComments();
                case END -> element.endComments();
            };
            comments.forEach(comment -> add(site.apply(comment), comment));
        }

        private void addAll(DisassemblyCommentSite site, List<MetaComment> comments) {
            comments.forEach(comment -> add(site, comment));
        }

        private void add(DisassemblyCommentSite site, MetaComment comment) {
            entries.add(new Entry(site, comment));
        }

        private DisassemblyCommentMap finish() {
            entries.sort(Comparator.comparing(entry -> entry.comment().location()));
            Map<DisassemblyCommentSite, List<String>> comments = new LinkedHashMap<>();
            DisassemblyCommentSite groupSite = null;
            StringBuilder group = new StringBuilder();
            int previousEndLine = -1;
            for (Entry entry : entries) {
                String body = entry.comment().content();
                if (GENERATED_TRY_RANGE.matcher(body).matches())
                    continue;
                if (groupSite != null && !groupSite.equals(entry.site())) {
                    comments.computeIfAbsent(groupSite, ignored -> new ArrayList<>()).add(group.toString());
                    group.setLength(0);
                    previousEndLine = -1;
                    groupSite = null;
                }
                if (groupSite == null) {
                    groupSite = entry.site();
                    group.append(body);
                } else {
                    int startLine = entry.comment().location().line();
                    int lineBreaks = Math.max(1, startLine - previousEndLine);
                    group.append("\n".repeat(lineBreaks)).append(body);
                }
                previousEndLine = entry.comment().location().line() + countNewlines(body);
            }
            if (groupSite != null)
                comments.computeIfAbsent(groupSite, ignored -> new ArrayList<>()).add(group.toString());
            return new DisassemblyCommentMap(comments);
        }

        private static int countNewlines(String value) {
            int count = 0;
            for (int i = 0; i < value.length(); i++) {
                if (value.charAt(i) == '\n')
                    count++;
            }
            return count;
        }

        private static boolean hasComments(ASTElement element) {
            if (!element.leadingComments().isEmpty() || !element.trailingComments().isEmpty()
                    || !element.endComments().isEmpty())
                return true;
            return element.children().stream().anyMatch(Extractor::hasComments);
        }
    }

    /**
     * Helper class to map AST instructions to their respective disassembly sites.
     */
    private static final class CodeSites {
        private final DisassemblyCommentSite.MethodSite method;
        private final ASTCode code;
        private final List<ASTInstruction> instructions;
        private final Map<ASTInstruction, Integer> positions = new IdentityHashMap<>();
        private final Map<ASTInstruction, Integer> ordinals = new IdentityHashMap<>();
        private final int executableCount;

        private CodeSites(DisassemblyCommentSite.MethodSite method, ASTCode code) {
            this.method = method;
            this.code = code;
            this.instructions = code.getInstructions();
            int ordinal = 0;
            for (ASTInstruction instruction : instructions) {
                positions.put(instruction, ordinal);
                if (!isLabel(instruction) && !isLinePseudo(instruction)) {
                    ordinals.put(instruction, ordinal);
                    ordinal++;
                }
            }
            this.executableCount = ordinal;
        }

        private DisassemblyCommentSite siteFor(ASTInstruction instruction, Placement placement) {
            int position = positions.getOrDefault(instruction, executableCount);
            if (isLabel(instruction))
                return position >= executableCount ? new DisassemblyCommentSite.MethodEndSite(method)
                        : new DisassemblyCommentSite.LabelSite(method, position);
            if (!isLinePseudo(instruction))
                return new DisassemblyCommentSite.InstructionSite(method,
                        ordinals.getOrDefault(instruction, Math.min(position, executableCount)));
            if (placement == Placement.END)
                return new DisassemblyCommentSite.MethodEndSite(method);
            ASTInstruction adjacent = placement == Placement.TRAILING
                    ? previousSiteInstruction(instruction) : nextSiteInstruction(instruction);
            if (adjacent == null && placement == Placement.TRAILING)
                adjacent = nextSiteInstruction(instruction);
            if (adjacent == null)
                return new DisassemblyCommentSite.MethodEndSite(method);
            return siteFor(adjacent, placement == Placement.TRAILING ? Placement.TRAILING : Placement.LEADING);
        }

        private @Nullable ASTInstruction previousSiteInstruction(ASTInstruction instruction) {
            int index = instructions.indexOf(instruction);
            for (int i = index - 1; i >= 0; i--) {
                ASTInstruction candidate = instructions.get(i);
                if (!isLinePseudo(candidate))
                    return candidate;
            }
            return null;
        }

        private @Nullable ASTInstruction nextSiteInstruction(ASTInstruction instruction) {
            int index = instructions.indexOf(instruction);
            for (int i = index + 1; i < instructions.size(); i++) {
                ASTInstruction candidate = instructions.get(i);
                if (!isLinePseudo(candidate))
                    return candidate;
            }
            return null;
        }

        private static boolean isLabel(ASTInstruction instruction) {
            return instruction instanceof ASTLabel;
        }

        private static boolean isLinePseudo(ASTInstruction instruction) {
            return instruction.identifier().content().equalsIgnoreCase("line");
        }
    }
}
