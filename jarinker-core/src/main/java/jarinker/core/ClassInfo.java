package jarinker.core;

import java.lang.classfile.ClassModel;

/**
 * Information about a class discovered during analysis.
 *
 * @author Freeman
 */
public class ClassInfo {

    private final String className;
    private final ClassModel model;

    private ClassInfo(String className, ClassModel model) {
        this.className = className;
        this.model = model;
    }

    public String getClassName() {
        return className;
    }

    public ClassModel getModel() {
        return model;
    }

    @Override
    public String toString() {
        return "ClassInfo{" + "className='" + className + '\'' + ", model=" + model + '}';
    }

    public static ClassInfo of(ClassModel classModel) {
        String className = classModel.thisClass().asInternalName().replace('/', '.');
        return new ClassInfo(className, classModel);
    }
}
