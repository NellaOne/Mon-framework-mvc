package mg.etu3273.framework;

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
import mg.etu3273.framework.annotation.Url;

public class PackageScanner {
    public static Map<String, Mapping> scanAllClasspath() throws Exception {
        Map<String, Mapping> urlMappings = new HashMap<>();
        
        System.out.println("\n╔════════════════════════════════════════════════════╗");
        System.out.println("║  SCAN DES CONTRÔLEURS - Sprint 2 bis              ║");
        System.out.println("╚════════════════════════════════════════════════════╝");
        System.out.println("🔍 Scan de TOUTES les classes chargées...");
        
        // 1. Récupérer toutes les classes du package (récursif)
        // List<Class<?>> classes = getClassesInPackage(packageName);
        List<Class<?>> allClasses = getAllClassesInWebapp();
        System.out.println("📁 Nombre de classes trouvées: " + allClasses.size());
        System.out.println();

        int controllersFound = 0;
        
        // 2. Pour chaque classe, vérifier si elle a @Controller
        for (Class<?> clazz : allClasses) {
            System.out.println("🔍 Analyse: " + clazz.getSimpleName());

            String className = clazz.getName();
            if (shouldIgnoreClass(className)) {
                continue;
            }
            
            if (clazz.isAnnotationPresent(Controller.class)) {
                controllersFound++;
                System.out.println("   ✅ CONTRÔLEUR TROUVÉ: " + clazz.getName());
                
                // 3. Scanner les méthodes de ce contrôleur
                Method[] methods = clazz.getDeclaredMethods();
                System.out.println("   📋 Nombre de méthodes: " + methods.length);
                
                for (Method method : methods) {
                    // Vérifier si la méthode a @Url
                    if (method.isAnnotationPresent(Url.class)) {
                        Url urlAnnotation = method.getAnnotation(Url.class);
                        String url = urlAnnotation.value();
                        
                        // 4. Créer le mapping et l'ajouter à la Map
                        Mapping mapping = new Mapping(url, clazz.getName(), method);
                        urlMappings.put(url, mapping);
                        
                        System.out.println("      🔗 URL mappée: " + url + " → " + method.getName() + "()");
                    }
                }
            } else {
                System.out.println("   ❌ Pas de @Controller (ignoré)");
            }
            System.out.println();
        }
        
        System.out.println("╔════════════════════════════════════════════════════╗");
        System.out.println("║  RÉSULTAT DU SCAN                                  ║");
        System.out.println("╚════════════════════════════════════════════════════╝");
        System.out.println("✅ Contrôleurs trouvés: " + controllersFound);
        System.out.println("✅ Total URLs mappées: " + urlMappings.size());
        System.out.println();
        
        return urlMappings;
    }


