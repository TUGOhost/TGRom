package android.app;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;
import java.lang.reflect.*;

public class tgUnpacker {

    private static Object invokeStaticMethod(String class_name, String method_name, Class[] pareTyple, Object[] pareVaules) {
        try {
            Class obj_class = Class.forName(class_name);
            Method method = obj_class.getMethod(method_name, pareTyple);
            return method.invoke(null, pareVaules);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static ClassLoader getClassloader() {
        ClassLoader resultClassloader = null;

        Object currentActivityThread = invokeStaticMethod(
                "android.app.ActivityThread", "currentActivityThread",
                new Class[]{}, new Object[]{});
        Object mBoundApplication = getFieldOjbect(
                "android.app.ActivityThread", currentActivityThread,
                "mBoundApplication");
        Object loadedApkInfo = getFieldOjbect(
                "android.app.ActivityThread$AppBindData",
                mBoundApplication, "info");
        Application mApplication = (Application) getFieldOjbect("android.app.LoadedApk", loadedApkInfo, "mApplication");
        resultClassloader = mApplication.getClassLoader();
        return resultClassloader;
    }

    private static Field getClassField(ClassLoader classloader, String class_name, String filedName) {
        try {
            Class obj_class = classloader.loadClass(class_name);
            Field field = obj_class.getDeclaredField(filedName);
            field.setAccessible(true);
            return field;
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Object getFieldOjbect(String class_name, Object obj, String filedName) {
        try {
            Class obj_class = Class.forName(class_name);
            Field field = obj_class.getDeclaredField(filedName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Object getClassFieldObject(ClassLoader classloader, String class_name, Object obj, String filedName) {
        try {
            Class obj_class = classloader.loadClass(class_name);
            Field field = obj_class.getDeclaredField(filedName);
            field.setAccessible(true);
            Object result = null;
            result = field.get(obj);
            return result;
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void loadClassAndInvoke(ClassLoader appClassloader, String eachclassname, Method dumpMethodCode_method) {
        //Log.i("ActivityThread", "go into loadClassAndInvoke->" + "classname:" + eachclassname);
        Class resultclass = null;
        try {
            resultclass = appClassloader.loadClass(eachclassname);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        } catch (Error e) {
            e.printStackTrace();
            return;
        }
        if (resultclass != null) {
            try {
                Constructor<?> cons[] = resultclass.getDeclaredConstructors();
                for (Constructor<?> constructor : cons) {
                    if (dumpMethodCode_method != null) {
                        try {
                            dumpMethodCode_method.invoke(null, constructor);
                        } catch (Exception e) {
                            e.printStackTrace();
                            continue;
                        } catch (Error e) {
                            e.printStackTrace();
                            continue;
                        }
                    } else {
                        //Log.e("ActivityThread", "dumpMethodCode_method is null ");
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            } catch (Error e) {
                e.printStackTrace();
            }
            try {
                Method[] methods = resultclass.getDeclaredMethods();
                if (methods != null) {
                    for (Method m : methods) {
                        if (dumpMethodCode_method != null) {
                            try {
                                dumpMethodCode_method.invoke(null, m);
                            } catch (Exception e) {
                                e.printStackTrace();
                                continue;
                            } catch (Error e) {
                                e.printStackTrace();
                                continue;
                            }
                        } else {
                            //Log.e("ActivityThread", "dumpMethodCode_method is null ");
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } catch (Error e) {
                e.printStackTrace();
            }
        }
    }


    public static void tgunpacker() {
        ClassLoader appClassloader = getClassloader();
        List<Object> dexFilesArray = new ArrayList<Object>();
        Field pathList_Field = (Field) getClassField(appClassloader, "dalvik.system.BaseDexClassLoader", "pathList");
        Object pathList_object = getFieldOjbect("dalvik.system.BaseDexClassLoader", appClassloader, "pathList");
        Object[] ElementsArray = (Object[]) getFieldOjbect("dalvik.system.DexPathList", pathList_object, "dexElements");
        Field dexFile_fileField = null;
        try {
            dexFile_fileField = (Field) getClassField(appClassloader, "dalvik.system.DexPathList$Element", "dexFile");
        } catch (Exception e) {
            e.printStackTrace();
        }
        Class DexFileClazz = null;
        try {
            DexFileClazz = appClassloader.loadClass("dalvik.system.DexFile");
        } catch (Exception e) {
            e.printStackTrace();
        }
        Method getClassNameList_method = null;
        Method defineClass_method = null;
        Method dumpDexFile_method = null;
        Method dumpMethodCode_method = null;

        for (Method field : DexFileClazz.getDeclaredMethods()) {
            if (field.getName().equals("getClassNameList")) {
                getClassNameList_method = field;
                getClassNameList_method.setAccessible(true);
            }
            if (field.getName().equals("defineClassNative")) {
                defineClass_method = field;
                defineClass_method.setAccessible(true);
            }
            if (field.getName().equals("dumpMethodCode")) {
                dumpMethodCode_method = field;
                dumpMethodCode_method.setAccessible(true);
            }
        }
        Field mCookiefield = getClassField(appClassloader, "dalvik.system.DexFile", "mCookie");
        for (int j = 0; j < ElementsArray.length; j++) {
            Object element = ElementsArray[j];
            Object dexfile = null;
            try {
                dexfile = (Object) dexFile_fileField.get(element);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (dexfile == null) {
                continue;
            }
            if (dexfile != null) {
                dexFilesArray.add(dexfile);
                Object mcookie = getClassFieldObject(appClassloader, "dalvik.system.DexFile", dexfile, "mCookie");
                if (mcookie == null) {
                    continue;
                }
                String[] classnames = null;
                try {
                    classnames = (String[]) getClassNameList_method.invoke(dexfile, mcookie);
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                } catch (Error e) {
                    e.printStackTrace();
                    continue;
                }
                if (classnames != null) {
                    for (String eachclassname : classnames) {
                        loadClassAndInvoke(appClassloader, eachclassname, dumpMethodCode_method);
                    }
                }

            }
        }
        return;
    }


    private static boolean shouldUnpack() {
        boolean should_unpack = false;
        String processName = ActivityThread.currentProcessName();
        BufferedReader br = null;
        String configPath = "/data/local/tmp/fext.config";
        try {
            br = new BufferedReader(new FileReader(configPath));
            String line;
            while ((line = br.readLine()) != null) {
                if (processName.equals(line)) {
                    should_unpack = true;
                    break;
                }
            }
            br.close();
        } catch (Exception ignored) {

        }
        return should_unpack;
    }


    private static String getClassList() {
        String processName = ActivityThread.currentProcessName();
        BufferedReader br = null;
        String configPath = "/data/local/tmp/" + processName;
        //Log.e("ActivityThread", "getClassList processName:"+processName);
        StringBuilder sb = new StringBuilder();
        try {
            br = new BufferedReader(new FileReader(configPath));
            String line;
            while ((line = br.readLine()) != null) {

                if (line.length() >= 2) {
                    sb.append(line + "\n");
                }
            }
            br.close();
        } catch (Exception ex) {
            //Log.e("ActivityThread", "getClassList err:"+ex.getMessage());
            return "";
        }
        return sb.toString();
    }


    //对指定类进行主动调用
    private static void fartWithClassList(String classlist) {
        ClassLoader appClassloader = getClassloader();
        if (appClassloader == null) {
            //Log.e("ActivityThread", "appClassloader is null");
            return;
        }
        Class DexFileClazz = null;
        try {
            DexFileClazz = appClassloader.loadClass("dalvik.system.DexFile");
        } catch (Exception e) {
            e.printStackTrace();
        } catch (Error e) {
            e.printStackTrace();
        }
        Method dumpMethodCode_method = null;
        for (Method field : DexFileClazz.getDeclaredMethods()) {
            if (field.getName().equals("fartextMethodCode")) {
                dumpMethodCode_method = field;
                dumpMethodCode_method.setAccessible(true);
            }
        }
        String[] classes = classlist.split("\n");
        for (String clsname : classes) {
            String line = clsname;
            if (line.startsWith("L") && line.endsWith(";") && line.contains("/")) {
                line = line.substring(1, line.length() - 1);
                line = line.replace("/", ".");
            }
            loadClassAndInvoke(appClassloader, line, dumpMethodCode_method);
        }
    }

    public static void tgunpackerThread() {
        if (!shouldUnpack()) {
            return;
        }

        String classList = getClassList();
        if (null != classList && !"".equals(classList)) {
            fartWithClassList(classList);
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                tgunpacker();
                // todo try...catch
            }
        }).start();
    }

}
