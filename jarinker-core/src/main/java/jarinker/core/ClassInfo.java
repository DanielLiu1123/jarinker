package jarinker.core;

import java.lang.classfile.ClassModel;

/**
 * Information about a class discovered during analysis.
 *
 * @author Freeman
 */
public class ClassInfo {

    private final String className;
    private final String packageName;
    private final ClassModel model;

    private ClassInfo(String className, String packageName, ClassModel model) {
        this.className = className;
        this.packageName = packageName;
        this.model = model;
    }

    public String getClassName() {
        return className;
    }

    public String getPackageName() {
        return packageName;
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
        String packageName = getPackageName(className);
        return new ClassInfo(className, packageName, classModel);
    }

    private static String getPackageName(String className) {
        int lastDot = className.lastIndexOf('.');
        return lastDot > 0 ? className.substring(0, lastDot) : "";
    }
}