    /**
     * VERSION ALTERNATIVE: Scanner un package spécifique (pour optimisation)
     * Cette version est OPTIONNELLE si quelqu'un veut spécifier un package
     */
    public static Map<String, Mapping> scanControllers(String packageName) throws Exception {
        // Si le package est vide ou null, scanner tout le classpath
        if (packageName == null || packageName.trim().isEmpty()) {
            return scanAllClasspath(); // Appel à la version sans paramètre
        }
        
        Map<String, Mapping> urlMappings = new HashMap<>();
        
        System.out.println("📦 Package: " + packageName);
        
        List<Class<?>> classes = getClassesInPackage(packageName);
        System.out.println("📁 Classes trouvées: " + classes.size());
        System.out.println();
        
        int controllersFound = 0;
        
        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(Controller.class)) {
                controllersFound++;
                System.out.println("✅ CONTRÔLEUR: " + clazz.getName());
                
                Method[] methods = clazz.getDeclaredMethods();
                for (Method method : methods) {
                    if (method.isAnnotationPresent(Url.class)) {
                        Url urlAnnotation = method.getAnnotation(Url.class);
                        String url = urlAnnotation.value();
                        
                        Mapping mapping = new Mapping(url, clazz.getName(), method);
                        urlMappings.put(url, mapping);
                        
                        System.out.println("   🔗 " + url + " → " + method.getName() + "()");
                    }
                }
                System.out.println();
            }
        }
        
        System.out.println("✅ Contrôleurs trouvés: " + controllersFound);
        System.out.println("✅ URLs mappées: " + urlMappings.size());
        System.out.println();
        
        return urlMappings;
    }
    
    /**
     * Récupère toutes les classes d'un package (récursif)
     */
    private static List<Class<?>> getClassesInPackage(String packageName) throws Exception {
        List<Class<?>> classes = new ArrayList<>();
         String path = packageName.replace('.', '/');
                
        // Récupérer l'URL du package depuis le classpath
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources = classLoader.getResources(path);
        // URL resource = classLoader.getResource(path);
        
         while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            File directory = new File(resource.getFile());
            
            if (directory.exists()) {
                scanDirectoryForPackage(directory, packageName, classes);
            }
        }
        
        return classes;
    }

    /**
     * Scanne un répertoire pour un package spécifique
     */
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
                        // Ignorer
                    }
                }
            }
        }
    }
    
    /**
     * Scanne récursivement un répertoire pour trouver les classes
     */
    private static void scanDirectory(File directory, String packageName, List<Class<?>> classes) {
        File[] files = directory.listFiles();
        
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    String subPackage = packageName.isEmpty() ? 
                        file.getName() : packageName + "." + file.getName();
                    // Récursion dans les sous-packages
                    scanDirectory(file, subPackage, classes);
                } else if (file.getName().endsWith(".class")) {
                    // Charger la classe
                    String className = packageName + "." + file.getName().replace(".class", "");
                    try {
                        Class<?> clazz = Class.forName(className);
                        classes.add(clazz);
                    } catch (ClassNotFoundException | NoClassDefFoundError e) {
                        // Ignorer les classes qui ne peuvent pas être chargées
                    }
                }
            }
        }
    }

    /**
     * VERSION 2: Scanner un package spécifique (optionnel)
     * Pour ceux qui veulent optimiser
     */
    public static Map<String, Mapping> scanPackage(String packageName) throws Exception {
        Map<String, Mapping> urlMappings = new HashMap<>();
        
        System.out.println("\n╔════════════════════════════════════════════════════╗");
        System.out.println("║  SCAN D'UN PACKAGE SPÉCIFIQUE - Sprint 3           ║");
        System.out.println("╚════════════════════════════════════════════════════╝");
        System.out.println("📦 Package: " + packageName);
        System.out.println();
        
        List<Class<?>> classes = getClassesInPackage(packageName);
        System.out.println("📁 Classes trouvées: " + classes.size());
        System.out.println();
        
        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(Controller.class)) {
                System.out.println("✅ CONTRÔLEUR: " + clazz.getName());
                
                Method[] methods = clazz.getDeclaredMethods();
                for (Method method : methods) {
                    if (method.isAnnotationPresent(Url.class)) {
                        Url urlAnnotation = method.getAnnotation(Url.class);
                        String url = urlAnnotation.value();
                        
                        Mapping mapping = new Mapping(url, clazz.getName(), method);
                        urlMappings.put(url, mapping);
                        
                        System.out.println("   🔗 " + url + " → " + method.getName() + "()");
                    }
                }
                System.out.println();
            }
        }
        
        System.out.println("✅ URLs mappées: " + urlMappings.size());
        System.out.println();
        
        return urlMappings;
    }


    /**
     * Récupère TOUTES les classes chargées dans le classpath
     * MÉTHODE 1: Via les ressources du ClassLoader
     */
    private static List<Class<?>> getAllClassesInWebapp() throws Exception {
    List<Class<?>> classes = new ArrayList<>();
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    // 1. Scanner WEB-INF/classes
    Enumeration<URL> roots = classLoader.getResources("");
    while (roots.hasMoreElements()) {
        URL root = roots.nextElement();
        String path = root.getPath();
        if (path.contains("/WEB-INF/classes/")) {
            File classesDir = new File(root.getFile());
            scanDirectory(classesDir, "", classes);
        }
    }

    // 2. Scanner les JARs dans WEB-INF/lib
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

    /**
     * Scanne un fichier JAR
     */
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
                            // Ignorer
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignorer les JARs problématiques
        }
    }


    /**
     * Détermine si une classe doit être ignorée (JDK, Jakarta, etc.)
     */
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