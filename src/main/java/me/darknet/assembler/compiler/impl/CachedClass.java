package me.darknet.assembler.compiler.impl;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import me.darknet.assembler.parser.AssemblerException;
import me.darknet.assembler.parser.groups.*;
import me.darknet.assembler.parser.groups.module.*;
import me.darknet.assembler.transform.ClassGroupVisitor;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CachedClass implements ClassGroupVisitor {
	private int version;
	private int access;
	private String type;
	private String superType = "java/lang/Object";
	private List<String> implementedTypes = new ArrayList<>();
	private List<AnnotationGroup> annotations = new ArrayList<>();
	private List<InnerClass> innerClasses = new ArrayList<>();
	private List<String> nestMembers = new ArrayList<>();
	private String permittedSubclass;
	private String nestHost;
	private String signatureType;
	private String sourceFile;
	private ASMBaseVisitor asmBaseVisitor;
	private ModuleNode moduleNode;

	public void build(ClassVisitor cv) throws AssemblerException {
		cv.visit(version, access, type, signatureType, superType, implementedTypes.toArray(new String[0]));
		for (AnnotationGroup annotation : annotations) {
			String desc = annotation.getClassGroup().content();
			AnnotationVisitor av = cv.visitAnnotation(desc, !annotation.isInvisible());
			for (AnnotationParamGroup param : annotation.getParams())
				ASMBaseVisitor.annotationParam(param, av);
			av.visitEnd();
		}
		cv.visitSource(sourceFile, null);
		for (InnerClass innerClass : innerClasses) {
			cv.visitInnerClass(innerClass.name, innerClass.outerName, innerClass.innerName, innerClass.access);
		}
		if (nestHost != null) {
			cv.visitNestHost(nestHost);
		}
		for (String nestMember : nestMembers) {
			cv.visitNestMember(nestMember);
		}
		// permitted subclasses
		if(permittedSubclass != null)
			cv.visitPermittedSubclass(permittedSubclass);
		if(moduleNode != null)
			moduleNode.accept(cv);
	}

	@Override
	public void visitAnnotation(AnnotationGroup annotation) {
		annotations.add(annotation);
	}

	@Override
	public void visitSignature(SignatureGroup signature) {
		signatureType = signature.getDescriptor().content();
	}

	@Override
	public void visitVersion(VersionGroup version) {
		this.version = version.getVersion();
	}

	@Override
	public void visitSourceFile(SourceFileGroup sourceFile) throws AssemblerException {
		this.sourceFile = sourceFile.getSourceFile();
	}

	@Override
	public void visitInnerClass(InnerClassGroup innerClass) throws AssemblerException {
		int access = asmBaseVisitor.getAccess(innerClass.getAccessMods());
		String name = innerClass.getName().content();
		String outerName = innerClass.getOuterName().content();
		String innerName = innerClass.getInnerName().content();
		this.innerClasses.add(new InnerClass(name, outerName, innerName, access));
	}

	@Override
	public void visitNestHost(NestHostGroup nestHost) throws AssemblerException {
		this.nestHost = nestHost.getHostName().content();
	}

	@Override
	public void visitNestMember(NestMemberGroup nestMember) throws AssemblerException {
		this.nestMembers.add(nestMember.getMemberName().content());
	}

	@Override
	public void visitModule(ModuleGroup module) throws AssemblerException {
		int access = asmBaseVisitor.getAccess(module.getAccessMods());
		String name = module.getName().content();
		String version = module.getVersion().content();
		this.moduleNode = new ModuleNode(name, access, version);
		List<ModuleRequireNode> requires = new ArrayList<>();
		for (RequireGroup require : module.getRequires()) {
			requires.add(new ModuleRequireNode(require.getModule().content(),
					asmBaseVisitor.getAccess(require.getAccessMods()), require.getVersion().content()));
		}
		this.moduleNode.requires = requires;
		List<ModuleExportNode> exports = new ArrayList<>();
		for (ExportGroup export : module.getExports()) {
			List<String> to = new ArrayList<>();
			for (IdentifierGroup id : export.getTo().getTo()) {
				to.add(id.content());
			}
			exports.add(new ModuleExportNode(export.getModule().content(),
					asmBaseVisitor.getAccess(export.getAccessMods()), to));
		}
		this.moduleNode.exports = exports;
		List<ModuleOpenNode> opens = new ArrayList<>();
		for (OpenGroup open : module.getOpens()) {
			List<String> to = new ArrayList<>();
			for (IdentifierGroup id : open.getTo().getTo()) {
				to.add(id.content());
			}
			opens.add(new ModuleOpenNode(open.getModule().content(),
					asmBaseVisitor.getAccess(open.getAccessMods()), to));
		}
		this.moduleNode.opens = opens;
		List<String> uses = new ArrayList<>();
		for (UseGroup use : module.getUses()) {
			uses.add(use.getService().content());
		}
		this.moduleNode.uses = uses;
		List<ModuleProvideNode> provides = new ArrayList<>();
		for (ProvideGroup provide : module.getProvides()) {
			List<String> with = new ArrayList<>();
			for (IdentifierGroup id : provide.getWith()) {
				with.add(id.content());
			}
			provides.add(new ModuleProvideNode(provide.getService().content(), with));
		}
		this.moduleNode.provides = provides;
		List<String> packages = new ArrayList<>();
		for (PackageGroup pkg : module.getPackages()) {
			packages.add(pkg.getPackageClass().content());
		}
		this.moduleNode.packages = packages;
		if(module.getMainClass() != null)
			this.moduleNode.mainClass = module.getMainClass().content();
	}

	@Override
	public void visitPermittedSubclass(PermittedSubclassGroup permittedSubclass) throws AssemblerException {
		this.permittedSubclass = permittedSubclass.getSubclass().content();
	}

	@Data
	private static class InnerClass {
		private final String name;
		private final String outerName;
		private final String innerName;
		private final int access;
	}
}
