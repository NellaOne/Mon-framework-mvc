package mg.etu3273.framework;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mg.etu3273.framework.annotation.Controller;
import mg.etu3273.framework.annotation.Url;

public class PackageScanner {
    public static Map<String, Mapping> scanControllers(String packageName) throws Exception {
        Map<String, Mapping> urlMappings = new HashMap<>();
        
        System.out.println("\n╔════════════════════════════════════════════════════╗");
        System.out.println("║  SCAN DES CONTRÔLEURS - Sprint 2 bis              ║");
        System.out.println("╚════════════════════════════════════════════════════╝");
        System.out.println("📦 Package de base: " + packageName);
        
        // 1. Récupérer toutes les classes du package (récursif)
        List<Class<?>> classes = getClassesInPackage(packageName);
        System.out.println("📁 Nombre de classes trouvées: " + classes.size());
        System.out.println();
        
        // 2. Pour chaque classe, vérifier si elle a @Controller
        for (Class<?> clazz : classes) {
            System.out.println("🔍 Analyse: " + clazz.getSimpleName());
            
            if (clazz.isAnnotationPresent(Controller.class)) {
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
        System.out.println("✅ Total URLs mappées: " + urlMappings.size());
        System.out.println();
        
        return urlMappings;
    }
    
    /**
     * Récupère toutes les classes d'un package (récursif)
     */
    private static List<Class<?>> getClassesInPackage(String packageName) throws Exception {
        List<Class<?>> classes = new ArrayList<>();
        
        // Convertir le nom du package en chemin (mg.etu3273 → mg/etu3273)
        String path = packageName.replace('.', '/');
        
        // Récupérer l'URL du package depuis le classpath
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL resource = classLoader.getResource(path);
        
        if (resource == null) {
            System.out.println("⚠️  ATTENTION: Package '" + packageName + "' non trouvé dans le classpath");
            return classes;
        }
        
        File directory = new File(resource.getFile());
        
        if (directory.exists()) {
            // Scanner tous les fichiers .class récursivement
            scanDirectory(directory, packageName, classes);
        }
        
        return classes;
    }
    
    /**
     * Scanne récursivement un répertoire pour trouver les classes
     */
    private static void scanDirectory(File directory, String packageName, List<Class<?>> classes) {
        File[] files = directory.listFiles();
        
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // Récursion dans les sous-packages
                    scanDirectory(file, packageName + "." + file.getName(), classes);
                } else if (file.getName().endsWith(".class")) {
                    // Charger la classe
                    String className = packageName + "." + file.getName().replace(".class", "");
                    try {
                        Class<?> clazz = Class.forName(className);
                        classes.add(clazz);
                    } catch (ClassNotFoundException e) {
                        System.out.println("⚠️  Impossible de charger: " + className);
                    } catch (NoClassDefFoundError e) {
                        // Ignorer les erreurs de dépendances manquantes
                    }
                }
            }
        }
    }
}