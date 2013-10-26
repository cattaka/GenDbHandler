
package net.cattaka.util.genparcelfunc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import com.sun.mirror.apt.AnnotationProcessor;
import com.sun.mirror.apt.AnnotationProcessorEnvironment;
import com.sun.mirror.apt.AnnotationProcessorFactory;
import com.sun.mirror.declaration.AnnotationTypeDeclaration;

public class GenParcelFuncAnnotationProcessorFactory implements AnnotationProcessorFactory {

    public Collection<String> supportedOptions() {
        return Collections.emptyList();
    }

    public Collection<String> supportedAnnotationTypes() {
        return annotations;
    }

    public AnnotationProcessor getProcessorFor(Set<AnnotationTypeDeclaration> atds,
            AnnotationProcessorEnvironment env) {
        return new GenParcelFuncAnnotationProcessor(env);
    }

    private static ArrayList<String> annotations = new ArrayList<String>();

    {
        annotations.add(GenParcelFunc.class.getName());
    }
}
