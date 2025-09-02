package jarinker.core;

import java.util.HashSet;
import java.util.Set;

/**
 * Information about a class discovered during analysis.
 *
 * @author Freeman
 */
public class ClassInfo {

    private final String className;
    private final String packageName;
    private final boolean isInterface;
    private final boolean isAbstract;
    private final String superClass;
    private final Set<String> interfaces;
    private final Set<String> annotations;
    private final long size;

    public ClassInfo(
            String className,
            String packageName,
            boolean isInterface,
            boolean isAbstract,
            String superClass,
            Set<String> interfaces,
            Set<String> annotations,
            long size) {
        this.className = className;
        this.packageName = packageName;
        this.isInterface = isInterface;
        this.isAbstract = isAbstract;
        this.superClass = superClass;
        this.interfaces = new HashSet<>(interfaces);
        this.annotations = new HashSet<>(annotations);
        this.size = size;
    }

    // Getter methods
    public String getClassName() {
        return className;
    }

    public String getPackageName() {
        return packageName;
    }

    public boolean isInterface() {
        return isInterface;
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public String getSuperClass() {
        return superClass;
    }

    public Set<String> getInterfaces() {
        return new HashSet<>(interfaces);
    }

    public Set<String> getAnnotations() {
        return new HashSet<>(annotations);
    }

    public long getSize() {
        return size;
    }

    @Override
    public String toString() {
        return "ClassInfo{" + "className='"
                + className + '\'' + ", packageName='"
                + packageName + '\'' + ", isInterface="
                + isInterface + ", isAbstract="
                + isAbstract + ", superClass='"
                + superClass + '\'' + ", interfaces="
                + interfaces + ", annotations="
                + annotations + ", size="
                + size + '}';
    }
}
