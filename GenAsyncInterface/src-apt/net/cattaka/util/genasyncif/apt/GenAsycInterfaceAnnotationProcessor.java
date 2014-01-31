
package net.cattaka.util.genasyncif.apt;

import static javax.lang.model.util.ElementFilter.typesIn;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;

import net.cattaka.util.genasyncif.AsyncIfAttr;
import net.cattaka.util.genasyncif.GenAsyncInterface;

@SupportedSourceVersion(SourceVersion.RELEASE_6)
@SupportedAnnotationTypes("net.cattaka.util.genasyncif.*")
public class GenAsycInterfaceAnnotationProcessor extends AbstractProcessor {
    private static class ArgType {
        String typeName;

        String innerTypeName;

        private ArgType(String typeName, String innerTypeName) {
            super();
            this.typeName = typeName;
            this.innerTypeName = innerTypeName;
        }

        @Override
        public String toString() {
            return "ArgType [typeName=" + typeName + ", innerTypeName=" + innerTypeName + "]";
        }

    }

    private static class InterfaceInfo {
        String packageName;

        String interfaceName;

        String asyncClassName;

        public InterfaceInfo(String packageName, String className, String asyncClassName) {
            super();
            this.packageName = packageName;
            this.interfaceName = className;
            this.asyncClassName = asyncClassName;
        }

        @Override
        public String toString() {
            return "InterfaceInfo [packageName=" + packageName + ", className=" + interfaceName
                    + ", asyncClassName=" + asyncClassName + "]";
        }

    }

    private static class MethodInfo {
        boolean needSync;

        String methodName;

        String eventName;

        List<ArgType> argTypes;

        List<String> throwsList;

        ArgType returnType;

        public MethodInfo(boolean needSync, String methodName, String eventName,
                List<ArgType> argTypes, List<String> throwsList, ArgType returnType) {
            super();
            this.needSync = needSync;
            this.methodName = methodName;
            this.eventName = eventName;
            this.argTypes = argTypes;
            this.throwsList = throwsList;
            this.returnType = returnType;
        }

        @Override
        public String toString() {
            return "MethodInfo [methodName=" + methodName + ", argTypes=" + argTypes
                    + ", throwsList=" + throwsList + ", returnType=" + returnType + "]";
        }

    }

