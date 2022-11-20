package me.darknet.assembler.parser.groups;

import lombok.Getter;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.groups.module.*;
import me.darknet.assembler.util.GroupLists;

import java.util.List;

@Getter
public class ModuleGroup extends Group implements ClassAttributeGroup {

    private final AccessModsGroup accessMods;
    private final IdentifierGroup name;
    private final IdentifierGroup version;
    private final MainClassGroup mainClass;
    private final List<PackageGroup> packages;
    private final List<RequireGroup> requires;
    private final List<ExportGroup> exports;
    private final List<OpenGroup> opens;
    private final List<UseGroup> uses;
    private final List<ProvideGroup> provides;

    public ModuleGroup(Token token, AccessModsGroup accessMods, IdentifierGroup name, IdentifierGroup version, MainClassGroup mainClassGroup, List<PackageGroup> packages,
                       List<RequireGroup> requires, List<ExportGroup> exports, List<OpenGroup> opens, List<UseGroup> uses, List<ProvideGroup> provides) {
          super(GroupType.MODULE, token,
                  GroupLists.fromArray(accessMods, name, version, mainClassGroup, packages, requires, exports, opens, uses, provides));
          this.accessMods = accessMods;
          this.name = name;
          this.version = version;
          this.mainClass = mainClassGroup;
          this.packages = packages;
          this.requires = requires;
          this.exports = exports;
          this.opens = opens;
          this.uses = uses;
          this.provides = provides;
     }
}
