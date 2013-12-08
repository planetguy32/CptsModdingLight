package kovukore.asm.transformer;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.*;

public class ASMClassTransformer
{
	public ASMClassTransformer(HashMap<String, String> classesToTransform, HashMap<String, String> fieldRepl, HashMap<String, String> MethodRepl)
	{
		c2t = classesToTransform;
		fr = fieldRepl;
		mr = MethodRepl;
	}

	HashMap<String, String> c2t = null;
	HashMap<String, String> fr = null;
	HashMap<String, String> mr = null;

	public byte[] transform(String s, byte[] bytes)
	{
		String nameToMatch = s.replace('/', '.');
		if (c2t.containsKey(nameToMatch))
		{
			return transformClass(bytes, c2t.get(nameToMatch));
		}
		{
			return bytes;	
		}
	}

	protected byte[] transformClass(byte[] bytes, String clssname)
	{
		InputStream is = getClass().getResourceAsStream("/" + clssname.replace('.', '/') + ".class");
		ClassReader orig = null;
		try
		{
			ClassReader crRepl = new ClassReader(is);
			ClassNode cnRepl = new ClassNode(Opcodes.ASM4);
			crRepl.accept(cnRepl, ClassReader.SKIP_FRAMES);
			ClassReader crOrig = new ClassReader(bytes);
			ClassNode cnOrig = new ClassNode(Opcodes.ASM4);
			crOrig.accept(cnOrig, ClassReader.SKIP_FRAMES);
			for (Object ofnRepl : cnRepl.fields)
			{
				FieldNode fnRepl = (FieldNode) ofnRepl;
				if (hasReplaceAnnotation(fnRepl.visibleAnnotations))
				{
					FieldNode fnOrig = findField(cnOrig.fields, fnRepl);
					if (fnOrig != null)
					{
						cnOrig.fields.remove(fnOrig);
						cnOrig.fields.add(cnOrig.fields.size(), scrubField(cnOrig, cnRepl, fnRepl));
					}
				}
				else if (hasAddAnnotation(fnRepl.visibleAnnotations))
				{
					cnOrig.fields.add(cnOrig.fields.size(), scrubField(cnOrig, cnRepl, fnRepl));
				}
			}
			for (Object omnRepl : cnRepl.methods)
			{
				MethodNode mnRepl = (MethodNode) omnRepl;
				if (hasReplaceAnnotation(mnRepl.visibleAnnotations))
				{
					MethodNode mnOrig = findMethod(cnOrig.methods, mnRepl);
					if (mnOrig != null)
					{
						cnOrig.methods.remove(mnOrig);
						cnOrig.methods.add(cnOrig.methods.size(), scrubMethod(cnOrig, cnRepl, mnRepl));
					}
				}
				else if (hasAddAnnotation(mnRepl.visibleAnnotations))
				{
					cnOrig.methods.add(cnOrig.methods.size() + 1, scrubMethod(cnOrig, cnRepl, mnRepl));
				}
			}
			{
				ClassWriter cwNew = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
				cnOrig.accept(cwNew);
				return cwNew.toByteArray();	
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		{
			return bytes;	
		}
	}

	protected FieldNode findField(List fields, FieldNode fnRepl)
	{
		String find = fnRepl.name;
		if (fr.containsKey(fnRepl.name))
		{
			find = fr.get(fnRepl.name);
		}
		for (Object ofn : fields)
		{
			FieldNode fn = (FieldNode) ofn;

			if (fn.name.equals(find) && fn.desc.equals(fnRepl.desc))
			{
				return fn;
			}
		}
		{
			return null;	
		}
	}

	protected FieldNode scrubField(ClassNode cnOrig, ClassNode cnRepl, FieldNode fnRepl)
	{
		if (fnRepl.desc.equals(cnRepl.name))
		{
			fnRepl.desc = fnRepl.desc.replace(cnRepl.name, cnOrig.name);
		}
		if (fr.containsKey(fnRepl.name))
		{
			fnRepl.name = fr.get(fnRepl.name);
		}
		{
			return fnRepl;	
		}
	}

	protected MethodNode scrubMethod(ClassNode orig, ClassNode repl, MethodNode mn)
	{
		if (mr.containsKey(mn.name))
		{
			mn.name = mr.get(mn.name);
		}
		for (Object oinsn : mn.instructions.toArray())
		{
			if (oinsn instanceof FieldInsnNode)
			{
				if (((FieldInsnNode) oinsn).owner.equals(repl.name))
				{
					((FieldInsnNode) oinsn).owner = orig.name;
				}
				if (fr.containsKey(((FieldInsnNode) oinsn).name))
				{
					((FieldInsnNode) oinsn).name = fr.get(((FieldInsnNode) oinsn).name);
				}
			}
			if (oinsn instanceof MethodInsnNode)
			{
				if (((MethodInsnNode) oinsn).owner.equals(repl.name))
				{
					((MethodInsnNode) oinsn).owner = orig.name;
				}
				if (mr.containsKey(((MethodInsnNode) oinsn).name))
				{
					((MethodInsnNode) oinsn).name = mr.get(((MethodInsnNode) oinsn).name);
				}
			}
		}
		{
			return mn;	
		}
	}

	private MethodNode findMethod(List methods, MethodNode mnRepl)
	{
		String find = mnRepl.name;
		if (mr.containsKey(mnRepl.name))
		{
			find = mr.get(mnRepl.name);
		}
		for (Object omn : methods)
		{
			MethodNode mn = (MethodNode) omn;

			if (mn.name.equals(find) && mn.desc.equals(mnRepl.desc))
			{
				return mn;
			}
		}
		{
			return null;
		}
	}

	protected boolean hasReplaceAnnotation(List<AnnotationNode> nodes)
	{
		if (nodes == null)
		{
			return false;
		}
		for (AnnotationNode an : nodes)
		{
			if (an.desc != null && an.desc.contains("ASMReplace"))
			{
				return true;	
			}
		}
		{
			return false;	
		}
	}

	protected boolean hasAddAnnotation(List<AnnotationNode> nodes)
	{
		if (nodes == null)
		{
			return false;
		}
		for (AnnotationNode an : nodes)
		{
			if (an.desc != null && an.desc.contains("ASMAdd"))
			{
				return true;	
			}
		}
		{
			return false;	
		}
	}
}