    private String mTemplate;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        InputStream in = getClass().getResourceAsStream("template.txt");
        try {
            Reader reader = new InputStreamReader(in, "UTF-8");
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[1 << 12];
            int r;
            while ((r = reader.read(buf)) > 0) {
                sb.append(buf, 0, r);
            }
            mTemplate = sb.toString();
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Kind.ERROR, e.getMessage());
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Kind.ERROR, e.getMessage());
            }
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement element : typesIn(roundEnv
                .getElementsAnnotatedWith(GenAsyncInterface.class))) {
            try {
                processElement(element);
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Kind.ERROR,
                        "Only single primary key is supported.", element);
            }
        }
        return true;
    }

    private void processElement(TypeElement srcElement) throws IOException {
        GenAsyncInterface gai = srcElement.getAnnotation(GenAsyncInterface.class);
        String packageName = getPackageName(srcElement);
        String className = String.valueOf(srcElement.getSimpleName());
        String asyncClassName = gai.prefix() + srcElement.getSimpleName() + gai.suffix();
        String qualifiedName = packageName + ".async." + asyncClassName;
        InterfaceInfo interfaceInfo = new InterfaceInfo(packageName, className, asyncClassName);

        Filer filer = processingEnv.getFiler();
        JavaFileObject fileObject = filer.createSourceFile(qualifiedName, srcElement);
        PrintWriter writer = new PrintWriter(fileObject.openWriter());
        try {
            generateCode(gai, srcElement, writer, interfaceInfo);
        } finally {
            writer.close();
        }

    }

    private void generateCode(GenAsyncInterface gai, TypeElement srcElement, PrintWriter writer,
            InterfaceInfo info) throws IOException {
        List<MethodInfo> methodInfos = pullMethodInfos(srcElement);
        String packageName = info.packageName + ".async";
        String importLines = "import " + info.packageName + "." + info.interfaceName + ";";
        String asyncClassName = info.asyncClassName;
        String interfaceName = info.interfaceName;
        String methodEventLines = "";
        for (int i = 0; i < methodInfos.size(); i++) {
            MethodInfo mi = methodInfos.get(i);
            methodEventLines += "    private static final int " + mi.eventName
                    + " = EVENT_START + " + i + ";\n";
        }
        String methodLines;
        int workSize = 0;
        {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < methodInfos.size(); i++) {
                MethodInfo mi = methodInfos.get(i);
                workSize = Math.max(workSize, mi.argTypes.size() + 4);
                sb.append("    @Override\n");
                sb.append("    public " + mi.returnType.typeName + " " + mi.methodName + "(");
                for (int j = 0; j < mi.argTypes.size(); j++) {
                    ArgType arg = mi.argTypes.get(j);
                    if (j > 0) {
                        sb.append(", ");
                    }
                    sb.append(arg.typeName + " arg" + j);
                }
                sb.append(")");
                if (mi.throwsList.size() > 0) {
                    sb.append(" throws ");
                    for (int j = 0; j < mi.throwsList.size(); j++) {
                        if (j > 0) {
                            sb.append(", ");
                        }
                        sb.append(mi.throwsList.get(0));
                    }

                }
                sb.append(" {\n");
                sb.append("        Object[] work = obtain();\n");
                sb.append("        work[0] = this;\n");
                sb.append("        work[1] = orig;\n");
                for (int j = 0; j < mi.argTypes.size(); j++) {
                    // ArgType arg = mi.argTypes.get(j);
                    sb.append("        work[" + (j + 2) + "] = arg" + j + ";\n");
                }
                if (!mi.needSync) {
                    sb.append("        mHandler.obtainMessage(" + mi.eventName
                            + ", work).sendToTarget();\n");
                } else {
                    sb.append("        synchronized (work) {\n");
                    sb.append("            mHandler.obtainMessage(" + mi.eventName + ", work)\n");
                    sb.append("                    .sendToTarget();\n");
                    sb.append("            try {\n");
                    sb.append("                work.wait();\n");
                    sb.append("            } catch (InterruptedException e) {\n");
                    sb.append("                throw new AsyncInterfaceException(e);\n");
                    sb.append("            }\n");
                    sb.append("        }\n");
                    sb.append("        if (work[WORK_SIZE - 1] != null) {\n");
                    if (mi.throwsList.size() == 0) {
                        sb.append("            throw new AsyncInterfaceException((Exception) work[WORK_SIZE - 1]);\n");
                    } else {
                        for (int j = 0; j < mi.throwsList.size(); j++) {
                            String t = mi.throwsList.get(j);
                            sb.append("            ");
                            if (j > 0) {
                                sb.append("} else ");
                            }
                            sb.append("if (work[WORK_SIZE - 1] instanceof " + t + ") {\n");
                            sb.append("                throw (" + t + ") work[WORK_SIZE - 1];\n");
                        }
                        sb.append("            } else {\n");
                        sb.append("                throw new AsyncInterfaceException((Exception) work[WORK_SIZE - 1]);\n");
                        sb.append("            }\n");
                    }
                    sb.append("        }\n");
                    if (!"void".equalsIgnoreCase(mi.returnType.typeName)) {
                        sb.append("        " + mi.returnType.typeName + " result = ("
                                + mi.returnType.innerTypeName + ") work[WORK_SIZE - 2];\n");
                    }
                    sb.append("        recycle(work);\n");
                    if (!"void".equalsIgnoreCase(mi.returnType.typeName)) {
                        sb.append("        return result;\n");
                    }
                }
                sb.append("    }\n");
            }
            methodLines = sb.toString();
        }
        String caseLines;
        {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < methodInfos.size(); i++) {
                MethodInfo mi = methodInfos.get(i);
                sb.append("            case " + mi.eventName + ": {\n");
                sb.append("                Object[] work = (Object[]) msg.obj;\n");
                if (!mi.needSync) {
                    sb.append("                ${asyncClassName} me = (${asyncClassName}) work[0];\n");
                }
                sb.append("                ${interfaceName} orig = (${interfaceName}) work[1];\n");
                for (int j = 0; j < mi.argTypes.size(); j++) {
                    ArgType arg = mi.argTypes.get(j);
                    sb.append("                " + arg.innerTypeName + " arg" + j + " = ("
                            + arg.innerTypeName + ") (work[" + (j + 2) + "]);\n");
                }

                if (!mi.needSync) {
                    sb.append("                orig." + mi.methodName + "(");
                    for (int j = 0; j < mi.argTypes.size(); j++) {
                        if (j > 0) {
                            sb.append(" ,");
                        }
                        sb.append("arg" + j);
                    }
                    sb.append(");\n");
                    sb.append("                me.recycle(work);\n");
                    sb.append("                return true;\n");
                } else {
                    sb.append("                try {\n");
                    sb.append("                    ");
                    if (!"void".equalsIgnoreCase(mi.returnType.typeName)) {
                        sb.append("work[WORK_SIZE - 2] = ");
                    }
                    sb.append("orig." + mi.methodName + "(");
                    for (int j = 0; j < mi.argTypes.size(); j++) {
                        if (j > 0) {
                            sb.append(" ,");
                        }
                        sb.append("arg" + j);
                    }
                    sb.append(");\n");
                    sb.append("                } catch (Exception e) {\n");
                    sb.append("                    work[WORK_SIZE - 1] = e;\n");
                    sb.append("                }\n");
                    sb.append("                synchronized (work) {\n");
                    sb.append("                    work.notify();\n");
                    sb.append("                }\n");
                    sb.append("                return true;\n");
                }
                sb.append("            }\n");
            }
            caseLines = sb.toString();
        }

        StringBuilder sb = new StringBuilder(mTemplate);
        replaceStringBuilder(sb, "${importLines}", importLines);
        replaceStringBuilder(sb, "${caseLines}", caseLines);
        replaceStringBuilder(sb, "${methodEventLines}", methodEventLines);
        replaceStringBuilder(sb, "${methodLines}", methodLines);
        replaceStringBuilder(sb, "${packageName}", packageName);
        replaceStringBuilder(sb, "${asyncClassName}", asyncClassName);
        replaceStringBuilder(sb, "${interfaceName}", interfaceName);
        replaceStringBuilder(sb, "${poolSize}", String.valueOf(gai.poolSize()));
        replaceStringBuilder(sb, "${workSize}", String.valueOf(workSize));

        writer.print(sb.toString());
    }

    public static List<MethodInfo> pullMethodInfos(Element element) {
        List<MethodInfo> methodInfos = new ArrayList<MethodInfo>();
        int count = 0;
        for (ExecutableElement method : ElementFilter.methodsIn(element.getEnclosedElements())) {
            AsyncIfAttr attr = method.getAnnotation(AsyncIfAttr.class);

            String methodName = method.getSimpleName().toString();
            String eventName = "EVENT_METHOD_" + count + "_" + methodName;

            List<ArgType> argTypes = new ArrayList<ArgType>();
            for (VariableElement arg : method.getParameters()) {
                argTypes.add(createArgType(arg.asType()));
            }
            // for (TypeParameterElement arg : method.getTypeParameters()) {
            // }

            List<String> throwsList = new ArrayList<String>();
            for (TypeMirror tm : method.getThrownTypes()) {
                throwsList.add(pickQualifiedName(tm));
            }

            ArgType returnType = createArgType(method.getReturnType());
            boolean needSync = !"void".equalsIgnoreCase(returnType.typeName)
                    || (throwsList.size() > 0);

            if (attr != null) {
                if (attr.forceSync()) {
                    needSync = true;
                }
            }

            MethodInfo methodInfo = new MethodInfo(needSync, methodName, eventName, argTypes,
                    throwsList, returnType);
            methodInfos.add(methodInfo);
            count++;
        }
        return methodInfos;
    }

    private static ArgType createArgType(TypeMirror tm) {
        switch (tm.getKind()) {
            case BOOLEAN:
                return new ArgType("boolean", "Boolean");
            case BYTE:
                return new ArgType("byte", "Byte");
            case CHAR:
                return new ArgType("char", "Char");
            case SHORT:
                return new ArgType("short", "Short");
            case INT:
                return new ArgType("int", "Integer");
            case LONG:
                return new ArgType("long", "Long");
            case FLOAT:
                return new ArgType("float", "Float");
            case DOUBLE:
                return new ArgType("double", "Double");
            case VOID:
                return new ArgType("void", "Void");
            default: {
                String name = pickQualifiedName(tm);
                return new ArgType(name, name);
            }
        }
    }

    private static String pickQualifiedName(TypeMirror src) {
        if (src instanceof PrimitiveType) {
            return ((PrimitiveType)src).getKind().name().toLowerCase();
        } else if (src instanceof ArrayType) {
            return pickQualifiedName(((ArrayType)src).getComponentType()) + "[]";
        } else if (src instanceof DeclaredType) {
            Element element = ((DeclaredType)src).asElement();
            return getPackageName(element) + "." + String.valueOf(element.getSimpleName());
        }
        return null;
    }

    public static String getPackageName(Element element) {
        while (!(element instanceof PackageElement)) {
            element = element.getEnclosingElement();
        }
        return ((PackageElement)element).getQualifiedName().toString();
    }

    private static void replaceStringBuilder(StringBuilder sb, String target, String replacement) {
        int start = 0;
        while (true) {
            start = sb.indexOf(target, start);
            if (start >= 0) {
                sb.replace(start, start + target.length(), replacement);
            } else {
                break;
            }
        }
    }
}
