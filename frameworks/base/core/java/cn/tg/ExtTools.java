package cn.tg;

import android.app.ITGRom;
import android.app.ActivityThread;
import android.os.FileUtils;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

// add
public class ExtTools {
    public static List<PackageItem> mikConfigs;
    public static List<String> bClass = new ArrayList<String>();
    public static List<String> whiteClass = new ArrayList<String>();
    public static String whitePath = "";

    public static String getTGConfig() {
        try {
            ITGRom mikrom = getiTGRom();
            if (mikrom == null) {
                return "";
            }
            return mikrom.readFile("/data/system/tg.conf");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static void initConfig() {
        try {
            mikConfigs = new ArrayList<PackageItem>();
            String mikromConfigJson = getTGConfig();
            Log.e("tgrom", "initConfig config:" + mikromConfigJson);
            if (mikromConfigJson.length() > 5) {
                final JSONArray arr = new JSONArray(mikromConfigJson);
                Log.e("tgrom", "initConfig package count:" + arr.length());
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject jobj = arr.getJSONObject(i);
                    PackageItem cfg = new PackageItem();
                    cfg.enabled = jobj.getBoolean("enabled");
                    cfg.packageName = jobj.getString("packageName");
                    cfg.appName = jobj.getString("appName");
                    if (!cfg.enabled) {
                        Log.e("tgrom", "initConfig enabled is false skip " + cfg.packageName);
                        continue;
                    }
                    cfg.whiteClass = jobj.getString("whiteClass");
                    cfg.whitePath = jobj.getString("whitePath");
                    cfg.breakClass = jobj.getString("breakClass");
                    cfg.isTuoke = jobj.getBoolean("isTuoke");
                    cfg.isDeep = jobj.getBoolean("isDeep");

                    cfg.isInvokePrint = jobj.getBoolean("isInvokePrint");
                    cfg.isJNIMethodPrint = jobj.getBoolean("isJNIMethodPrint");
                    cfg.isRegisterNativePrint = jobj.getBoolean("isRegisterNativePrint");

                    cfg.traceMethod = jobj.getString("traceMethod");
                    cfg.sleepNativeMethod = jobj.getString("sleepNativeMethod");
                    cfg.fridaJsPath = jobj.getString("fridaJsPath");
                    cfg.port = jobj.getInt("port");
                    cfg.gadgetPath = jobj.getString("gadgetPath");
                    cfg.gadgetArm64Path = jobj.getString("gadgetArm64Path");
                    cfg.soPath = jobj.getString("soPath");
                    cfg.dexPath = jobj.getString("dexPath");
                    cfg.isDobby = jobj.getBoolean("isDobby");
                    cfg.forbids = jobj.getString("forbids");
                    cfg.rediectFile = jobj.getString("rediectFile");
                    cfg.rediectDir = jobj.getString("rediectDir");
                    cfg.dexClassName = jobj.getString("dexClassName");
                    cfg.isBlock = jobj.getBoolean("isBlock");
                    mikConfigs.add(cfg);
                    Log.e("tgrom", "initConfig packageName" + cfg.packageName);
                    String processName = ActivityThread.currentProcessName();
                }
            }
            String breakPath = "/data/system/break.conf";
            String breakData = getiTGRom().readFile(breakPath);
            for (String item : breakData.split("\n")) {
                bClass.add(item);
            }
//            Log.e("tgrom", "initConfig over");
        } catch (Exception ex) {
            Log.e("tgrom", "initConfig err:" + ex.getMessage());
            return;
        }
    }

    public static void loadGadget() {
        String processName = ActivityThread.currentProcessName();
        Log.e("tgrom", "loadGadget enter package:" + processName);
        for (PackageItem item : mikConfigs) {
            Log.e("tgrom", "loadGadget package:" + processName + " mikconfig:" + item.packageName);
            if (item.packageName.equals(processName)) {
                try {
                    if (item.fridaJsPath.length() <= 0) {
                        Log.e("tgrom", "loadGadget package:" + processName + " jspath <=0");
                        continue;
                    }
                    String gadPath = "";
                    String arch = System.getProperty("os.arch");
                    if (item.gadgetPath != null && item.gadgetPath.length() > 0) {
                        if (arch.indexOf("64") >= 0) {
                            gadPath = item.gadgetArm64Path;
                        } else {
                            gadPath = item.gadgetPath;
                        }
                    } else {
                        boolean use14 = false;
                        ITGRom mikrom = getiTGRom();
                        if (mikrom != null) {
                            String res = mikrom.readFile("/data/system/fver14.conf");
                            Log.e("tgrom", "fver14.conf data " + res);
                            if (res.contains("1")) {
                                use14 = true;
                            }
                        }
                        if (System.getProperty("os.arch").indexOf("64") >= 0) {
                            if (use14) {
                                gadPath = "/system/lib64/libfdgg14.so";
                            } else {
                                gadPath = "/system/lib64/libfdgg15.so";
                            }
                        } else {
                            if (use14) {
                                gadPath = "/system/lib/libfdgg14.so";
                            } else {
                                gadPath = "/system/lib/libfdgg15.so";
                            }
                        }
                    }
                    Log.e("tgrom", "loadGadget package:" + processName + " gadPath:" + gadPath);
                    File gadfile = new File(gadPath);
                    String name = gadfile.getName().replace(".so", "");
                    String configPath = "/data/data/" + processName + "/" + name + ".config.so";
                    if (item.fridaJsPath.equals("listen") || item.fridaJsPath.equals("listen_wait")) {
                        WriteConfig(configPath, item.fridaJsPath, item.port);
                    } else {
                        File file = new File(item.fridaJsPath);
                        if (!file.exists()) {
                            file = new File("/data/data/" + processName + "/" + file.getName());
                        }
                        if (!file.exists()) {
                            Log.e("tgrom", "loadGadget package:" + processName + " frida js path:" + item.fridaJsPath + " not found");
                            continue;
                        }
                        WriteConfig(configPath, item.fridaJsPath, item.port);
                    }
                    loadSo(gadPath);
                    Log.e("tgrom", "loadGadget package:" + processName + " over");
                } catch (Exception ex) {
                    Log.e("tgrom", "loadGadget package:" + processName + " frida js path:" + item.fridaJsPath + " load err:" + ex.getMessage());
                }
                break;
            }
        }
    }

    private static ITGRom iTGRom = null;

    public static ITGRom getiTGRom() {
        if (iTGRom == null) {
            try {
                IBinder binder = ServiceManager.getService("tgrom");
                if (binder == null) {
                    Log.d("tgrom", "getiTGRom binder is null");
                    return iTGRom;
                }
                iTGRom = ITGRom.Stub.asInterface(binder);
            } catch (Exception e) {
                Log.d("tgrom", "getiTGRom exception " + e.getMessage());
                e.printStackTrace();
            }
        }
        return iTGRom;
    }

    public static void WriteConfig(String path, String jspath, int port) {
        try {
            FileWriter writer = new FileWriter(path);
            String fconfig = "";
            if (jspath.equals("listen")) {
                fconfig = "{\n" +
                        "  \"interaction\": {\n" +
                        "    \"type\": \"listen\",\n" +
                        "    \"address\": \"0.0.0.0\",\n" +
                        "    \"port\": " + port + ",\n" +
                        "    \"on_load\": \"resume\"\n" +
                        "  }\n" +
                        "}";
            } else if (jspath.equals("listen_wait")) {
                fconfig = "{\n" +
                        "  \"interaction\": {\n" +
                        "    \"type\": \"listen\",\n" +
                        "    \"address\": \"0.0.0.0\",\n" +
                        "    \"port\": " + port + ",\n" +
                        "    \"on_load\": \"wait\"\n" +
                        "  }\n" +
                        "}";
            } else {
                String processName = ActivityThread.currentProcessName();
                String fName = jspath.trim();
                String fileName = fName.substring(fName.lastIndexOf("/") + 1);
                String newJsPath = "/data/data/" + processName + "/" + fileName;
                mycopy(jspath, newJsPath);
                int perm = FileUtils.S_IRWXU | FileUtils.S_IRWXG | FileUtils.S_IRWXO;
                FileUtils.setPermissions(newJsPath, perm, -1, -1);//将权限改为777
                fconfig = "{\n" +
                        "  \"interaction\": {\n" +
                        "    \"type\": \"script\",\n" +
                        "    \"path\": \"" + newJsPath + "\"\n" +
                        "  }\n" +
                        "}";
            }
            Log.e("tgrom", "WriteConfig config:" + fconfig);
            writer.write(fconfig);
            writer.close();
        } catch (IOException e) {
            Log.e("tgrom", "WriteConfig err:" + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void loadSo(String path) {
        String processName = ActivityThread.currentProcessName();
        String fName = path.trim();
        String fileName = fName.substring(fName.lastIndexOf("/") + 1);
        String tagPath = "/data/data/" + processName + "/" + fileName;//64位so的目录
        mycopy(path, tagPath);
        int perm = FileUtils.S_IRWXU | FileUtils.S_IRWXG | FileUtils.S_IRWXO;
        FileUtils.setPermissions(tagPath, perm, -1, -1);//将权限改为777
        File file = new File(tagPath);
        if (file.exists()) {
            Log.e("tgrom", "load so src:" + path + " to:" + tagPath);
            System.load(tagPath);
            file.delete();//用完就删否则不会更新
        }
    }

    public static void mycopy(String srcFileName, String trcFileName) {
        InputStream in = null;
        OutputStream out = null;
        try {
            // in = File.open(srcFileName);
            in = new FileInputStream(srcFileName);
            out = new FileOutputStream(trcFileName);
            byte[] bytes = new byte[1024];
            int i;
            while ((i = in.read(bytes)) != -1)
                out.write(bytes, 0, i);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null)
                    in.close();
                if (out != null) {
                    out.flush();
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


    }

    /*public static PackageItem shouldMikRom() {
            String processName = ActivityThread.currentProcessName();
            Log.e("tgrom", "m1 build shouldMikRom processName:"+processName);
            for(PackageItem item : mikConfigs){
                if(item.packageName.equals(processName)){
                    if(item.isTuoke){
                        if(item.breakClass.length()>0){
                            Log.e("tgrom", "shouldMikRom breakClass:"+item.breakClass);
                            String[] bclasses=item.breakClass.split("\n");
                            for(String cls : bclasses){
                                bClass.add(cls);
                            }
                        }
                        if(item.whiteClass.length()>0){
                            Log.e("tgrom", "shouldMikRom whiteClass:"+item.whiteClass);
                            String[] wclasses=item.whiteClass.split("\n");
                            for(String cls : wclasses){
                                whiteClass.add(cls);
                            }
                        }
                        whitePath=item.whitePath;
                    }
                    SetRomConfig(item);
                    return item;
                }
            }
            Log.e("tgrom", "shouldMikRom null processName:"+processName);
            return null;
        }*/

    //为了反射封装，根据类名和字段名，反射获取字段
    /*public static Field getClassField(ClassLoader classloader, String class_name,
                                      String filedName) {

        try {
            Class obj_class = classloader.loadClass(class_name);//Class.forName(class_name);
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

    public static Object getClassFieldObject(ClassLoader classloader, String class_name, Object obj,
                                             String filedName) {

        try {
            Class obj_class = classloader.loadClass(class_name);//Class.forName(class_name);
            Field field = obj_class.getDeclaredField(filedName);
            field.setAccessible(true);
            Object result = null;
            result = field.get(obj);
            return result;
            //field.setAccessible(true);
            //return field;
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

    public static Object invokeStaticMethod(String class_name,
                                            String method_name, Class[] pareTyple, Object[] pareVaules) {

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

    public static Object getFieldObject(String class_name, Object obj,
                                        String filedName) {
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
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return null;

    }


    public static ClassLoader getClassloader() {
        ClassLoader resultClassloader = null;
        Object currentActivityThread = invokeStaticMethod(
                "android.app.ActivityThread", "currentActivityThread",
                new Class[]{}, new Object[]{});
        Object mBoundApplication = getFieldObject(
                "android.app.ActivityThread", currentActivityThread,
                "mBoundApplication");
        Application mInitialApplication = (Application) getFieldObject("android.app.ActivityThread",
                currentActivityThread, "mInitialApplication");
        Object loadedApkInfo = getFieldObject(
                "android.app.ActivityThread$AppBindData",
                mBoundApplication, "info");
        Application mApplication = (Application) getFieldObject("android.app.LoadedApk", loadedApkInfo, "mApplication");
        String processName = ActivityThread.currentProcessName();
        Log.e("tgrom", "go into app->" + "packagename:" + processName);
        resultClassloader = mApplication.getClassLoader();
        return resultClassloader;
    }*/

    /*public static void SetRomConfig(PackageItem item) {
        Log.e("tgrom", "SetRomConfig start");
        ClassLoader appClassloader = getClassloader();
        if (appClassloader == null) {
            Log.e("tgrom", "SetRomConfig appClassloader is null");
            return;
        }
        Class DexFileClazz = null;
        try {
            DexFileClazz = appClassloader.loadClass("dalvik.system.DexFile");
        } catch (Exception e) {
            Log.e("tgrom", "SetRomConfig loadClass err:" + e.getMessage());
            e.printStackTrace();
        }
        Method setMikRomConfig_method = null;
        for (Method field : DexFileClazz.getDeclaredMethods()) {
            if (field.getName().equals("setMikRomConfig")) {
                setMikRomConfig_method = field;
                setMikRomConfig_method.setAccessible(true);
            }
        }
        if (setMikRomConfig_method == null) {
            Log.e("tgrom", "SetRomConfig setMikRomConfig_method is null");
            return;
        }
        try {
            Log.e("tgrom", "SetRomConfig invoke");
            setMikRomConfig_method.invoke(null, item);
        } catch (Exception e) {
            Log.e("tgrom", "SetRomConfig setMikRomConfig_method.invoke " + e.getMessage());
            e.printStackTrace();
        }
    }*/
}
