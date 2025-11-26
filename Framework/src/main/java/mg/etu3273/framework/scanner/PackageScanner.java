package mg.etu3273.framework.scanner;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import mg.etu3273.framework.annotation.Controller;
import mg.etu3273.framework.annotation.GetUrl;
import mg.etu3273.framework.annotation.PostUrl;
import mg.etu3273.framework.annotation.Url;


public class PackageScanner {

    public static Map<String, List<Mapping>> scanAllClasspath() throws Exception {
        Map<String, List<Mapping>> urlMappings = new HashMap<>();
                
        List<Class<?>> allClasses = getAllClassesInWebapp();
        int controllersFound = 0;
        int methodsFound = 0;

        for (Class<?> clazz : allClasses) {
            String className = clazz.getName();
            
            if (shouldIgnoreClass(className)) {
                continue;
            }

            if (clazz.isAnnotationPresent(Controller.class)) {
                controllersFound++;                
                Method[] methods = clazz.getDeclaredMethods();
                
                for (Method method : methods) {
                    String url = null;
                    String httpMethod = null;

                    if (method.isAnnotationPresent(GetUrl.class)) {
                        GetUrl annotation = method.getAnnotation(GetUrl.class);
                        url = annotation.value();
                        httpMethod = "GET";
                    } 
                    else if (method.isAnnotationPresent(PostUrl.class)) {
                        PostUrl annotation = method.getAnnotation(PostUrl.class);
                        url = annotation.value();
                        httpMethod = "POST";
                    } 
                    else if (method.isAnnotationPresent(Url.class)) {
                        Url annotation = method.getAnnotation(Url.class);
                        url = annotation.value();
                        httpMethod = null; // null = accepte GET et POST
                    }

                    if (url != null) {
                        Mapping mapping = new Mapping(url, clazz.getName(), method, httpMethod);
                        
                        urlMappings.computeIfAbsent(url, k -> new ArrayList<>()).add(mapping);
                        methodsFound++;
                    }
                }
                
                System.out.println();
            }
        }
        return urlMappings;
    }

    public static Map<String, List<Mapping>> scanControllers(String packageName) throws Exception {
        if (packageName == null || packageName.trim().isEmpty()) {
            return scanAllClasspath();
        }

        Map<String, List<Mapping>> urlMappings = new HashMap<>();
        List<Class<?>> classes = getClassesInPackage(packageName);

        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(Controller.class)) {
                Method[] methods = clazz.getDeclaredMethods();
                
                for (Method method : methods) {
                    String url = null;
                    String httpMethod = null;

                    if (method.isAnnotationPresent(GetUrl.class)) {
                        url = method.getAnnotation(GetUrl.class).value();
                        httpMethod = "GET";
                    } else if (method.isAnnotationPresent(PostUrl.class)) {
                        url = method.getAnnotation(PostUrl.class).value();
                        httpMethod = "POST";
                    } else if (method.isAnnotationPresent(Url.class)) {
                        url = method.getAnnotation(Url.class).value();
                        httpMethod = null;
                    }

                    if (url != null) {
                        Mapping mapping = new Mapping(url, clazz.getName(), method, httpMethod);
                        urlMappings.computeIfAbsent(url, k -> new ArrayList<>()).add(mapping);
                    }
                }
            }
        }

        return urlMappings;
    }

    private static List<Class<?>> getClassesInPackage(String packageName) throws Exception {
        List<Class<?>> classes = new ArrayList<>();
        String path = packageName.replace('.', '/');
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources = classLoader.getResources(path);

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            File directory = new File(resource.getFile());
            if (directory.exists()) {
                scanDirectoryForPackage(directory, packageName, classes);
            }
        }

        return classes;
    }

    private static void scanDirectoryForPackage(File directory, String packageName, List<Class<?>> classes) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    scanDirectoryForPackage(file, packageName + "." + file.getName(), classes);
                } else if (file.getName().endsWith(".class")) {
                    String className = packageName + "." + file.getName().replace(".class", "");
                    try {
                        Class<?> clazz = Class.forName(className);
                        classes.add(clazz);
                    } catch (ClassNotFoundException | NoClassDefFoundError e) {
                    }
                }
            }
        }
    }

    private static void scanDirectory(File directory, String packageName, List<Class<?>> classes) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    String subPackage = packageName.isEmpty() ? 
                        file.getName() : packageName + "." + file.getName();
                    scanDirectory(file, subPackage, classes);
                } else if (file.getName().endsWith(".class")) {
                    String className = packageName + "." + file.getName().replace(".class", "");
                    try {
                        Class<?> clazz = Class.forName(className);
                        classes.add(clazz);
                    } catch (ClassNotFoundException | NoClassDefFoundError e) {
                    }
                }
            }
        }
    }

    private static List<Class<?>> getAllClassesInWebapp() throws Exception {
        List<Class<?>> classes = new ArrayList<>();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> roots = classLoader.getResources("");

        while (roots.hasMoreElements()) {
            URL root = roots.nextElement();
            String path = root.getPath();
            if (path.contains("/WEB-INF/classes/")) {
                File classesDir = new File(root.getFile());
                scanDirectory(classesDir, "", classes);
            }
        }

        Enumeration<URL> libRoots = classLoader.getResources("WEB-INF/lib/");
        while (libRoots.hasMoreElements()) {
            URL libRoot = libRoots.nextElement();
            File libDir = new File(libRoot.getFile());
            if (libDir.isDirectory()) {
                for (File jarFile : libDir.listFiles((d, n) -> n.endsWith(".jar"))) {
                    scanJarFile(jarFile, classes);
                }
            }
        }

        return classes;
    }

    private static void scanJarFile(File jarFile, List<Class<?>> classes) {
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();
                if (entryName.endsWith(".class")) {
                    String className = entryName
                        .replace("/", ".")
                        .replace(".class", "");
                    if (!shouldIgnoreClass(className)) {
                        try {
                            Class<?> clazz = Class.forName(className);
                            classes.add(clazz);
                        } catch (ClassNotFoundException | NoClassDefFoundError e) {
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    private static boolean shouldIgnoreClass(String className) {
        return className.startsWith("java.") ||
               className.startsWith("javax.") ||
               className.startsWith("jakarta.") ||
               className.startsWith("sun.") ||
               className.startsWith("jdk.") ||
               className.startsWith("org.apache.") ||
               className.startsWith("com.sun.");
    }
}