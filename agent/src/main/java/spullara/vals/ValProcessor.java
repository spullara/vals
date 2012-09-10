package spullara.vals;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import java.io.FileOutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static org.objectweb.asm.Opcodes.*;

/**
 * This transformer finds all uses of lazyval and val and simplifies them.
 */
public class ValProcessor implements ClassFileTransformer {

  private static Pattern SIG = Pattern.compile("\\(\\)Lspullara/vals/val<(.+)>");
  private static Logger log = Logger.getLogger("vals");

  public static void premain(String agentArgs, Instrumentation inst) {
    inst.addTransformer(new ValProcessor(), false);
    log.info("Installed");
  }

  interface AddVals {
    void add(ClassWriter cw, MethodVisitor mv);
  }

  public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
    try {
      ClassReader cr = new ClassReader(classfileBuffer);
      final List<AddVals> staticInit = new ArrayList<>();
      final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
      final AtomicBoolean found = new AtomicBoolean(false);
      cr.accept(new ClassVisitor(ASM4) {

        private String owner;

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
          owner = name;
          super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions) {
          if (desc.equals("()Lspullara/vals/val;")) {
            log.info("Found val declaration: " + name + " " + desc + " " + signature);
            // If static, get the static initializer - instance, get the constructor
            final String newname = "__val_" + name;
            if ((access & ACC_STATIC) == ACC_STATIC) {
              staticInit.add(new AddVals() {
                @Override
                public void add(ClassWriter cw, MethodVisitor mv) {
                  // Call the method and set the static field in the static
                  mv.visitMethodInsn(INVOKESTATIC, owner, newname, desc);
                  mv.visitFieldInsn(PUTSTATIC, owner, newname, desc.substring(2));
                }

                public String toString() {
                  return "static initializing " + name;
                }
              });
            }

            // Create the method that just returns what is in the static field
            GeneratorAdapter ga = new GeneratorAdapter(cw.visitMethod(ACC_PRIVATE | ACC_STATIC, name, desc, signature, exceptions), ACC_PRIVATE | ACC_STATIC, name, desc);
            ga.getStatic(Type.getType(owner), newname, Type.getType(desc.substring(2)));
            ga.returnValue();
            ga.endMethod();

            // Make a final field to store the value
            cw.visitField(ACC_STATIC | ACC_FINAL | ACC_PRIVATE, newname, desc.substring(2), signature.substring(2), null);

            // Now rename the current method

            return cw.visitMethod((access | ACC_PRIVATE) & ~ACC_PROTECTED & ~ACC_PUBLIC, newname, desc, signature, exceptions);
          } else if (name.equals("<clinit>")) {
            found.set(true);
          }
          return super.visitMethod(access, name, desc, signature, exceptions);
        }
      }, 0);
      if (staticInit.size() > 0) {
        if (found.get()) {
          cr.accept(new ClassVisitor(ASM4, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
              MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
              if (name.equals("<clinit>")) {
                return new MethodVisitor(ASM4, mv) {
                  @Override
                  public void visitCode() {
                    super.visitCode();
                    for (AddVals addVals : staticInit) {
                      addVals.add(cw, this);
                    }
                  }
                };
              }
              return mv;
            }
          }, 0);
        } else {
          GeneratorAdapter ga = new GeneratorAdapter(ACC_STATIC, Method.getMethod("void <clinit>()"), null, null, cw);
          for (AddVals addVals : staticInit) {
            addVals.add(cw, ga);
          }
          ga.returnValue();
          ga.endMethod();
          cr.accept(new ClassVisitor(ASM4, cw) {
            @Override
            public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions) {
              if (desc.equals("()Lspullara/vals/val;")) {
                return new MethodVisitor(ASM4) {
                  // Ignore
                };
              }
              return super.visitMethod(access, name, desc, signature, exceptions);
            }
          }, 0);
        }
      } else {
        cr.accept(cw, 0);
      }

      if (className.equals("spullara/vals/ValsTest")) {
        FileOutputStream fos = new FileOutputStream("ValsTest.class");
        fos.write(cw.toByteArray());
        fos.close();
      }
      return cw.toByteArray();
    } catch (Exception e) {
      e.printStackTrace();
      return classfileBuffer;
    }
  }
}
