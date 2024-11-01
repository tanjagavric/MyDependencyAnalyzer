import org.pfsw.tools.cda.base.model.ClassInformation;
import org.pfsw.tools.cda.base.model.Workset;
import org.pfsw.tools.cda.base.model.workset.ClasspathPartDefinition;
import org.pfsw.tools.cda.core.init.WorksetInitializer;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

// Create a program to check, if the list of jars contains all required dependencies to execute the specified main class
// The program should take as input name of the main class, and a list of paths to jar files.
// It should determine whether it is possible to run this class with a classpath formed from these jar files.

public class MyDependencyAnalyzer {
    
    public boolean canExecute(String mainClassName, List<String> jarPaths) {
        
        //loading available classes from jar files
        Set<String> availableClasses = new HashSet<>();
        try {
            loadAvailableClasses(jarPaths, availableClasses);
        } catch (IOException e) {
            return false;
        }

        //loading necessary classes, i.e. classes that our class depends on
        Set<String> necessaryClasses = new HashSet<>();
        Set<String> processedClasses = new HashSet<>();
        try {
            loadNecessaryClasses(mainClassName, necessaryClasses, processedClasses);
        } catch (ClassNotFoundException e) {
            System.err.println("Main class name is invalid.");
            return false;
        }

        //checking if all necessary classes are in the set of available classes
        for (String necessaryClass : necessaryClasses) {
            if (!availableClasses.contains(necessaryClass)) {
                System.out.println("This class is missing: " + necessaryClass);
                return false;
            }
        }
        return true;
    }

    private void loadAvailableClasses(List<String> jarPaths, Set<String> availableClasses) throws IOException {
        for (String jarPath : jarPaths) {
            File file = new File("build/libs/" + jarPath);
            try (JarFile jarFile = new JarFile(file)) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.getName().endsWith(".class")) {
                        //System.out.println(entry.getName());
                        String className = entry.getName().replace('/', '.').replace(".class", "");
                        availableClasses.add(className);
                    }
                }
            } catch (IOException e) {
                System.err.println("This jar path is invalid: " + jarPath);
                throw e;
            }
        }
    }

    private void loadNecessaryClasses(String className, Set<String> necessaryClasses, Set<String> processedClasses) throws ClassNotFoundException {
        if (processedClasses.contains(className)) return;
        processedClasses.add(className);

        Workset workset = new Workset("buildLibs");
        ClasspathPartDefinition partDefinition = new ClasspathPartDefinition("build/libs/*.jar");
        workset.addClasspathPartDefinition(partDefinition);
        WorksetInitializer wsInitializer = new WorksetInitializer(workset);
        wsInitializer.initializeWorksetAndWait(null);

        ClassInformation classInfo = workset.getClassInfo(className);
        ClassInformation[] classes = classInfo.getReferredClassesArray();
        for (ClassInformation ci : classes) {
            String referredClassName = ci.getName();
            if (!referredClassName.startsWith("java")) {
                //System.out.println(referredClassName);
                necessaryClasses.add(referredClassName);
                loadNecessaryClasses(referredClassName, necessaryClasses, processedClasses);
            }
        }
    }

    public static void main(String[] args) {

        if (args.length < 2) {
            System.out.println("Usage: java MyDependencyAnalyzer <mainClass> <jar1> <jar2> ... <jarN>");
            return;
        }
        String mainClassName = args[0];
        List<String> jarPaths = List.of(args).subList(1, args.length);

        MyDependencyAnalyzer mda = new MyDependencyAnalyzer();
        System.out.println(mda.canExecute(mainClassName, jarPaths) ? "Class can be successfully executed." : "Dependency is missing.");
    }
}